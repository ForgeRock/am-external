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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.wait.WaitingHelper;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that waits for a defined period of time before progressing.
 */
@Node.Metadata(outcomeProvider  = PollingWaitNode.PollingWaitOutcomeProvider.class,
        configClass      = PollingWaitNode.Config.class)
public class PollingWaitNode implements Node {

    private static final String BUNDLE = PollingWaitNode.class.getName().replace(".", "/");
    private static final int EXIT_PRESSED = 0;
    private static final int EXIT_NOT_PRESSED = 100;
    private final Config config;
    private final LocaleSelector localeSelector;
    private final WaitingHelper waitingHelper;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The number of seconds to wait for.
         * @return The number of seconds.
         */
        @Attribute(order = 100)
        default int secondsToWait() {
            return 8;
        }

        /**
         * Whether to enable spam protection.
         * @return True to enable, false to disable.
         */
        @Attribute(order = 200)
        default boolean spamDetectionEnabled() {
            return false;
        }

        /**
         * The tolerance for spam. The user will be allowed to make up to this number of early requests.
         * @return The spam tolerance.
         */
        @Attribute(order = 300)
        default int spamDetectionTolerance() {
            return 3;
        }

        /**
         * The message to display to the user while waiting, keyed on the locale. Falls back to default.waiting.message
         * @return The message to display on the waiting indicator.
         */
        @Attribute(order = 400)
        default Map<Locale, String> waitingMessage() {
            return Collections.emptyMap();
        }

        /**
         * Whether the user is given the option to exit the request early by pressing a button.
         * @return True to enable, false to disable.
         */
        @Attribute(order = 500)
        default boolean exitable() {
            return false;
        }

        /**
         * The message to display to the user to prompt an exit, keyed on the locale. Falls back to default.exit.message
         * @return The message to display on the exit button.
         */
        @Attribute(order = 600)
        default Map<Locale, String> exitMessage() {
            return Collections.emptyMap();
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @param uuid The UUID of th node.
     * @param localeSelector The locale selector support class.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public PollingWaitNode(@Assisted Config config, @Assisted UUID uuid, LocaleSelector localeSelector)
            throws NodeProcessException {
        this.config = config;
        this.localeSelector = localeSelector;
        this.waitingHelper = new WaitingHelper(uuid, config.secondsToWait());

    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (config.exitable() && isExited(context)) {
            return complete(context, PollingWaitOutcome.EXITED);
        }

        if (config.spamDetectionEnabled()
                && waitingHelper.spamCountExceeds(context.sharedState, config.spamDetectionTolerance())) {
            return complete(context, PollingWaitOutcome.SPAM);
        }

        if (waitingHelper.waitTimeCompleted(context.sharedState)) {
            return complete(context, PollingWaitOutcome.DONE);
        }

        return sendCallbacks(context);
    }

    private boolean isExited(TreeContext treeContext) {
        return treeContext.getCallback(ConfirmationCallback.class)
                .map(confirmationCallback -> confirmationCallback.getSelectedIndex() == EXIT_PRESSED)
                .orElse(false);
    }

    private Action complete(TreeContext context, PollingWaitOutcome outcome) {
        return Action.goTo(outcome.getOutcome().id)
                .replaceSharedState(waitingHelper.clearState(context.sharedState))
                .build();
    }

    private Action sendCallbacks(TreeContext context) {
        List<Callback> callbacks = waitingHelper.createCallbacks(getWaitingMessage(context));
        if (config.exitable()) {
            ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                    new String[] {getExitMessage(context)}, EXIT_PRESSED);
            confirmationCallback.setSelectedIndex(EXIT_NOT_PRESSED);
            callbacks.add(confirmationCallback);
        }
        return Action.send(callbacks)
                .replaceSharedState(waitingHelper.getNextState(context.sharedState))
                .build();
    }


    private String getWaitingMessage(TreeContext context) {
        String value = getLocalisedMessage(context, config.waitingMessage(), "default.waiting.message");
        return value.replaceAll("\\{\\{time\\}\\}", String.valueOf(config.secondsToWait()));
    }

    private String getExitMessage(TreeContext context) {
        return getLocalisedMessage(context, config.exitMessage(), "default.exit.message");
    }

    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations,
                                       String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        if (bestLocale != null) {
            return localisations.get(bestLocale);
        }

        ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(PollingWaitNode.BUNDLE,
                PollingWaitNode.class.getClassLoader());
        return bundle.getString(defaultMessageKey);
    }

    /**
     * The possible outcomes for the PollingWaitNode.
     */
    public enum PollingWaitOutcome {
        /**
         * The wait was successfully finished.
         */
        DONE("Done"),
        /**
         * The user chose to leave early.
         */
        EXITED("Exited"),
        /**
         * The user spammed the node.
         */
        SPAM("Spam");

        String displayValue;

        /**
         * Constructor.
         * @param displayValue The value which is displayed to the user.
         */
        PollingWaitOutcome(String displayValue) {
            this.displayValue = displayValue;
        }

        private OutcomeProvider.Outcome getOutcome() {
            return new OutcomeProvider.Outcome(name(), displayValue);
        }
    }

    /**
     * Provides the outcomes for the polling wait node.
     */
    public static class PollingWaitOutcomeProvider implements OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            List<Outcome> outcomes = new ArrayList<>();
            outcomes.add(PollingWaitOutcome.DONE.getOutcome());
            if (nodeAttributes.isNotNull()) {
                // nodeAttributes is null when the node is created
                if (nodeAttributes.get("exitable").required().asBoolean()) {
                    outcomes.add(PollingWaitOutcome.EXITED.getOutcome());
                }
                if (nodeAttributes.get("spamDetectionEnabled").required().asBoolean()) {
                    outcomes.add(PollingWaitOutcome.SPAM.getOutcome());
                }
            }
            return outcomes;
        }
    }
}