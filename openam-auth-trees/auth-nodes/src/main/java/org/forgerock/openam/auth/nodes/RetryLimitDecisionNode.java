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
 * Copyright 2017-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRY_COUNT;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;
import static org.forgerock.opendj.ldap.ResultCode.OBJECTCLASS_VIOLATION;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LifecycleNode;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

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
    private final CoreWrapper coreWrapper;
    private final IdentityUtils identityUtils;
    private final RetryStateHandler retryStateHandler;
    private int currentRetryCount;

    /**
     * Create the node.
     * @param config The service config.
     * @param nodeId The UUID of this RetryLimitDecisionNode.
     * @param coreWrapper the core wrapper
     * @param identityUtils the identity utils
     */
    @Inject
    public RetryLimitDecisionNode(@Assisted Config config, @Assisted UUID nodeId, CoreWrapper coreWrapper,
                                  IdentityUtils identityUtils) {
        this.config = config;
        this.nodeId = nodeId;
        this.coreWrapper = coreWrapper;
        this.identityUtils = identityUtils;
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
            JsonValue newSharedState = context.sharedState.copy();
            newSharedState.put(nodeRetryCountKey(), currentRetryCount + 1);
            return goTo(RETRY).replaceSharedState(newSharedState).build();
        } else {
            return retryStateHandler.processReject(context);
        }
    }

    private String nodeRetryCountKey() {
        return nodeId + "." + RETRY_COUNT;
    }

    /**
     * Provides a static set of outcomes for decision nodes.
     */
    public static class RetryLimitDecisionNodeOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
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

    private AMIdentity getIdentityFromContext(TreeContext context) throws NodeProcessException {
        Optional<AMIdentity> identity = getAMIdentity(context, identityUtils, coreWrapper);
        if (identity.isEmpty()) {
            logger.warn("identity not found");
            throw new NodeProcessException("identity.failure");
        }

        return identity.get();
    }

    private interface RetryStateHandler {

        int getCurrentCount(TreeContext context) throws NodeProcessException;

        void saveRetryCount(TreeContext context, int currentRetryCount) throws NodeProcessException;

        Action processReject(TreeContext context) throws NodeProcessException;

        void clearAttribute(TreeContext context) throws NodeProcessException;
    }

    private class SharedStateRetryHandler implements RetryStateHandler {
        @Override
        public int getCurrentCount(TreeContext context) {
            JsonValue retryCountFromSharedState = context.sharedState.get(nodeRetryCountKey());
            return Optional.ofNullable(retryCountFromSharedState.asInteger())
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

    private class UserStoreRetryHandler implements RetryStateHandler {
        @Override
        public int getCurrentCount(TreeContext context) throws NodeProcessException {
            try {
                AMIdentity identity = getIdentityFromContext(context);
                Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);
                if (retryLimitNodeCounts != null && !retryLimitNodeCounts.isEmpty()) {
                    Optional<String> nodeCount = retryLimitNodeCounts.stream()
                            .filter(s -> s.startsWith(nodeId.toString()))
                            .findFirst();
                    return nodeCount.isPresent() ? Integer.parseInt(nodeCount.get().split("=")[1]) : 0;
                }
                return 0;
            } catch (IdRepoException | SSOException | NodeProcessException e) {
                logger.warn("Error getting current retry count", e);
                throw new NodeProcessException("attribute.failure");
            }
        }

        @Override
        public void saveRetryCount(TreeContext context, int currentRetryCount) throws NodeProcessException {
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
                Optional<AMIdentity> amIdentity = getAMIdentity(context, identityUtils, coreWrapper);
                if (amIdentity.isPresent()) {
                    AMIdentity identity = amIdentity.get();
                    Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);

                    retryLimitNodeCounts.removeAll(
                            retryLimitNodeCounts.stream()
                                    .filter(s -> s.startsWith(nodeId.toString()))
                                    .collect(Collectors.toList()));
                    identity.setAttributes(Collections.singletonMap(RETRY_COUNT_ATTRIBUTE, retryLimitNodeCounts));
                    identity.store();
                }
            } catch (IdRepoException | SSOException e) {
                logger.warn("Error clearing attribute", e);
                throw new NodeProcessException("attribute.failure");
            }
        }

        private void setRetryCountOnUserAttribute(TreeContext context, Integer value) throws NodeProcessException {
            try {
                AMIdentity identity = getIdentityFromContext(context);
                Set<String> retryLimitNodeCounts = identity.getAttribute(RETRY_COUNT_ATTRIBUTE);

                // Remove any existing entries for this nodes ID
                retryLimitNodeCounts.removeAll(
                        retryLimitNodeCounts.stream()
                                .filter(s -> s.startsWith(nodeId.toString()))
                                .collect(Collectors.toList()));

                // Add an entry for this nodeID and store in user attribute
                retryLimitNodeCounts.add(nodeId + "=" + value);
                identity.setAttributes(Collections.singletonMap(RETRY_COUNT_ATTRIBUTE, retryLimitNodeCounts));
                identity.store();
            } catch (IdRepoException e) {
                if (OBJECTCLASS_VIOLATION.intValue() == e.getLdapErrorIntCode()) {
                    logger.debug("Failed to save retryLimitNodeCount to user: Identity Repo has not been upgraded.");
                    return;
                }
                throw new NodeProcessException("attribute.failure");
            } catch (SSOException | NodeProcessException e) {
                throw new NodeProcessException("attribute.failure");
            }
        }
    }
}
