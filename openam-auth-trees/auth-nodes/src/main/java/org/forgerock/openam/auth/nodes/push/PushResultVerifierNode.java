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
 * Copyright 2018-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CONTENT_KEY;
import static org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.PushResultVerifierOutcome.EXPIRED;
import static org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.PushResultVerifierOutcome.FALSE;
import static org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.PushResultVerifierOutcome.TRUE;
import static org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.PushResultVerifierOutcome.WAITING;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.Node.Metadata;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.Config;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode.PushReceiveOutcomeProvider;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * This authentication node checks whether a previously sent push notification has been responded to already. There are
 * three possible outcomes of this verification process:
 * <ul>
 * <li>TRUE: The push notification has been approved by the end-user.</li>
 * <li>FALSE: The push notification was explicitly denied by the end-user.</li>
 * <li>EXPIRED: The push notification has expired.</li>
 * <li>WAITING: The push notification has not been reacted to just yet, the node should wait before checking the
 * result again.</li>
 * </ul>
 */
@Metadata(outcomeProvider = PushReceiveOutcomeProvider.class, configClass = Config.class)
public class PushResultVerifierNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushResultVerifierNode.class);
    private static final String BUNDLE = PushResultVerifierNode.class.getName();
    private final PushNotificationService pushNotificationService;
    private final MessageIdFactory messageIdFactory;

    /**
     * The authentication node configuration.
     */
    public interface Config {
    }

    /**
     * Guice injected constructor.
     *
     * @param pushNotificationService The push notification service.
     * @param messageIdFactory The message key factory.
     */
    @Inject
    public PushResultVerifierNode(PushNotificationService pushNotificationService,
            MessageIdFactory messageIdFactory) {
        this.pushNotificationService = pushNotificationService;
        this.messageIdFactory = messageIdFactory;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (!context.sharedState.isDefined(MESSAGE_ID_KEY)) {
            LOGGER.error("Unable to find push message ID in sharedState");
            throw new NodeProcessException("Unable to find push message ID");
        }
        String pushMessageId = context.sharedState.get(MESSAGE_ID_KEY).asString();
        String realm = context.sharedState.get(REALM).asString();
        try {
            MessageId messageId = messageIdFactory.create(pushMessageId, realm);
            ClusterMessageHandler messageHandler = pushNotificationService.getMessageHandlers(realm)
                    .get(messageId.getMessageType());
            if (messageHandler == null) {
                LOGGER.error("The push message corresponds to {} message type which is not registered in the {} realm",
                        messageId.getMessageType(), realm);
                return finishNode(FALSE, context);
            }
            MessageState state = messageHandler.check(messageId);

            JsonValue newSharedState = context.sharedState.copy();
            if (state == null) {
                LOGGER.debug("The push message with ID {} has timed out", messageId.toString());
                return finishNode(EXPIRED, context);
            }

            switch (state) {
            case SUCCESS:
                JsonValue pushContent = messageHandler.getContents(messageId);
                messageHandler.delete(messageId);
                if (pushContent != null) {
                    newSharedState.put(PUSH_CONTENT_KEY, pushContent);
                }
                return finishNode(TRUE, context);
            case DENIED:
                messageHandler.delete(messageId);
                return finishNode(FALSE, context);
            case UNKNOWN:
                return Action.goTo(WAITING.name()).build();
            default:
                throw new NodeProcessException("Unrecognized push message status: " + state);
            }
        } catch (PushNotificationException | CoreTokenException ex) {
            throw new NodeProcessException("An unexpected error occurred while verifying the push result", ex);
        }
    }

    private Action finishNode(PushResultVerifierOutcome outcome, TreeContext context) {
        JsonValue newSharedState = context.sharedState.copy();
        newSharedState.remove(MESSAGE_ID_KEY);
        Action.ActionBuilder builder = goTo(outcome.name());
        builder.replaceSharedState(newSharedState);
        if (outcome == TRUE) {
            builder.addNodeType(context, "AuthenticatorPush");
        }
        return builder.build();
    }

    /**
     * The possible outcomes of the push result verifier node.
     */
    public enum PushResultVerifierOutcome {

        /**
         * Push authentication successful.
         */
        TRUE("true"),
        /**
         * Push authentication explicitly denied or timed out.
         */
        FALSE("false"),
        /**
         * Push authentication expired.
         */
        EXPIRED("expired"),
        /**
         * Push authentication is not yet complete, should wait before checking the result again.
         */
        WAITING("waiting");

        private final String displayName;

        PushResultVerifierOutcome(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Provides the possible outcomes for the push result verifier node.
     */
    public static class PushReceiveOutcomeProvider implements OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            List<Outcome> outcomes = Arrays.stream(PushResultVerifierOutcome.values())
                    .map(outcome -> new Outcome(outcome.name(),
                            bundle.getString(outcome.displayName + "Outcome")))
                    .collect(Collectors.toList());
            return ImmutableList.copyOf(outcomes);
        }
    }
}
