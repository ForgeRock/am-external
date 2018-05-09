/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.nodes.TimerStartNode.DEFAULT_START_TIME_PROPERTY;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.junit.runner.RunWith;

import com.sun.identity.shared.debug.Debug;

@RunWith(CuppaRunner.class)
@org.forgerock.cuppa.Test
public class TimerStartNodeTest {

    private static final long MOCK_START_TIME = 1000L;
    private static final String CUSTOM_START_TIME_PROPERTY = "customKey";

    private TimerStartNode node;

    private Debug logger;
    private TimerStartNode.Config config;
    private Action result;
    private TreeContext context;

    {
        describe("TimerStartNode", () -> {
            before(() -> {
                context = emptyTreeContext();
                logger = mock(Debug.class);
                TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
                FrozenTimeService.INSTANCE.setCurrentTimeMillis(MOCK_START_TIME);
            });
            after(() -> {
                TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
            });
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