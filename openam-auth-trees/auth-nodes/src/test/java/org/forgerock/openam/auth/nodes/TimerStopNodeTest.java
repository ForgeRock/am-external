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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.forgerock.openam.monitoring.test.MonitoringAssertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.JsonValue;
import org.forgerock.monitoring.api.instrument.Clock;
import org.forgerock.monitoring.api.instrument.MeterRegistry;
import org.forgerock.monitoring.dropwizard.DropwizardMeterRegistry;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.junit.runner.RunWith;

import com.sun.identity.shared.debug.Debug;

@RunWith(CuppaRunner.class)
@org.forgerock.cuppa.Test
public class TimerStopNodeTest {

    private static final long MOCK_START_TIME = 1000L;
    private static final long MOCK_END_TIME = 1010L;
    private static final String CUSTOM_START_TIME_PROPERTY = "customKey";
    private static final String METRIC_KEY = "authentication.timer.metric";

    private TimerStopNode node;

    private Debug logger;
    private TimerStopNode.Config config;
    private Action result;
    private TreeContext context;
    private MeterRegistry meterRegistry;

    {
        describe("TimerStopNode", () -> {
            before(() -> {
                TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
                FrozenTimeService.INSTANCE.setCurrentTimeMillis(MOCK_END_TIME);
                meterRegistry = new DropwizardMeterRegistry(Clock.SYSTEM);
                logger = mock(Debug.class);
            });
            after(() -> {
                TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
            });
            when("using default config", () -> {
                before(() -> {
                    config = new TimerStopNodeConfig(METRIC_KEY);
                });
                recordsElapsedTimeAsMetric(DEFAULT_START_TIME_PROPERTY);
                reportsErrorIfStartTimeNotFoundInSharedState(DEFAULT_START_TIME_PROPERTY);
            });
            when("using custom config", () -> {
                before(() -> {
                    config = mock(TimerStopNode.Config.class);
                    given(config.startTimeProperty()).willReturn(CUSTOM_START_TIME_PROPERTY);
                    given(config.metricKey()).willReturn(METRIC_KEY);
                });
                recordsElapsedTimeAsMetric(CUSTOM_START_TIME_PROPERTY);
                reportsErrorIfStartTimeNotFoundInSharedState(CUSTOM_START_TIME_PROPERTY);
            });
        });
    }

    private void reportsErrorIfStartTimeNotFoundInSharedState(String expectedSharedStateKey) {
        when("start time not present in shared state", () -> {
            when("process method is called", () -> {
                before(() -> {
                    context = emptyTreeContext();
                    node = new TimerStopNode(config, logger, meterRegistry);
                    result = node.process(context);
                });
                it("records error to debug log and continues", () -> {
                    verify(logger).error("{} could not find start time in {}", "TimerStopNode", expectedSharedStateKey);

                });
            });
        });
    }

    private void recordsElapsedTimeAsMetric(String expectedSharedStateKey) {
        when("start time present in shared state", () -> {
            when("process method is called", () -> {
                before(() -> {
                    JsonValue sharedState = json(object(
                            field(expectedSharedStateKey, MOCK_START_TIME)
                    ));
                    context = newTreeContext(sharedState);
                    node = new TimerStopNode(config, logger, meterRegistry);
                    result = node.process(context);
                });
                it("records elapsed time in metric " + METRIC_KEY, () -> {
                    assertThat(meterRegistry)
                            .timer(METRIC_KEY)
                            .updatesFor(() -> node.process(context)).isEqualTo(1);
                });
                it("returns default outcome", () -> {
                    assertThat(result.outcome).isEqualTo("outcome");
                });
            });
        });
    }

    private class TimerStopNodeConfig implements TimerStopNode.Config {

        private final String metricKey;

        private TimerStopNodeConfig(String testMetricKey) {
            this.metricKey = testMetricKey;
        }

        @Override
        public String metricKey() {
            return metricKey;
        }
    }

}
