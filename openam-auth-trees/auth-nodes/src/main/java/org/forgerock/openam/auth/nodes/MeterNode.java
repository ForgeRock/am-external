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
 * Copyright 2017-2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.utils.StringUtils.isEmpty;

import javax.inject.Inject;

import org.forgerock.monitoring.api.instrument.DistributionSummary;
import org.forgerock.monitoring.api.instrument.MeterRegistry;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which increments a metric.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = MeterNode.Config.class,
        tags = {"metrics", "utilities"})
public class MeterNode extends SingleOutcomeNode {

    private final Logger logger = LoggerFactory.getLogger(MeterNode.class);
    private final Config config;
    private final DistributionSummary distributionSummary;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Identifier of metric to update when processing this node e.g. {@literal "authentication.user-agent.chrome"}.
         *
         * @return a map of properties.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        String metricKey();
    }

    /**
     * Constructs a new {@link MeterNode} instance.
     *
     * @param config
     *          Node configuration.
     * @param meterRegistry
     *          The metric registry.
     */
    @Inject
    public MeterNode(
            @Assisted MeterNode.Config config,
            MeterRegistry meterRegistry) {
        Reject.ifTrue(isEmpty(config.metricKey()), "MeterNode config must define metric key");
        this.config = config;
        this.distributionSummary = DistributionSummary.builder(config.metricKey())
                .description("authentication tree MeterNode meter")
                .register(meterRegistry);
    }

    @Override
    public Action process(TreeContext context) {
        Action.ActionBuilder actionBuilder = goToNext();
        logger.debug("{} incrementing {}", MeterNode.class.getSimpleName(), config.metricKey());
        distributionSummary.record();
        return actionBuilder.build();
    }

}
