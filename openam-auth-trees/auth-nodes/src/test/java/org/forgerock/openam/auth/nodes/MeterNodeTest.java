/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.forgerock.openam.monitoring.test.MonitoringAssertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.forgerock.monitoring.api.instrument.Clock;
import org.forgerock.monitoring.api.instrument.MeterRegistry;
import org.forgerock.monitoring.dropwizard.DropwizardMeterRegistry;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class MeterNodeTest {

    private static final String METRIC_KEY = "authentication.meter.metric";

    private MeterNode meterNode;

    private MeterNode.Config config;
    private MeterRegistry meterRegistry;
    private TreeContext context;

    @BeforeTest
    protected void setUp() throws Exception {
        config = mock(MeterNode.Config.class);
        meterRegistry = new DropwizardMeterRegistry(Clock.SYSTEM);
        context = emptyTreeContext();
    }

    @Test
    public void callsToProcessShouldIncrementConfiguredMetric() throws Exception {
        given(config.metricKey()).willReturn(METRIC_KEY);
        meterNode = new MeterNode(config, meterRegistry);

        assertThat(meterRegistry)
                .summary(METRIC_KEY)
                .changeCausedBy(() -> meterNode.process(context)).isEqualTo(1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void throwsExceptionIfConfigurationDoesNotDefineMetricKey() throws Exception {
        given(config.metricKey()).willReturn(null);
        meterNode = new MeterNode(config, meterRegistry);
    }

}