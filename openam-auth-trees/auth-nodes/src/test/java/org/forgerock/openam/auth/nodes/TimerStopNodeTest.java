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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import org.forgerock.json.JsonValue;
import org.forgerock.monitoring.api.instrument.Clock;
import org.forgerock.monitoring.dropwizard.DropwizardMeterRegistry;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.forgerock.openam.monitoring.test.MonitoringAssertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TimerStopNode}
 */
class TimerStopNodeTest {

    private static final long MOCK_START_TIME = 1000L;
    private static final long MOCK_END_TIME = 1010L;
    private static final String CUSTOM_START_TIME_PROPERTY = "customKey";
    private static final String METRIC_KEY = "authentication.timer.metric";

    private TimerStopNode node;
    private Logger logger;
    private TimerStopNode.Config config;
    private DropwizardMeterRegistry meterRegistry;
    private TreeContext context;

    @BeforeEach
    void setUp() {
        TimeTravelUtil.useFixedClock(MOCK_END_TIME);
        meterRegistry = new DropwizardMeterRegistry(Clock.SYSTEM);
        logger = mock(Logger.class);
    }

    @AfterEach
    void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test
    void shouldRecordElapsedTimeGivenDefaultConfigAndStartTimePresent() throws Exception {
        // Given
        setupDefaultConfig();
        setupContextWithStartTime(DEFAULT_START_TIME_PROPERTY);

        // When
        Action result = processNode();

        // Then
        verifyTimerUpdated();
        verifyDefaultOutcome(result);
    }

    @Test
    void shouldRecordElapsedTimeGivenCustomStartTimePropertyAndStartTimePresent() throws Exception {
        // Given
        setupCustomConfig();
        setupContextWithStartTime(CUSTOM_START_TIME_PROPERTY);

        // When
        Action result = processNode();

        // Then
        verifyTimerUpdated();
        verifyDefaultOutcome(result);
    }

    @Test
    void shouldLogErrorGivenDefaultConfigAndStartTimeMissing() {
        // Given
        setupDefaultConfig();
        setupEmptyContext();

        // When
        Action result = processNode();

        // Then
        verifyErrorLogged(DEFAULT_START_TIME_PROPERTY);
        verifyDefaultOutcome(result);
    }

    @Test
    void shouldLogErrorGivenCustomStartTimePropertyAndStartTimeMissing() {
        // Given
        setupCustomConfig();
        setupEmptyContext();

        // When
        Action result = processNode();

        // Then
        verifyErrorLogged(CUSTOM_START_TIME_PROPERTY);
        verifyDefaultOutcome(result);
    }

    private void setupDefaultConfig() {
        config = new TimerStopNodeConfig(METRIC_KEY);
    }

    private void setupCustomConfig() {
        config = mock(TimerStopNode.Config.class);
        given(config.startTimeProperty()).willReturn(CUSTOM_START_TIME_PROPERTY);
        given(config.metricKey()).willReturn(METRIC_KEY);
    }

    private void setupContextWithStartTime(String propertyKey) {
        JsonValue sharedState = json(object(field(propertyKey, MOCK_START_TIME)));
        context = newTreeContext(sharedState);
    }

    private void setupEmptyContext() {
        context = emptyTreeContext();
    }

    private Action processNode() {
        node = new TimerStopNode(config, meterRegistry, logger);
        return node.process(context);
    }

    private void verifyTimerUpdated() throws Exception {
        assertThat(meterRegistry)
                .timer(METRIC_KEY)
                .updatesFor(() -> node.process(context)).isEqualTo(1);
    }

    private void verifyErrorLogged(String propertyKey) {
        verify(logger).error("{} could not find start time in {}", "TimerStopNode", propertyKey);
    }

    private void verifyDefaultOutcome(Action result) {
        assertThat(result.outcome).isEqualTo("outcome");
    }

    // Simple config implementation for default config tests
    private static class TimerStopNodeConfig implements TimerStopNode.Config {
        private final String metricKey;

        TimerStopNodeConfig(String metricKey) {
            this.metricKey = metricKey;
        }

        @Override
        public String metricKey() {
            return metricKey;
        }

        @Override
        public String startTimeProperty() {
            return DEFAULT_START_TIME_PROPERTY;
        }
    }
}