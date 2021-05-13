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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.wait;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.after;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.runner.RunWith;

@Test
@RunWith(CuppaRunner.class)
public class WaitingHelperTest {
    private static final int INITIAL_TIME_MILLIS = 100;
    private static final int SECONDS_TO_WAIT = 10;
    private static final int SPAM_MAX = 3;
    private WaitingHelper helper;
    private UUID uuid;

    private JsonValue currentState;

    {
        describe("Waiting helper", () -> {

            when("seconds to wait is a positive integer", () -> {
                beforeEach(() -> {
                    uuid = UUID.randomUUID();
                    helper = new WaitingHelper(uuid, SECONDS_TO_WAIT);
                    TimeTravelUtil.useFixedClock(INITIAL_TIME_MILLIS);
                });
                after(TimeTravelUtil::resetClock);
                when("creating callbacks", () -> {
                    it("has a polling wait callback", () -> {
                        PollingWaitCallback pollingWaitCallback = getCallback(helper.createCallbacks("badger"),
                                PollingWaitCallback.class);
                        assertThat(pollingWaitCallback.getWaitTime())
                                .isEqualTo(Integer.toString(SECONDS_TO_WAIT * 1000));
                        assertThat(pollingWaitCallback.getMessage()).contains("badger");
                    });
                });
                when("producing next state", () -> {
                    when("waiting is not in progress", () -> {
                        beforeEach(() -> {
                            currentState = nonEmptyState();
                        });
                        it("sets the end time", () -> {
                            assertThat(helper.getNextState(currentState).get(endTimeKey()).asInteger())
                                    .isEqualTo(INITIAL_TIME_MILLIS + SECONDS_TO_WAIT * 1000);
                        });
                        it("sets the spam count to 0", () -> {
                            assertThat(helper.getNextState(currentState).get(spamCountKey()).required().asInteger())
                                    .isEqualTo(0);
                        });
                    });
                    when("waiting is already in progress", () -> {
                        beforeEach(() -> {
                            currentState = helper.getNextState(nonEmptyState());
                        });
                        it("does not change the end time", () -> {
                            assertThat(helper.getNextState(currentState).get(endTimeKey()).asInteger())
                                    .isEqualTo(currentState.get(endTimeKey()).asInteger());
                        });
                        it("adds 1 to the spam count", () -> {
                            assertThat(helper.getNextState(currentState).get(spamCountKey()).asInteger())
                                    .isEqualTo(currentState.get(spamCountKey()).asInteger() + 1);
                        });
                    });
                });
                when("clearing state", () -> {
                    when("state contains keys for this helper", () -> {
                        beforeEach(() -> {
                            currentState = helper.getNextState(nonEmptyState());
                        });
                        it("returns a copy of the state with the fields removed", () -> {
                            JsonValue newState = helper.clearState(currentState);
                            assertThat(newState).isNotSameAs(currentState);
                            assertThat(newState.get(endTimeKey()).isNull()).isTrue();
                            assertThat(newState.get(spamCountKey()).isNull()).isTrue();
                        });
                    });
                    when("state does not contain keys for this helper", () -> {
                        beforeEach(() -> {
                            currentState = nonEmptyState();
                        });
                        it("returns an unchanged copy of the state", () -> {
                            JsonValue newState = helper.clearState(currentState);
                            assertThat(newState).isNotSameAs(currentState);
                            assertThat(newState.getObject()).isEqualTo(currentState.getObject());
                        });
                    });
                    when("state contains keys for a different WaitingHelper", () -> {
                        beforeEach(() -> {
                            currentState = new WaitingHelper(UUID.randomUUID(), 27).getNextState(nonEmptyState());
                        });
                        it("returns an unchanged copy of the state", () -> {
                            JsonValue newState = helper.clearState(currentState);
                            assertThat(newState).isNotSameAs(currentState);
                            assertThat(newState.getObject()).isEqualTo(currentState.getObject());
                        });
                    });
                });
                when("checking spam count", () -> {
                    when("count is less than limit", () -> {
                        beforeEach(() -> currentState = createSpamState(SPAM_MAX - 1));
                        it("returns false", () -> {
                            assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isFalse();
                        });
                    });
                    when("count is more than limit", () -> {
                        beforeEach(() -> currentState = createSpamState(SPAM_MAX + 1));
                        it("returns true", () -> {
                            assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isTrue();
                        });
                    });
                    when("count is equal to limit", () -> {
                        beforeEach(() -> currentState = createSpamState(SPAM_MAX));
                        it("returns false", () -> {
                            assertThat(helper.spamCountExceeds(currentState, SPAM_MAX)).isFalse();
                        });
                    });
                });
                when("checking wait time", () -> {
                    when("waiting time is complete", () -> {
                        beforeEach(() -> currentState = createEndTimeState(99));
                        it("returns true", () -> {
                            assertThat(helper.waitTimeCompleted(currentState)).isTrue();
                        });
                    });
                    when("waiting time is not complete", () -> {
                        beforeEach(() -> currentState = createEndTimeState(101));
                        it("returns false", () -> {
                            assertThat(helper.waitTimeCompleted(currentState)).isFalse();
                        });
                    });
                });
            });
        });
    }

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
}
