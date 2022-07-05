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
 * Copyright 2017-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.monitoring.api.instrument.MeterRegistry;
import org.forgerock.monitoring.api.instrument.Timer;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which calculates time-elapsed since {@link TimerStopNode} was run and records this elapsed time in a metric.
 *
 * This node should be used with {@link TimerStartNode}.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = TimerStopNode.Config.class,
        tags = {"metrics", "utilities"})
public class TimerStopNode extends SingleOutcomeNode {

    private final Logger logger;
    private final Config config;
    private final Timer timer;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Identifier of property from which start time should be read.
         *
         * Defaults to {@literal "TimerNodeStartTime"}
         *
         * @return property name.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String startTimeProperty() {
            return DEFAULT_START_TIME_PROPERTY;
        }

        /**
         * Identifier of metric to update when processing this node e.g. {@literal "authentication.iproov"}.
         *
         * @return metric key name.
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        String metricKey();
    }

    /**
     * Constructs a new {@link TimerStopNode} instance.
     *
     * @param config
     *          Node configuration.
     * @param meterRegistry
     *          The metric registry.
     */
    @Inject
    public TimerStopNode(@Assisted TimerStopNode.Config config, MeterRegistry meterRegistry) {
        this(config, meterRegistry, LoggerFactory.getLogger(Config.class));
    }

    @VisibleForTesting
    TimerStopNode(TimerStopNode.Config config, MeterRegistry meterRegistry, Logger logger) {
        this.logger = logger;
        Reject.ifTrue(isEmpty(config.startTimeProperty()), "TimerStopNode config must define start time property key");
        Reject.ifTrue(isEmpty(config.metricKey()), "TimerStopNode config must define metric key");
        this.config = config;
        this.timer = Timer.builder(config.metricKey())
                .description("authentication tree TimerStopNode timer")
                .register(meterRegistry);
    }

    @Override
    public Action process(TreeContext context) {
        Action.ActionBuilder actionBuilder = goToNext();

        Long startTime = context.sharedState.get(config.startTimeProperty()).asLong();
        if (startTime == null) {
            logger.error("{} could not find start time in {}",
                    TimerStopNode.class.getSimpleName(), config.startTimeProperty());
            return actionBuilder.build();
        }

        long elapsedTime = currentTimeMillis() - startTime;
        logger.debug("{} recording elapsed time {}ms (since {} in property {})",
                TimerStopNode.class.getSimpleName(), elapsedTime, startTime, config.startTimeProperty());

        timer.record(elapsedTime, TimeUnit.MILLISECONDS);

        return actionBuilder.build();
    }

    @Override
    public InputState[] getInputs() {
        if (config.startTimeProperty() != null) {
            return new InputState[]{
                new InputState(config.startTimeProperty())
            };
        }
        return super.getInputs();
    }
}
