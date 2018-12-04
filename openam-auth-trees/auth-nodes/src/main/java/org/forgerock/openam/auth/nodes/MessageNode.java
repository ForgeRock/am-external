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
 * Copyright 2018 ForgeRock AS.
 */


package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;

/**
 * Node with configurable message and acknowledgement buttons.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = MessageNode.Config.class)
public class MessageNode extends AbstractDecisionNode {

    private final Config config;
    private final LocaleSelector localeSelector;
    private static final String BUNDLE = MessageNode.class.getName().replace(".", "/");

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
     * @param config The service config.
     * @param localeSelector The locale selector support class.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public MessageNode(@Assisted Config config, LocaleSelector localeSelector) throws NodeProcessException {
        this.config = config;
        this.localeSelector = localeSelector;
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

    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations,
            String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        if (bestLocale != null) {
            return localisations.get(bestLocale);
        } else if (localisations.size() > 0) {
            return localisations.get(localisations.keySet().iterator().next());
        }

        ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(MessageNode.BUNDLE,
                MessageNode.class.getClassLoader());
        return bundle.getString(defaultMessageKey);
    }
}