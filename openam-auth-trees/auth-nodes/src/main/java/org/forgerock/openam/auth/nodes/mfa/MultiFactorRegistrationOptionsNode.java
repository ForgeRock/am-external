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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.MFA_METHOD;

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

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

/**
 * The Multi-Factor Registration Options node provides to the authenticated user a set of options to
 * register the device for the previously selected second factor authenticator method.
 */
@Node.Metadata(outcomeProvider = MultiFactorRegistrationOptionsNode.OutcomeProvider.class,
        configClass = MultiFactorRegistrationOptionsNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class MultiFactorRegistrationOptionsNode implements Node {

    /** Default message for Register device for 2FA. */
    static final String DEFAULT_MESSAGE_KEY = "default.message";
    /** Default for Register Device option. */
    static final String DEFAULT_REGISTER_DEVICE_OPTION_KEY = "default.registerDeviceLabel";
    /** Default for Get App option. */
    static final String DEFAULT_GET_APP_OPTION_KEY = "default.getAppLabel";
    /** Default for Skip step option. */
    static final String DEFAULT_SKIP_STEP_OPTION_KEY = "default.skipStepLabel";
    /** Default for Opt-out option. */
    static final String DEFAULT_OPT_OUT_OPTION_KEY = "default.optOutLabel";
    /** Default enable get app outcome. */
    static final boolean DEFAULT_GET_APP = true;
    /** Default disable skip outcome. */
    static final boolean DEFAULT_MANDATORY = false;
    /** Option begin the registration process now. */
    static final int REGISTER_START_REGISTRATION_OPTION = 0;
    /** Option to navigate to the get the app page. */
    static final int REGISTER_GET_APP_OPTION = 1;
    /** Option to skip the registration module if 2FA is not mandatory. */
    static final int REGISTER_SKIP_OPTION = 2;
    /** Option to opt-out the registration module if 2FA is not mandatory. */
    static final int REGISTER_OPT_OUT_OPTION = 3;

    private static final Logger logger = LoggerFactory.getLogger(MultiFactorRegistrationOptionsNode.class);
    private static final String BUNDLE = MultiFactorRegistrationOptionsNode.class.getName();

    private final Config config;
    private final LocaleSelector localeSelector;

    /**
     * The node constructor.
     *
     * @param config the node configuration.
     * @param localeSelector a LocaleSelector for choosing the correct message to display.
     */
    @Inject
    public MultiFactorRegistrationOptionsNode(@Assisted Config config, LocaleSelector localeSelector) {
        this.config = config;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("MultiFactorRegistrationOptionsNode started.");

        Optional<ConfirmationCallback> confirmationCallback = context.getCallback(ConfirmationCallback.class);

        if (confirmationCallback.isPresent()) {
            logger.debug("Handles user option selection.");
            return handleSelectedOption(context);
        } else {
            String username = context.sharedState.get(SharedStateConstants.USERNAME).asString();
            if (StringUtils.isBlank(username)) {
                logger.debug("No username found.");
                throw new NodeProcessException("Expected username to be set.");
            }

            String mfaMethod = context.sharedState.get(MFA_METHOD).asString();
            if (StringUtils.isBlank(mfaMethod)) {
                logger.debug("No multi-factor authentication method found.");
                throw new NodeProcessException("Expected multi-factor authentication method to be set.");
            }

            logger.debug("User '{}' not registered for MFA. Displaying registration options.", username);
            String[] registerOptions = getRegisterOptions(context);
            List<Callback> callbacks = ImmutableList.of(
                    new TextOutputCallback(
                            TextOutputCallback.INFORMATION,
                            getLocalisedMessage(context, config.message(), DEFAULT_MESSAGE_KEY)),
                    new ConfirmationCallback(
                            ConfirmationCallback.INFORMATION,
                            registerOptions,
                            0)
            );
            return send(callbacks).build();
        }
    }

    /**
     * Builds an Action for the passed Outcome.
     *
     * @return the next action to perform.
     */
    private Action buildAction(String outcome) {
        Action.ActionBuilder builder = Action.goTo(outcome);
        return builder.build();
    }

    /**
     * Handles the user selected option on the confirmation callback.
     *
     * @param context the context of the tree authentication.
     * @return the next action to perform.
     */
    private Action handleSelectedOption(TreeContext context) throws NodeProcessException {
        Optional<ConfirmationCallback> confirmationCallback = context.getCallback(ConfirmationCallback.class);
        String mfaMethod = context.sharedState.get(MFA_METHOD).asString();

        int userSelection = confirmationCallback.get().getSelectedIndex();
        if (userSelection == REGISTER_START_REGISTRATION_OPTION) {
            logger.debug("Registration option selected.");
            return buildAction(OutcomeProvider.REGISTER_OUTCOME);
        } else if ((userSelection == REGISTER_GET_APP_OPTION) && config.getApp()) {
            logger.debug("Get App option selected.");
            return buildAction(OutcomeProvider.GET_APP_OUTCOME);
        } else if (((userSelection == REGISTER_SKIP_OPTION) && config.getApp())
                || ((userSelection == (REGISTER_SKIP_OPTION - 1)) && !config.getApp())) {
            logger.debug("Skip this time option selected.");
            return buildAction(OutcomeProvider.SKIP_OUTCOME);
        } else if (((userSelection == REGISTER_OPT_OUT_OPTION) && config.getApp())
                || ((userSelection == (REGISTER_OPT_OUT_OPTION - 1)) && !config.getApp())) {
            logger.debug("Opt-out option selected.");
            JsonValue sharedState = context.sharedState
                    .copy()
                    .put(MFA_METHOD, mfaMethod);
            return Action.goTo(OutcomeProvider.OPT_OUT_OUTCOME)
                    .replaceSharedState(sharedState)
                    .build();
        } else {
            throw new NodeProcessException("Unrecognized option: "
                    + confirmationCallback.get().getSelectedIndex());
        }
    }

    /**
     * Create the registration options for the ConfirmationCallback.
     *
     * @param context the context of the tree authentication.
     * @return the array of String containing registration options.
     */
    private String[] getRegisterOptions(TreeContext context) {
        List<String> list = new ArrayList<>();
        list.add(getLocalisedMessage(context, config.registerDeviceLabel(), DEFAULT_REGISTER_DEVICE_OPTION_KEY));
        if (config.getApp()) {
            list.add(getLocalisedMessage(context, config.getAppLabel(), DEFAULT_GET_APP_OPTION_KEY));
        }
        if (!config.mandatory()) {
            list.add(getLocalisedMessage(context, config.skipStepLabel(), DEFAULT_SKIP_STEP_OPTION_KEY));
            list.add(getLocalisedMessage(context, config.optOutLabel(), DEFAULT_OPT_OUT_OPTION_KEY));
        }
        return list.toArray(String[]::new);
    }

    /**
     * Get the localized message.
     *
     * @param context the context of the tree authentication.
     * @param localisations the localization map
     * @param defaultMessageKey the key for the default message in the resource bundle.
     * @return the localized string.
     */
    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations,
            String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        if (bestLocale != null) {
            return localisations.get(bestLocale);
        } else if (localisations.size() > 0) {
            return localisations.get(localisations.keySet().iterator().next());
        }

        ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(MultiFactorRegistrationOptionsNode.BUNDLE,
                MultiFactorRegistrationOptionsNode.class.getClassLoader());
        return bundle.getString(defaultMessageKey);
    }

    /**
     * The node configuration.
     */
    public interface Config {

        /**
         * Allows to remove the 'skip' outcome.
         *
         * @return enforces this node in the tree.
         */
        @Attribute(order = 10)
        default boolean mandatory() {
            return DEFAULT_MANDATORY;
        }

        /**
         * Specifies whether to display the authenticator app links.
         *
         * @return true if the authenticator app links will be displayed.
         */
        @Attribute(order = 20)
        default boolean getApp() {
            return DEFAULT_GET_APP;
        }

        /**
         * The message to displayed on device registration.
         *
         * @return the message.
         */
        @Attribute(order = 30)
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }

        /**
         * The label for Register Device button.
         *
         * @return the label.
         */
        @Attribute(order = 40)
        default Map<Locale, String> registerDeviceLabel() {
            return Collections.emptyMap();
        }

        /**
         * The label for Get the App button.
         *
         * @return the label.
         */
        @Attribute(order = 50)
        default Map<Locale, String> getAppLabel() {
            return Collections.emptyMap();
        }

        /**
         * The label for Skip this time button.
         *
         * @return the label.
         */
        @Attribute(order = 60)
        default Map<Locale, String> skipStepLabel() {
            return Collections.emptyMap();
        }

        /**
         * The label for Opt-out button.
         *
         * @return the label.
         */
        @Attribute(order = 70)
        default Map<Locale, String> optOutLabel() {
            return Collections.emptyMap();
        }
    }

    /**
     * Provides the push registration node's set of outcomes.
     */
    public static final class OutcomeProvider implements BoundedOutcomeProvider {
        /**
         * Outcomes Ids for this node.
         */
        static final String REGISTER_OUTCOME = "registerOutcome"; // register
        static final String SKIP_OUTCOME = "skipOutcome"; // skip registration
        static final String OPT_OUT_OUTCOME = "optOutOutcome"; // skip registration
        static final String GET_APP_OUTCOME = "getAppOutcome"; // get authenticator app

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

            return getAllOutcomes(locales).stream()
                           .filter(outcome -> {
                               if (GET_APP_OUTCOME.equals(outcome.id)) {
                                   return nodeAttributes.isNull()
                                                  || nodeAttributes.get("getApp").required().asBoolean();
                               }
                               if (SKIP_OUTCOME.equals(outcome.id) || OPT_OUT_OUTCOME.equals(outcome.id)) {
                                   return nodeAttributes.isNull()
                                                  || !nodeAttributes.get("mandatory").required().asBoolean();
                               }
                               return true;
                           }).toList();
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return List.of(
                    new Outcome(REGISTER_OUTCOME, bundle.getString(REGISTER_OUTCOME)),
                    new Outcome(GET_APP_OUTCOME, bundle.getString(GET_APP_OUTCOME)),
                    new Outcome(SKIP_OUTCOME, bundle.getString(SKIP_OUTCOME)),
                    new Outcome(OPT_OUT_OUTCOME, bundle.getString(OPT_OUT_OUTCOME))
            );
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(USERNAME),
            new InputState(MFA_METHOD)
        };
    }
}
