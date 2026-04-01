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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Objects.requireNonNull;
import static org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthErrorCode.TRANSACTION_EXPIRED;
import static org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthErrorCode.TRANSACTION_NOT_FOUND;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.JOURNEY_SESSION_PROPERTIES;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.REDIRECT_URI_STATE_KEY;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.TRANSACTION_ID_STATE_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.forgerock.am.authn.backchannel.BackChannelTransactionInformationResponseData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionService;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.transactions.core.TransactionResult;
import org.forgerock.openam.transactions.core.TransactionState;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * This node checks the status of a back channel transaction and returns the appropriate outcome based on the status.
 */
@Node.Metadata(outcomeProvider = BackChannelStatusNode.BackChannelStatusOutcomeProvider.class,
        configClass = BackChannelStatusNode.Config.class, tags =  {"utilities", "backchannel"})
public class BackChannelStatusNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(BackChannelStatusNode.class);
    private final Config config;
    private final BackChannelTransactionService backChannelAuthnService;

    /**
     * Configuration for the JourneyStatusNode.
     */
    public interface Config {

        /**
         * Whether to record the session data on success.
         * @return true if the session data should be recorded, false otherwise.
         */
        @Attribute(order = 100, requiredValue = true)
        default boolean recordData() {
            return true;
        }
    }

    /**
     * Outcome provider for the JourneyStatusNode.
     */
    public static class BackChannelStatusOutcomeProvider implements StaticOutcomeProvider {

        private static final String PENDING = "pending";
        private static final String IN_PROGRESS = "inprogress";
        private static final String SUCCESS = "success";
        private static final String FAILURE = "failure";
        private static final String UNKNOWN = "unknown";
        private static final String ERROR = "error";

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            var bundle = locales.getBundleInPreferredLocale(BackChannelStatusNode.class.getName(),
                    BackChannelStatusNode.class.getClassLoader());
            return List.of(
                    new Outcome(PENDING, bundle.getString(PENDING)),
                    new Outcome(IN_PROGRESS, bundle.getString(IN_PROGRESS)),
                    new Outcome(SUCCESS, bundle.getString(SUCCESS)),
                    new Outcome(FAILURE, bundle.getString(FAILURE)),
                    new Outcome(UNKNOWN, bundle.getString(UNKNOWN)),
                    new Outcome(ERROR, bundle.getString(ERROR))
            );
        }
    }

    @Inject
    BackChannelStatusNode(@Assisted BackChannelStatusNode.Config config,
            BackChannelTransactionService backChannelAuthnService) {
        this.config = config;
        this.backChannelAuthnService = backChannelAuthnService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        if (!nodeState.isDefined(TRANSACTION_ID_STATE_KEY)) {
            logger.warn("Transaction ID not found in node state");
            return Action.goTo(BackChannelStatusOutcomeProvider.ERROR).build();
        }
        String transaction = requireNonNull(nodeState.get(TRANSACTION_ID_STATE_KEY)).asString();
        try {
            BackChannelTransactionInformationResponseData status = backChannelAuthnService.checkStatus(transaction);
            return switch (TransactionState.valueOf(status.state())) {
            case CREATED -> Action.goTo(BackChannelStatusOutcomeProvider.PENDING).build();
            case IN_PROGRESS -> Action.goTo(BackChannelStatusOutcomeProvider.IN_PROGRESS).build();
            case COMPLETED -> {
                if (config.recordData()) {
                    if (status.sessionProperties() == null || status.sessionProperties().isEmpty()) {
                        logger.info("Skipping non-existent session properties");
                    } else {
                        nodeState.putTransient(JOURNEY_SESSION_PROPERTIES, status.sessionProperties());
                    }
                }
                nodeState.remove(TRANSACTION_ID_STATE_KEY);
                nodeState.remove(REDIRECT_URI_STATE_KEY);
                yield switch (TransactionResult.valueOf(status.result())) {
                    case APPROVED -> Action.goTo(BackChannelStatusOutcomeProvider.SUCCESS).build();
                    case DENIED -> Action.goTo(BackChannelStatusOutcomeProvider.FAILURE).build();
                    default -> {
                        logger.warn("Unexpected transaction result: {}", status.result());
                        yield Action.goTo(BackChannelStatusOutcomeProvider.ERROR).build();
                    }
                    };
            }
            };
        } catch (BackChannelAuthException e) {
            if (TRANSACTION_NOT_FOUND.equals(e.errorCode()) || TRANSACTION_EXPIRED.equals(e.errorCode())) {
                return Action.goTo(BackChannelStatusOutcomeProvider.UNKNOWN).build();
            }
            logger.error("Error checking back channel transaction status", e);
            return Action.goTo(BackChannelStatusOutcomeProvider.ERROR).build();
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
                new InputState(TRANSACTION_ID_STATE_KEY, true)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        List<OutputState> outputs = new ArrayList<>();
        if (config.recordData()) {
            outputs.add(new OutputState(JOURNEY_SESSION_PROPERTIES,
                    Map.of(BackChannelStatusOutcomeProvider.SUCCESS, true,
                            BackChannelStatusOutcomeProvider.FAILURE, false,
                            BackChannelStatusOutcomeProvider.PENDING, false,
                            BackChannelStatusOutcomeProvider.IN_PROGRESS, false,
                            BackChannelStatusOutcomeProvider.UNKNOWN, false,
                            BackChannelStatusOutcomeProvider.ERROR, false)));
        }
        return outputs.toArray(OutputState[]::new);
    }
}
