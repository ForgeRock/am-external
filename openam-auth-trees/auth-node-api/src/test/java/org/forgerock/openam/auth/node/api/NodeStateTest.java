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
 * Copyright 2021-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.test.assertj.Conditions.equalTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.JsonValue;
import org.junit.runner.RunWith;

@Test
@RunWith(CuppaRunner.class)
public class NodeStateTest {

    {
        describe("NodeState", () -> {
            describe("#get", () -> {
                when("state is not present", () -> {
                    it("returns null", () -> {
                        NodeState nodeState = new NodeState(emptyList(),
                                state(of("KEY_1", "VALUE_A")),
                                state(of("KEY_2", "VALUE_C")),
                                state(of("KEY_3", "VALUE_E")));

                        Assertions.assertThat(nodeState.get("KEY_4")).isNull();
                    });
                });
                when("state is in transient", () -> {
                    it("retrieves state", () -> {
                        NodeState nodeState = createTransientState(emptyList(), "KEY_1", "VALUE_1");
                        assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_1"));
                    });
                });
                when("state is in secure", () -> {
                    it("retrieves state", () -> {
                        NodeState nodeState = createSecureState(emptyList(), "KEY_1", "VALUE_1");
                        assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_1"));
                    });
                });
                when("state is in shared", () -> {
                    it("retrieves state", () -> {
                        NodeState nodeState = createSharedState(emptyList(), "KEY_1", "VALUE_1");
                        assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_1"));
                    });
                });
                when("state is duplicated across state types", () -> {
                    when("state is in both transient, secure and shared", () -> {
                        it("returns value from transient", () -> {
                            NodeState nodeState = new NodeState(emptyList(),
                                    state(of("KEY_1", "VALUE_A")),
                                    state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                                    state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")));

                            assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_A"));
                        });
                    });
                    when("state is in both secure and shared", () -> {
                        it("returns value from secure", () -> {
                            NodeState nodeState = new NodeState(emptyList(),
                                    state(of("KEY_1", "VALUE_A")),
                                    state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                                    state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")));

                            assertThat(nodeState.get("KEY_2")).stringIs("", equalTo("VALUE_D"));
                        });
                    });
                });
            });
            describe("#isDefined", () -> {
                when("state filter is empty", () -> {
                    when("state stored in transient state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createTransientState(emptyList(), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                    when("state stored in secure state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createSecureState(emptyList(), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                    when("state shared in shared state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createSharedState(emptyList(), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                });
                when("state filter is set to *", () -> {
                    when("state stored in transient state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createTransientState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                    when("state stored in secure state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createSecureState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                    when("state shared in shared state", () -> {
                        it("retrieves the state", () -> {
                            NodeState nodeState = createSharedState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                            assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                        });
                    });
                });
                when("state filter is set to specific values", () -> {
                    when("requested state matches state filter", () -> {
                        when("state stored in transient state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createTransientState(singletonList("SOME_KEY"), "SOME_KEY",
                                        "SOME_VALUE");
                                assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                            });
                        });
                        when("state stored in secure state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createSecureState(singletonList("SOME_KEY"), "SOME_KEY",
                                        "SOME_VALUE");
                                assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                            });
                        });
                        when("state shared in shared state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createSharedState(singletonList("SOME_KEY"), "SOME_KEY",
                                        "SOME_VALUE");
                                assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                            });
                        });
                    });
                    when("requested state does not matches state filter", () -> {
                        when("state stored in transient state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createTransientState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                        "SOME_OTHER_VALUE");
                                assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                            });
                        });
                        when("state stored in secure state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createSecureState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                        "SOME_OTHER_VALUE");
                                assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                            });
                        });
                        when("state shared in shared state", () -> {
                            it("retrieves the state", () -> {
                                NodeState nodeState = createSharedState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                        "SOME_OTHER_VALUE");
                                assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                            });
                        });
                    });
                });
            });
            describe("#keys", () -> {
                it("returns distinct keys", () -> {
                    NodeState nodeState = new NodeState(emptyList(),
                            state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                            state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                            state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")));

                    assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2", "KEY_3", "KEY_4");
                });
                it("returns filtered distinct keys", () -> {
                    NodeState nodeState = new NodeState(List.of("KEY_1", "KEY_2"),
                            state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                            state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                            state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")));

                    assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2");
                });
                it("returns all distinct keys when filter wilcard", () -> {
                    NodeState nodeState = new NodeState(List.of("*"),
                            state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                            state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                            state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")));

                    assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2", "KEY_3", "KEY_4");
                });
            });
            describe("#putShared", () -> {
                it("adds state", () -> {
                    NodeState nodeState = new NodeState(emptyList(),
                            state(new HashMap<>()),
                            state(new HashMap<>()),
                            state(new HashMap<>()));
                    nodeState.putShared("KEY_1", "VALUE_1");

                    assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_1"));
                });
            });
            describe("#putTransient", () -> {
                it("adds state", () -> {
                    NodeState nodeState = new NodeState(emptyList(),
                            state(new HashMap<>()),
                            state(new HashMap<>()),
                            state(new HashMap<>()));
                    nodeState.putTransient("KEY_1", "VALUE_1");

                    assertThat(nodeState.get("KEY_1")).stringIs("", equalTo("VALUE_1"));
                });
            });
        });
    }

    private JsonValue state(Map<String, Object> state) {
        return json(state);
    }

    private NodeState createTransientState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object(field(key, value))),
                json(object()),
                json(object()));
    }

    private NodeState createSecureState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object()),
                json(object(field(key, value))),
                json(object()));
    }

    private NodeState createSharedState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object()),
                json(object()),
                json(object(field(key, value))));
    }
}
