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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRY_COUNT;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.opendj.ldap.ResultCode.OBJECTCLASS_VIOLATION;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.LifecycleNode;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A decision node that allows to go through the success path only given number of times.
 */
@Node.Metadata(outcomeProvider = RetryLimitDecisionNode.RetryLimitDecisionNodeOutcomeProvider.class,
        configClass = RetryLimitDecisionNode.Config.class,
        tags = {"utilities"})
public class RetryLimitDecisionNode implements Node, LifecycleNode {

    private static final String RETRY = "Retry";
    private static final String REJECT = "Reject";
    private static final String BUNDLE = RetryLimitDecisionNode.class.getName();
    private static final String RETRY_COUNT_ATTRIBUTE = "retryLimitNodeCount";

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The number of times to allow a retry.
         * @return The number of retries.
         */
        @Attribute(order = 100, validators = GreaterThanZeroValidator.class)
        default int retryLimit() {
            return 3;
        }

        /**
         * Flag for whether to store this nodes retry limit on the user for persistence.
         * @return the value of the flag.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default Boolean incrementUserAttributeOnFailure() {
            return true;
        }
    }

    private final Config config;
    private final UUID nodeId;
    private final Logger logger = LoggerFactory.getLogger(RetryLimitDecisionNode.class);
    private final RetryStateHandler retryStateHandler;
    private final NodeUserIdentityProvider identityProvider;
    private int currentRetryCount;

    /**
     * Create the node.
     * @param config The service config.
     * @param nodeId The UUID of this RetryLimitDecisionNode.
     * @param identityProvider The NodeUserIdentityProvider.
     */
    @Inject
    public RetryLimitDecisionNode(@Assisted Config config, @Assisted UUID nodeId,
            NodeUserIdentityProvider identityProvider) {
        this.config = config;
        this.nodeId = nodeId;
        this.identityProvider = identityProvider;
        if (config.incrementUserAttributeOnFailure()) {
            this.retryStateHandler = new UserStoreRetryHandler();
        } else {
            this.retryStateHandler = new SharedStateRetryHandler();
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("RetryLimitDecisionNode started");
        currentRetryCount = retryStateHandler.getCurrentCount(context);

        if (currentRetryCount < config.retryLimit()) {
            retryStateHandler.saveRetryCount(context, currentRetryCount);
            context.getStateFor(this).putShared(nodeRetryCountKey(), currentRetryCount + 1);
            return goTo(RETRY).build();
        } else {
            return retryStateHandler.processReject(context);
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(nodeRetryCountKey()),
            new InputState(USERNAME),
            new InputState(REALM)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(nodeRetryCountKey(), Map.of(RETRY, true, REJECT, false))
        };
    }

    private String nodeRetryCountKey() {
        return nodeId + "." + RETRY_COUNT;
    }

    /**
     * Provides a static set of outcomes for decision nodes.
     */
    public static class RetryLimitDecisionNodeOutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
            return ImmutableList.of(
                    new Outcome(RETRY, bundle.getString("retryOutcome")),
                    new Outcome(REJECT, bundle.getString("rejectOutcome")));
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(field("retryCount", String.valueOf(currentRetryCount))));
    }

    @Override
    public void onTreeComplete(TreeContext context, boolean success) throws NodeProcessException {
        if (success) {
            retryStateHandler.clearAttribute(context);
        }
    }

    private Optional<AMIdentity> getIdentityFromContext(TreeContext context) {
        return identityProvider.getAMIdentity(context.universalId, context.getStateFor(this));
    }

    private interface RetryStateHandler {

        int getCurrentCount(TreeContext context) throws NodeProcessException;

        void saveRetryCount(TreeContext context, int currentRetryCount) throws NodeProcessException;

        Action processReject(TreeContext context) throws NodeProcessException;

        void clearAttribute(TreeContext context) throws NodeProcessException;
    }

    private final class SharedStateRetryHandler implements RetryStateHandler {
        @Override
        public int getCurrentCount(TreeContext context) {
            JsonValue retryCountFromSharedState = context.getStateFor(RetryLimitDecisionNode.this)
                                                          .get(nodeRetryCountKey());
            return Optional.ofNullable(retryCountFromSharedState)
                           .map(JsonValue::asInteger)
                           .orElse(0);
        }

        @Override
        public void saveRetryCount(TreeContext context, int currentRetryCount) {
            // no action
        }

        @Override
        public Action processReject(TreeContext context) {
            return goTo(REJECT).build();
        }

        @Override
        public void clearAttribute(TreeContext context) {
            // no action
        }
    }

    private final class UserStoreRetryHandler implements RetryStateHandler {
        @Override
        public int getCurrentCount(TreeContext context) {
            return getIdentityFromContext(context)
                        .map(identity -> {
                            try {
                                Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);
                                if (retryLimitNodeCounts != null && !retryLimitNodeCounts.isEmpty()) {
                                    Optional<String> nodeCount = retryLimitNodeCounts.stream()
                                                                         .filter(s -> s.startsWith(nodeId.toString()))
                                                                         .findFirst();
                                    return nodeCount.map(s -> Integer.parseInt(s.split("=")[1])).orElse(0);
                                }
                                return 0;
                            } catch (IdRepoException | SSOException e) {
                                logger.warn("Error getting current retry count", e);
                                return 0;
                            }
                        })
                           .or(() -> Optional.ofNullable(context.getStateFor(RetryLimitDecisionNode.this)
                                                                 .get(nodeRetryCountKey()))
                                             .map(JsonValue::asInteger))
                           .orElse(0);
        }

        @Override
        public void saveRetryCount(TreeContext context, int currentRetryCount) {
            setRetryCountOnUserAttribute(context, currentRetryCount + 1);
        }

        @Override
        public Action processReject(TreeContext context) throws NodeProcessException {
            clearAttribute(context);
            return goTo(REJECT).build();
        }

        @Override
        public void clearAttribute(TreeContext context) throws NodeProcessException {
            try {
                Optional<AMIdentity> amIdentity = identityProvider.getAMIdentity(context.universalId,
                        context.getStateFor(RetryLimitDecisionNode.this));
                if (amIdentity.isPresent()) {
                    AMIdentity identity = amIdentity.get();
                    Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);

                    retryLimitNodeCounts.removeIf(s -> s.startsWith(nodeId.toString()));
                    identity.setAttributes(Collections.singletonMap(RETRY_COUNT_ATTRIBUTE, retryLimitNodeCounts));
                    identity.store();
                }
            } catch (IdRepoException | SSOException e) {
                logger.warn("Error clearing attribute", e);
                throw new NodeProcessException("attribute.failure");
            }
        }

        private void setRetryCountOnUserAttribute(TreeContext context, Integer value) {
            getIdentityFromContext(context).ifPresent(identity -> {
                try {
                    Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);

                    // Remove any existing entries for this nodes ID
                    retryLimitNodeCounts.removeIf(s -> s.startsWith(nodeId.toString()));

                    // Add an entry for this nodeID and store in user attribute
                    retryLimitNodeCounts.add(nodeId + "=" + value);
                    identity.setAttributes(Collections.singletonMap(RETRY_COUNT_ATTRIBUTE, retryLimitNodeCounts));
                    identity.store();
                } catch (IdRepoException e) {
                    if (OBJECTCLASS_VIOLATION.intValue() == e.getLdapErrorIntCode()) {
                        logger.error("Failed to save retryLimitNodeCount to user: "
                                             + "Identity Repo has not been upgraded.");
                    } else {
                        logger.error("Error setting retry count on user attribute", e);
                    }
                } catch (SSOException e) {
                    logger.error("Error setting retry count on user attribute", e);
                }
            });
        }
    }
}
