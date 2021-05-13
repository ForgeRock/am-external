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