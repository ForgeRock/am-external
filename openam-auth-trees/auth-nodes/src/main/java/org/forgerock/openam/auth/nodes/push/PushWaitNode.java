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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_NUMBER_CHALLENGE_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.nodes.wait.WaitingHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that waits for a Push notification in defined period of time before progressing.
 * If the Push notification is of type Push-to-Challenge it displays the challenge number.
 */
@Node.Metadata(outcomeProvider = PushWaitNode.PushWaitOutcomeProvider.class,
        configClass = PushWaitNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class PushWaitNode implements Node {

    private static final String BUNDLE = PushWaitNode.class.getName();
    private static final int EXIT_PRESSED = 0;
    private static final int EXIT_NOT_PRESSED = 100;

    private final Config config;
    private final WaitingHelper waitingHelper;
    private final LocalizedMessageProvider localizationHelper;

    static final String DEFAULT_WAITING_MESSAGE_KEY = "default.waiting.message";
    static final String DEFAULT_CHALLENGE_MESSAGE_KEY = "default.challenge.message";
    static final String DEFAULT_EXIT_MESSAGE_KEY = "default.exit.message";

    /** The ID for the HiddenCallback containing the number for push challenge. */
    public static final String PUSH_CHALLENGE_CALLBACK_ID = "pushChallengeNumber";

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
            return 5;
        }

        /**
         * The message to display to the user while waiting, keyed on the locale. Falls back to default.waiting.message
         * @return The message to display on the waiting indicator.
         */
        @Attribute(order = 200)
        default Map<Locale, String> waitingMessage() {
            return Collections.emptyMap();
        }

        /**
         * The message to display to the user while waiting if Push Type is Push-to-Challenge, keyed on the locale.
         * Falls back to default.challenge.message.
         * @return The message to display on the waiting indicator.
         */
        @Attribute(order = 300)
        default Map<Locale, String> challengeMessage() {
            return Collections.emptyMap();
        }

        /**
         * The message to display to the user to prompt an exit, keyed on the locale. Falls back to default.exit.message
         * @return The message to display on the exit button.
         */
        @Attribute(order = 400)
        default Map<Locale, String> exitMessage() {
            return Collections.emptyMap();
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @param uuid The UUID of th node.
     * @param realm The realm.
     * @param localizationHelperFactory the localization helper factory.
     */
    @Inject
    public PushWaitNode(@Assisted Config config,
                        @Assisted UUID uuid,
                        @Assisted Realm realm,
                        LocalizedMessageProviderFactory localizationHelperFactory) {
        this.config = config;
        this.localizationHelper = localizationHelperFactory.create(realm);
        this.waitingHelper = new WaitingHelper(uuid, config.secondsToWait());
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (isExited(context)) {
            return complete(context, PushWaitOutcome.EXITED);
        }

        if (waitingHelper.waitTimeCompleted(context.sharedState)) {
            return complete(context, PushWaitOutcome.DONE);
        }

        return sendCallbacks(context);
    }

    private boolean isExited(TreeContext treeContext) {
        return treeContext.getCallback(ConfirmationCallback.class)
                .map(confirmationCallback -> confirmationCallback.getSelectedIndex() == EXIT_PRESSED)
                .orElse(false);
    }

    private Action complete(TreeContext context, PushWaitOutcome outcome) {
        return Action.goTo(outcome.getOutcome().id)
                .replaceSharedState(waitingHelper.clearState(context.sharedState))
                .build();
    }

    private Action sendCallbacks(TreeContext context) {
        List<Callback> callbacks;
        if (context.getStateFor(this).isDefined(PUSH_NUMBER_CHALLENGE_KEY)) {
            String challenge = Objects
                    .requireNonNull(context.getStateFor(this).get(PUSH_NUMBER_CHALLENGE_KEY))
                    .asString();
            callbacks = waitingHelper.createCallbacks(getChallengeMessage(context, challenge));
            HiddenValueCallback hiddenValueCallback = new HiddenValueCallback(
                    PUSH_CHALLENGE_CALLBACK_ID,
                    challenge
            );
            callbacks.add(hiddenValueCallback);
        } else {
            callbacks = waitingHelper.createCallbacks(getWaitingMessage(context));
        }

        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[] {getExitMessage(context)}, EXIT_PRESSED);
        confirmationCallback.setSelectedIndex(EXIT_NOT_PRESSED);
        callbacks.add(confirmationCallback);

        return Action.send(callbacks)
                .replaceSharedState(waitingHelper.getNextState(context.sharedState))
                .build();
    }

    private String getWaitingMessage(TreeContext context) {
        String value = localizationHelper.getLocalizedMessage(context,
                PushWaitNode.class, config.waitingMessage(), DEFAULT_WAITING_MESSAGE_KEY);
        return value.replaceAll("\\{\\{time\\}\\}", String.valueOf(config.secondsToWait()));
    }

    private String getChallengeMessage(TreeContext context, String challenge) {
        String value = localizationHelper.getLocalizedMessage(context,
                PushWaitNode.class, config.challengeMessage(), DEFAULT_CHALLENGE_MESSAGE_KEY);
        return value.replaceAll("\\{\\{challenge\\}\\}", challenge);
    }

    private String getExitMessage(TreeContext context) {
        return localizationHelper.getLocalizedMessage(context,
                PushWaitNode.class, config.exitMessage(), DEFAULT_EXIT_MESSAGE_KEY);
    }

    /**
     * The possible outcomes for the PushWaitNode.
     */
    private enum PushWaitOutcome {
        /**
         * The wait was successfully finished.
         */
        DONE("Done"),
        /**
         * The user chose to leave early.
         */
        EXITED("Exit");

        String displayValue;

        /**
         * Constructor.
         * @param displayValue The value which is displayed to the user.
         */
        PushWaitOutcome(String displayValue) {
            this.displayValue = displayValue;
        }

        private OutcomeProvider.Outcome getOutcome() {
            return new OutcomeProvider.Outcome(name(), displayValue);
        }
    }

    /**
     * Provides the outcomes for the Push wait node.
     */
    public static class PushWaitOutcomeProvider implements StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            List<Outcome> outcomes = new ArrayList<>();
            outcomes.add(PushWaitOutcome.DONE.getOutcome());
            outcomes.add(PushWaitOutcome.EXITED.getOutcome());
            return outcomes;
        }
    }
}
