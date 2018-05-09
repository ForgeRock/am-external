/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.monitoring.api.instrument.MeterRegistry;
import org.forgerock.monitoring.api.instrument.Timer;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which calculates time-elapsed since {@link TimerStopNode} was run and records this elapsed time in a metric.
 *
 * This node should be used with {@link TimerStartNode}.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = TimerStopNode.Config.class)
public class TimerStopNode extends SingleOutcomeNode {

    private final Debug logger;
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
     * @param logger
     *          Debug logging instance for authentication.
     * @param meterRegistry
     *          The metric registry.
     */
    @Inject
    public TimerStopNode(
            @Assisted TimerStopNode.Config config,
            @Named("amAuth") Debug logger,
            MeterRegistry meterRegistry) {
        Reject.ifTrue(isEmpty(config.startTimeProperty()), "TimerStopNode config must define start time property key");
        Reject.ifTrue(isEmpty(config.metricKey()), "TimerStopNode config must define metric key");
        this.config = config;
        this.logger = logger;
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
        logger.message("{} recording elapsed time {}ms (since {}) in {}",
                TimerStopNode.class.getSimpleName(), elapsedTime, config.startTimeProperty());

        timer.record(elapsedTime, TimeUnit.MILLISECONDS);

        return actionBuilder.build();
    }

}
