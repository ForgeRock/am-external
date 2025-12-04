/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018-2025 Ping Identity Corporation.
 */


package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * Node with configurable message and acknowledgement buttons.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = MessageNode.Config.class,
        configValidator = MessageNode.MessageNodeValidator.class,
        tags = {"utilities"})
public class MessageNode extends AbstractDecisionNode {

    private final Config config;
    private static final List<String> MESSAGE_ATTRIBUTES = List.of("messageYes", "messageNo", "message");
    private final LocalizedMessageProvider localizedMessageProvider;


    /**
     * Validates the message node, ensuring all provided Locales are valid.
     */
    public static class MessageNodeValidator implements ServiceConfigValidator {

        private final Logger logger = LoggerFactory.getLogger(MessageNodeValidator.class);

        private static String getLocaleStringFromMessage(String message) {
            return StringUtils.substringBetween(message, "[", "]");
        }

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            for (String messageAttribute : MESSAGE_ATTRIBUTES) {
                validateMessageAttribute(attributes, messageAttribute);
            }
        }

        private void validateMessageAttribute(Map<String, Set<String>> attributes, String messageAttribute)
                throws ServiceConfigException {
            Set<String> attributesSet = attributes.get(messageAttribute);
            Set<Locale> messageLocales = attributesSet.stream()
                    .map(MessageNodeValidator::getLocaleStringFromMessage)
                    .map(com.sun.identity.shared.locale.Locale::getLocale)
                    .collect(Collectors.toSet());
            for (Locale messageLocale : messageLocales) {
                if (!LocaleUtils.isAvailableLocale(messageLocale)) {
                    logger.debug("Invalid messageLocale {} for {} attribute", messageLocale.toString(),
                            messageAttribute);
                    throw new ServiceConfigException("Invalid locale provided");
                }
            }
        }
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Configurable message.
         *
         * @return collection of localized messages
         */
        @Attribute(order = 100)
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }

        /**
         * Configurable message for YES button.
         *
         * @return collection of localized YES messages
         */
        @Attribute(order = 200)
        default Map<Locale, String> messageYes() {
            return Collections.emptyMap();
        }

        /**
         * Configurable message for NO button.
         *
         * @return collection of localized NO messages
         */
        @Attribute(order = 300)
        default Map<Locale, String> messageNo() {
            return Collections.emptyMap();
        }

        /**
         * Configurable state variable name.
         *
         * @return name of state variable
         */
        @Attribute(order = 400)
        Optional<String> stateField();
    }

    /**
     * Create the node.
     *
     * @param config                          The service config.
     * @param realm                           The realm
     * @param localizedMessageProviderFactory Factory for creating a localized message provider
     */
    @Inject
    public MessageNode(@Assisted Config config, @Assisted Realm realm,
            LocalizedMessageProviderFactory localizedMessageProviderFactory) {
        this.config = config;
        this.localizedMessageProvider = localizedMessageProviderFactory.create(realm);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Optional<Integer> confirmationVariable = context.getCallback(ConfirmationCallback.class)
                .map(ConfirmationCallback::getSelectedIndex);
        Optional<Boolean> outcome = confirmationVariable.map(i -> i.equals(ConfirmationCallback.YES));

        String finalYes = getLocalisedMessage(context, config.messageYes(), "default.messageYes");
        String finalNo = getLocalisedMessage(context, config.messageNo(), "default.messageNo");
        String finalMessage = getLocalisedMessage(context, config.message(), "default.message");

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, finalMessage);
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[]{finalYes, finalNo}, 1);

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(textOutputCallback);
        callbacks.add(confirmationCallback);

        Action.ActionBuilder result = outcome.map(this::goTo).orElse(send(callbacks));
        config.stateField().ifPresent(key -> result.replaceSharedState(
                context.sharedState.copy().put(key, confirmationVariable.orElse(null))));
        return result.build();
    }

    /**
     * Resolve the localised message value based on the available localisations and preferences.
     * <p>
     * This method will use the following order of precedence for the resolution of the locale
     * to use for a given message:
     * <ol>
     *     <li>Ordered list of Locales provided by the {@link TreeContext#request}</li>
     *     <li>Locales defined in the Realm Authentication Settings</li>
     *     <li>Locales defined at the Server level</li>
     *     <li>Catch all Default localisation key</li>
     * </ol>
     *
     * @param context non-null context that informs this operation
     * @param localisations mapping of localisations to localised text
     * @param defaultMessageKey default message key in the event we do not locate an appropriate locale
     * @return a possibly null String containing the localised text
     */
    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations,
            String defaultMessageKey) {
        return localizedMessageProvider.getLocalizedMessage(context, MessageNode.class, localisations,
                defaultMessageKey);
    }

    @Override
    public OutputState[] getOutputs() {
        if (config.stateField().isPresent()) {
            return new OutputState[]{new OutputState(config.stateField().get())};
        }
        return new OutputState[]{};
    }
}
