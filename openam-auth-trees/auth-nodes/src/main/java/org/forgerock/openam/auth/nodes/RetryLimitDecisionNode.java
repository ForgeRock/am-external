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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRIES_REMAINING;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A decision node that allows to go through the success path only given number of times.
 */
@Node.Metadata(outcomeProvider = RetryLimitDecisionNode.RetryLimitDecisionNodeOutcomeProvider.class,
        configClass = RetryLimitDecisionNode.Config.class)
public class RetryLimitDecisionNode implements Node {

    private static final String RETRY = "Retry";
    private static final String REJECT = "Reject";
    private static final String BUNDLE = RetryLimitDecisionNode.class.getName().replace(".", "/");
    private int retryLimitCount;

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
    }

    private final Config config;
    private final UUID nodeId;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Create the node.
     * @param config The service config.
     * @param nodeId The UUID of this RetryLimitDecisionNode.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public RetryLimitDecisionNode(@Assisted Config config, @Assisted UUID nodeId) throws NodeProcessException {
        this.config = config;
        this.nodeId = nodeId;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("RetryLimitDecisionNode started");
        JsonValue retryLimit = context.sharedState.get(nodeRetryLimitKey());
        logger.debug("retryLimit {}", retryLimit.getObject());
        JsonValue newSharedState = context.sharedState.copy();
        if (retryLimit.isNull() || retryLimit.asInteger() > 0) {
            retryLimitCount = Optional.ofNullable(retryLimit.asInteger())
                    .orElse(config.retryLimit()) - 1;
            newSharedState.put(nodeRetryLimitKey(), retryLimitCount);
            return goTo(RETRY).replaceSharedState(newSharedState).build();
        } else {
            return goTo(REJECT).build();
        }
    }

    private String nodeRetryLimitKey() {
        return nodeId + "." + RETRIES_REMAINING;
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
        return json(object(field("remainingRetries", String.valueOf(retryLimitCount))));
    }
}
