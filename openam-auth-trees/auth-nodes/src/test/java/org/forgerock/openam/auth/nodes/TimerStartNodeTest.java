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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.after;
import static org.forgerock.cuppa.Cuppa.before;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.forgerock.cuppa.Test;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.slf4j.Logger;

@Test
public class TimerStartNodeTest {

    private static final long MOCK_START_TIME = 1000L;
    private static final String CUSTOM_START_TIME_PROPERTY = "customKey";

    private TimerStartNode node;

    private Logger logger;
    private TimerStartNode.Config config;
    private Action result;
    private TreeContext context;

    {
        describe("TimerStartNode", () -> {
            before(() -> {
                context = emptyTreeContext();
                logger = mock(Logger.class);
                TimeTravelUtil.useFixedClock(MOCK_START_TIME);
            });
            after(TimeTravelUtil::resetClock);
            when("using default config", () -> {
                before(() -> {
                    config = new TimerStartNode.Config() { };
                });
                startTimeNodeRecordsCurrentSystemTimeInSharedState(DEFAULT_START_TIME_PROPERTY);
            });
            when("using custom config", () -> {
                before(() -> {
                    config = mock(TimerStartNode.Config.class);
                    given(config.startTimeProperty()).willReturn(CUSTOM_START_TIME_PROPERTY);
                });
                startTimeNodeRecordsCurrentSystemTimeInSharedState(CUSTOM_START_TIME_PROPERTY);
            });
        });
    }

    private void startTimeNodeRecordsCurrentSystemTimeInSharedState(String expectedSharedStateKey) {
        before(() -> {
            node = new TimerStartNode(config, logger);
        });
        when("process method is called", () -> {
            before(() -> {
                result = node.process(context);
            });
            it("writes current system time to " + expectedSharedStateKey + " shared state key", () -> {
                assertThat(result.sharedState)
                        .hasPath(expectedSharedStateKey)
                        .isLong()
                        .isEqualTo(MOCK_START_TIME);
            });
            it("returns default outcome", () -> {
                assertThat(result.outcome).isEqualTo("outcome");
            });
        });
    }

}
