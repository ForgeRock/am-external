/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
        configClass = MeterNode.Config.class)
public class MeterNode extends SingleOutcomeNode {

    private final Logger logger = LoggerFactory.getLogger("amAuth");
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