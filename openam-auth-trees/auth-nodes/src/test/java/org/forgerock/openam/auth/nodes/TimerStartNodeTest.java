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

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TimerStartNode}.
 */
class TimerStartNodeTest {

    private static final long MOCK_START_TIME = 1000L;
    private static final String CUSTOM_START_TIME_PROPERTY = "customKey";

    private TimerStartNode node;
    private Logger logger;
    private TimerStartNode.Config config;
    private TreeContext context;

    @BeforeEach
    void setUp() {
        context = emptyTreeContext();
        logger = mock(Logger.class);
        TimeTravelUtil.useFixedClock(MOCK_START_TIME);
    }

    @AfterEach
    void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test
    void shouldProcessGivenDefaultConfig() {
        // Given
        config = new TimerStartNode.Config() { };
        node = new TimerStartNode(config, logger);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.sharedState.get(DEFAULT_START_TIME_PROPERTY).asLong())
                .isEqualTo(MOCK_START_TIME);
        assertThat(result.outcome).isEqualTo("outcome");
    }

    @Test
    void shouldProcessGivenCustomStartTimeProperty() {
        // Given
        config = mock(TimerStartNode.Config.class);
        given(config.startTimeProperty()).willReturn(CUSTOM_START_TIME_PROPERTY);
        node = new TimerStartNode(config, logger);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.sharedState.get(CUSTOM_START_TIME_PROPERTY).asLong())
                .isEqualTo(MOCK_START_TIME);
        assertThat(result.outcome).isEqualTo("outcome");
    }
}