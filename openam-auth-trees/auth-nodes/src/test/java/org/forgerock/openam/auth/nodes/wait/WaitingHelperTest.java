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

package org.forgerock.openam.auth.nodes.wait;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class WaitingHelperTest {
    private static final int INITIAL_TIME_MILLIS = 100;
    private static final int SECONDS_TO_WAIT = 10;
    private static final int SPAM_MAX = 3;
    private WaitingHelper helper;
    private UUID uuid;

    private JsonValue currentState;

    private JsonValue createSpamState(int count) {
        return json(object(field(spamCountKey(), count)));
    }

    private JsonValue createEndTimeState(int timeInMillis) {
        return json(object(field(endTimeKey(), timeInMillis)));
    }

    private String endTimeKey() {
        return uuid.toString() + ".end_time";
    }

    private String spamCountKey() {
        return uuid.toString() + ".spam_count";
    }

    private JsonValue nonEmptyState() {
        return json(object(field("a", "b")));
    }

    private <T extends Callback> T getCallback(List<Callback> callbacks, Class<T> clazz) {
        return callbacks.stream()
                .filter(clazz::isInstance)
                .map(c -> (T) c)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No callback of type " + clazz.getSimpleName() + "found"));
    }

    @Nested
    @DisplayName(value = "seconds to wait is a positive integer")
    class SecondsToWaitIsAPositiveInteger {

        @BeforeEach
        public void beforeEach() {
            uuid = UUID.randomUUID();
            helper = new WaitingHelper(uuid, SECONDS_TO_WAIT);
            TimeTravelUtil.useFixedClock(INITIAL_TIME_MILLIS);
        }

        @AfterAll
        public static void after() {
            TimeTravelUtil.resetClock();
        }

        @Test
        @DisplayName(value = "creating callbacks it has a polling wait callback")
        public void testCreatingCallbacksItHasAPollingWaitCallback() throws Exception {
            PollingWaitCallback pollingWaitCallback = getCallback(helper.createCallbacks("badger"),
                    PollingWaitCallback.class);
            assertThat(pollingWaitCallback.getWaitTime())
                    .isEqualTo(Integer.toString(SECONDS_TO_WAIT * 1000));
            assertThat(pollingWaitCallback.getMessage()).contains("badger");
        }

        @Nested
        @DisplayName(value = "producing next state")
        class ProducingNextState {

            @Nested
            @DisplayName(value = "waiting is not in progress")
            class WaitingIsNotInProgress {

                @BeforeEach
                public void beforeEach() {
                    currentState = nonEmptyState();
                }

                @Test
                @DisplayName(value = "sets the end time")
                public void testSetsTheEndTime() throws Exception {
                    assertThat(helper.getNextState(currentState).get(endTimeKey()).asInteger())
                            .isEqualTo(INITIAL_TIME_MILLIS + SECONDS_TO_WAIT * 1000);
                }

                @Test
                @DisplayName(value = "sets the spam count to 0")
                public void testSetsTheSpamCountTo0() throws Exception {
                    assertThat(helper.getNextState(currentState).get(spamCountKey()).required().asInteger())
                            .isEqualTo(0);
                }
            }

            @Nested
            @DisplayName(value = "waiting is already in progress")
            class WaitingIsAlreadyInProgress {

                @BeforeEach
                public void beforeEach() {
                    currentState = helper.getNextState(nonEmptyState());
                }

                @Test
                @DisplayName(value = "does not change the end time")
                public void testDoesNotChangeTheEndTime() throws Exception {
                    assertThat(helper.getNextState(currentState).get(endTimeKey()).asInteger())
                            .isEqualTo(currentState.get(endTimeKey()).asInteger());
                }

                @Test
                @DisplayName(value = "adds 1 to the spam count")
                public void testAdds1ToTheSpamCount() throws Exception {
                    assertThat(helper.getNextState(currentState).get(spamCountKey()).asInteger())
                            .isEqualTo(currentState.get(spamCountKey()).asInteger() + 1);
                }
            }
        }

        @Nested
        @DisplayName(value = "clearing state")
        class ClearingState {

            @Nested
            @DisplayName(value = "state contains keys for this helper")
            class StateContainsKeysForThisHelper {

                @BeforeEach
                public void beforeEach() {
                    currentState = helper.getNextState(nonEmptyState());
                }

                @Test
                @DisplayName(value = "returns a copy of the state with the fields removed")
                public void testReturnsACopyOfTheStateWithTheFieldsRemoved() throws Exception {
                    JsonValue newState = helper.clearState(currentState);
                    assertThat(newState).isNotSameAs(currentState);
                    assertThat(newState.get(endTimeKey()).isNull()).isTrue();
                    assertThat(newState.get(spamCountKey()).isNull()).isTrue();
                }
            }

            @Nested
            @DisplayName(value = "state does not contain keys for this helper")
            class StateDoesNotContainKeysForThisHelper {

                @BeforeEach
                public void beforeEach() {
                    currentState = nonEmptyState();
                }

                @Test
                @DisplayName(value = "returns an unchanged copy of the state")
                public void testReturnsAnUnchangedCopyOfTheState() throws Exception {
                    JsonValue newState = helper.clearState(currentState);
                    assertThat(newState).isNotSameAs(currentState);
                    assertThat(newState.getObject()).isEqualTo(currentState.getObject());
                }
            }

            @Nested
            @DisplayName(value = "state contains keys for a different WaitingHelper")
            class StateContainsKeysForADifferentWaitinghelper {

                @BeforeEach
                public void beforeEach() {
                    currentState = new WaitingHelper(UUID.randomUUID(), 27).getNextState(nonEmptyState());
                }

                @Test
                @DisplayName(value = "returns an unchanged copy of the state")
                public void testReturnsAnUnchangedCopyOfTheState() throws Exception {
                    JsonValue newState = helper.clearState(currentState);
                    assertThat(newState).isNotSameAs(currentState);
                    assertThat(newState.getObject()).isEqualTo(currentState.getObject());
                }
            }
        }

        @Nested
        @DisplayName(value = "checking spam count")
        class CheckingSpamCount {

            @Nested
            @DisplayName(value = "count is less than limit")
            class CountIsLessThanLimit {

                @BeforeEach
                public void beforeEach() {
                    currentState = createSpamState(SPAM_MAX - 1);
                }

                @Test
                @DisplayName(value = "returns false")
                public void testReturnsFalse() throws Exception {
                    assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isFalse();
                }
            }

            @Nested
            @DisplayName(value = "count is more than limit")
            class CountIsMoreThanLimit {

                @BeforeEach
                public void beforeEach() {
                    currentState = createSpamState(SPAM_MAX + 1);
                }

                @Test
                @DisplayName(value = "returns true")
                public void testReturnsTrue() throws Exception {
                    assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isTrue();
                }
            }

            @Nested
            @DisplayName(value = "count is equal to limit")
            class CountIsEqualToLimit {

                @BeforeEach
                public void beforeEach() {
                    currentState = createSpamState(SPAM_MAX);
                }

                @Test
                @DisplayName(value = "returns false")
                public void testReturnsFalse() throws Exception {
                    assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isFalse();
                }
            }
        }

        @Nested
        @DisplayName(value = "checking wait time")
        class CheckingWaitTime {

            @Nested
            @DisplayName(value = "waiting time is complete")
            class WaitingTimeIsComplete {

                @BeforeEach
                public void beforeEach() {
                    currentState = createEndTimeState(99);
                }

                @Test
                @DisplayName(value = "returns true")
                public void testReturnsTrue() throws Exception {
                    assertThat(helper.waitTimeCompleted(currentState)).isTrue();
                }
            }

            @Nested
            @DisplayName(value = "waiting time is not complete")
            class WaitingTimeIsNotComplete {

                @BeforeEach
                public void beforeEach() {
                    currentState = createEndTimeState(101);
                }

                @Test
                @DisplayName(value = "returns false")
                public void testReturnsFalse() throws Exception {
                    assertThat(helper.waitTimeCompleted(currentState)).isFalse();
                }
            }
        }
    }
}
