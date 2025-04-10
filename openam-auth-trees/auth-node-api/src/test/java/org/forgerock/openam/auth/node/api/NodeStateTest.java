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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NodeStateTest {

    @Nested
    @DisplayName(value = "NodeState")
    class Nodestate {

        @Nested
        @DisplayName(value = "#get")
        class Get {

            @Test
            @DisplayName(value = "state is not present it returns null")
            public void testStateIsNotPresentItReturnsNull() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(of("KEY_1", "VALUE_A")),
                        state(of("KEY_2", "VALUE_C")),
                        state(of("KEY_3", "VALUE_E")),
                        Set.of());

                Assertions.assertThat(nodeState.get("KEY_4")).isNull();
            }

            @Test
            @DisplayName(value = "state is in transient it retrieves state")
            public void testStateIsInTransientItRetrievesState() {
                NodeState nodeState = createTransientState(emptyList(), "KEY_1", "VALUE_1");
                assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "state is in secure it retrieves state")
            public void testStateIsInSecureItRetrievesState() {
                NodeState nodeState = createSecureState(emptyList(), "KEY_1", "VALUE_1");
                assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "state is in shared it retrieves state")
            public void testStateIsInSharedItRetrievesState() {
                NodeState nodeState = createSharedState(emptyList(), "KEY_1", "VALUE_1");
                assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
            }

            @Nested
            @DisplayName(value = "state is duplicated across state types")
            class StateIsDuplicatedAcrossStateTypes {

                @Test
                @DisplayName(value = "state is in both transient, secure and shared it returns value from transient")
                public void testStateIsInBothTransientSecureAndSharedItReturnsValueFromTransient() {
                    NodeState nodeState = new NodeState(emptyList(),
                            state(of("KEY_1", "VALUE_A")),
                            state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                            state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")),
                            Set.of());

                    assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_A");
                }

                @Test
                @DisplayName(value = "state is in both secure and shared it returns value from secure")
                public void testStateIsInBothSecureAndSharedItReturnsValueFromSecure() {
                    NodeState nodeState = new NodeState(emptyList(),
                            state(of("KEY_1", "VALUE_A")),
                            state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                            state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")),
                            Set.of());

                    assertThat(nodeState.get("KEY_2")).isString().isEqualTo("VALUE_D");
                }
            }
        }

        @Nested
        @DisplayName(value = "#getObject")
        class Getobject {

            @Nested
            @DisplayName(value = "state is duplicated across state types")
            class StateIsDuplicatedAcrossStateTypes {

                @Nested
                @DisplayName(value = "state is in both transient, secure and shared")
                class StateIsInBothTransientSecureAndShared {

                    @Nested
                    @DisplayName(value = "value is a map")
                    class ValueIsAMap {

                        @Test
                        @DisplayName(value = "combines states with distinct inner keys")
                        public void testCombinesStatesWithDistinctInnerKeys() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    state(of("KEY_1", of("INNER_KEY_1", "VALUE_A"))),
                                    state(of("KEY_1", of("INNER_KEY_2", "VALUE_B"), "KEY_2", "VALUE_D")),
                                    state(of("KEY_1", of("INNER_KEY_3", "VALUE_C"), "KEY_2", "VALUE_E")),
                                    Set.of());

                            assertThat(nodeState.getObject("KEY_1").asMap())
                                    .containsOnlyKeys("INNER_KEY_1", "INNER_KEY_2", "INNER_KEY_3");
                        }

                        @Test
                        @DisplayName(value = "combines states in order where keys clash (transient > secure > shared)")
                        public void testCombinesStatesInOrderWhereKeysClashTransientSecureShared() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    // transient
                                    state(of("KEY_1", of(
                                            "INNER_KEY_1", "VALUE_A"))),
                                    // secure
                                    state(of("KEY_1", of(
                                            "INNER_KEY_1", "VALUE_B",
                                            "INNER_KEY_2", "VALUE_D"))),
                                    // shared
                                    state(of("KEY_1", of(
                                            "INNER_KEY_1", "VALUE_C",
                                            "INNER_KEY_2", "VALUE_E",
                                            "INNER_KEY_3", "VALUE_F"))),
                                    Set.of());

                            assertThat(nodeState.getObject("KEY_1").asMap())
                                    .containsOnlyKeys("INNER_KEY_1", "INNER_KEY_2", "INNER_KEY_3")
                                    .containsEntry("INNER_KEY_1", "VALUE_A")
                                    .containsEntry("INNER_KEY_2", "VALUE_D")
                                    .containsEntry("INNER_KEY_3", "VALUE_F");
                        }

                        @Test
                        @DisplayName(value = "handles states with different types")
                        public void testHandlesStatesWithDifferentTypes() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    // transient
                                    state(of("KEY_1", of(
                                            "INNER_KEY_1", "VALUE_A"))),
                                    // secure
                                    state(of("KEY_1", List.of("VALUE_B", "VALUE_D"))),
                                    // shared
                                    state(of("KEY_1", of(
                                            "INNER_KEY_1", "VALUE_C",
                                            "INNER_KEY_2", "VALUE_E",
                                            "INNER_KEY_3", "VALUE_F"))),
                                    Set.of());

                            assertThat(nodeState.getObject("KEY_1").asMap())
                                    .containsOnlyKeys("INNER_KEY_1", "INNER_KEY_2", "INNER_KEY_3")
                                    .containsEntry("INNER_KEY_1", "VALUE_A")
                                    .containsEntry("INNER_KEY_2", "VALUE_E")
                                    .containsEntry("INNER_KEY_3", "VALUE_F");
                        }

                        @Test
                        @DisplayName(value = "prevents modification")
                        public void testPreventsModification() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    state(of("KEY_1", of("INNER_KEY_1", "VALUE_A"))),
                                    state(of("KEY_1", of("INNER_KEY_2", "VALUE_B"), "KEY_2", "VALUE_D")),
                                    state(of("KEY_1", of("INNER_KEY_3", "VALUE_C"), "KEY_2", "VALUE_E")),
                                    Set.of());

                            Map<String, Object> key1 = nodeState.getObject("KEY_1").asMap();
                            assertThatThrownBy(() -> key1.put("INNER_KEY_1", "VALUE_Z"))
                                    .isExactlyInstanceOf(UnsupportedOperationException.class);
                            assertThat(nodeState.getObject("KEY_1").asMap())
                                    .containsEntry("INNER_KEY_1", "VALUE_A");
                        }
                    }

                    @Test
                    @DisplayName(value = "value is not a map it returns value from transient")
                    public void testValueIsNotAMapItReturnsValueFromTransient() {
                        NodeState nodeState = new NodeState(emptyList(),
                                state(of("KEY_1", "VALUE_A")),
                                state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                                state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")),
                                Set.of());

                        assertThat(nodeState.getObject("KEY_1")).isString().isEqualTo("VALUE_A");
                    }
                }

                @Nested
                @DisplayName(value = "state is in both secure and shared")
                class StateIsInBothSecureAndShared {

                    @Nested
                    @DisplayName(value = "value is a map")
                    class ValueIsAMap {

                        @Test
                        @DisplayName(value = "combines states with distinct inner keys")
                        public void testCombinesStatesWithDistinctInnerKeys() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    // transient
                                    state(emptyMap()),
                                    // secure
                                    state(of("KEY_2", of(
                                            "INNER_KEY_1", "VALUE_A"))),
                                    // shared
                                    state(of("KEY_2", of(
                                            "INNER_KEY_1", "VALUE_B",
                                            "INNER_KEY_2", "VALUE_C"))),
                                    Set.of());

                            assertThat(nodeState.getObject("KEY_2").asMap())
                                    .containsOnlyKeys("INNER_KEY_1", "INNER_KEY_2")
                                    .containsEntry("INNER_KEY_1", "VALUE_A")
                                    .containsEntry("INNER_KEY_2", "VALUE_C");
                        }

                        @Test
                        @DisplayName(value = "handles states with null keys")
                        public void testHandlesStatesWithNullKeys() {
                            NodeState nodeState = new NodeState(emptyList(),
                                    // transient
                                    state(emptyMap()),
                                    // secure
                                    state(of("KEY_2", of(
                                            "INNER_KEY_1", "VALUE_A",
                                            "INNER_KEY_2", "VALUE_B"))),
                                    // shared
                                    state(of("KEY_2", json(null))),
                                    Set.of());

                            assertThat(nodeState.getObject("KEY_2").asMap())
                                    .containsOnlyKeys("INNER_KEY_1", "INNER_KEY_2")
                                    .containsEntry("INNER_KEY_1", "VALUE_A")
                                    .containsEntry("INNER_KEY_2", "VALUE_B");
                        }
                    }

                    @Test
                    @DisplayName(value = "value is not a map it returns value from secure")
                    public void testValueIsNotAMapItReturnsValueFromSecure() {
                        NodeState nodeState = new NodeState(emptyList(),
                                state(of("KEY_1", "VALUE_A")),
                                state(of("KEY_1", "VALUE_B", "KEY_2", "VALUE_D")),
                                state(of("KEY_1", "VALUE_C", "KEY_2", "VALUE_E")),
                                Set.of());

                        assertThat(nodeState.getObject("KEY_2")).isString().isEqualTo("VALUE_D");
                    }
                }
            }
        }

        @Nested
        @DisplayName(value = "#isDefined")
        class Isdefined {

            @Nested
            @DisplayName(value = "state filter is empty")
            class StateFilterIsEmpty {

                @Test
                @DisplayName(value = "state stored in transient state it retrieves the state")
                public void testStateStoredInTransientStateItRetrievesTheState() {
                    NodeState nodeState = createTransientState(emptyList(), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }

                @Test
                @DisplayName(value = "state stored in secure state it retrieves the state")
                public void testStateStoredInSecureStateItRetrievesTheState() {
                    NodeState nodeState = createSecureState(emptyList(), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }

                @Test
                @DisplayName(value = "state shared in shared state it retrieves the state")
                public void testStateSharedInSharedStateItRetrievesTheState() {
                    NodeState nodeState = createSharedState(emptyList(), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }
            }

            @Nested
            @DisplayName(value = "state filter is set to *")
            class StateFilterIsSetTo {

                @Test
                @DisplayName(value = "state stored in transient state it retrieves the state")
                public void testStateStoredInTransientStateItRetrievesTheState() {
                    NodeState nodeState = createTransientState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }

                @Test
                @DisplayName(value = "state stored in secure state it retrieves the state")
                public void testStateStoredInSecureStateItRetrievesTheState() {
                    NodeState nodeState = createSecureState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }

                @Test
                @DisplayName(value = "state shared in shared state it retrieves the state")
                public void testStateSharedInSharedStateItRetrievesTheState() {
                    NodeState nodeState = createSharedState(singletonList("*"), "SOME_KEY", "SOME_VALUE");
                    assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                }
            }

            @Nested
            @DisplayName(value = "state filter is set to specific values")
            class StateFilterIsSetToSpecificValues {

                @Nested
                @DisplayName(value = "requested state matches state filter")
                class RequestedStateMatchesStateFilter {

                    @Test
                    @DisplayName(value = "state stored in transient state it retrieves the state")
                    public void testStateStoredInTransientStateItRetrievesTheState() {
                        NodeState nodeState = createTransientState(singletonList("SOME_KEY"), "SOME_KEY",
                                "SOME_VALUE");
                        assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                    }

                    @Test
                    @DisplayName(value = "state stored in secure state it retrieves the state")
                    public void testStateStoredInSecureStateItRetrievesTheState() {
                        NodeState nodeState = createSecureState(singletonList("SOME_KEY"), "SOME_KEY",
                                "SOME_VALUE");
                        assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                    }

                    @Test
                    @DisplayName(value = "state shared in shared state it retrieves the state")
                    public void testStateSharedInSharedStateItRetrievesTheState() {
                        NodeState nodeState = createSharedState(singletonList("SOME_KEY"), "SOME_KEY",
                                "SOME_VALUE");
                        assertThat(nodeState.isDefined("SOME_KEY")).isTrue();
                    }
                }

                @Nested
                @DisplayName(value = "requested state does not matches state filter")
                class RequestedStateDoesNotMatchesStateFilter {

                    @Test
                    @DisplayName(value = "state stored in transient state it retrieves the state")
                    public void testStateStoredInTransientStateItRetrievesTheState() {
                        NodeState nodeState = createTransientState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                "SOME_OTHER_VALUE");
                        assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                    }

                    @Test
                    @DisplayName(value = "state stored in secure state it retrieves the state")
                    public void testStateStoredInSecureStateItRetrievesTheState() {
                        NodeState nodeState = createSecureState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                "SOME_OTHER_VALUE");
                        assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                    }

                    @Test
                    @DisplayName(value = "state shared in shared state it retrieves the state")
                    public void testStateSharedInSharedStateItRetrievesTheState() {
                        NodeState nodeState = createSharedState(singletonList("SOME_KEY"), "SOME_OTHER_KEY",
                                "SOME_OTHER_VALUE");
                        assertThat(nodeState.isDefined("SOME_OTHER_KEY")).isFalse();
                    }
                }
            }
        }

        @Nested
        @DisplayName(value = "#keys")
        class Keys {

            @Test
            @DisplayName(value = "returns distinct keys")
            public void testReturnsDistinctKeys() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                        state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                        state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")),
                        Set.of());

                assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2", "KEY_3", "KEY_4");
            }

            @Test
            @DisplayName(value = "returns filtered distinct keys")
            public void testReturnsFilteredDistinctKeys() {
                NodeState nodeState = new NodeState(List.of("KEY_1", "KEY_2"),
                        state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                        state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                        state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")),
                        Set.of());

                assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2");
            }

            @Test
            @DisplayName(value = "returns all distinct keys when filter wilcard")
            public void testReturnsAllDistinctKeysWhenFilterWilcard() {
                NodeState nodeState = new NodeState(List.of("*"),
                        state(of("KEY_1", "VALUE_A", "KEY_2", "VALUE_B")),
                        state(of("KEY_2", "VALUE_C", "KEY_3", "VALUE_D")),
                        state(of("KEY_3", "VALUE_E", "KEY_4", "VALUE_F")),
                        Set.of());

                assertThat(nodeState.keys()).containsExactlyInAnyOrder("KEY_1", "KEY_2", "KEY_3", "KEY_4");
            }
        }

        @Test
        @DisplayName(value = "#putShared it adds state")
        public void testPutsharedItAddsState() {
            NodeState nodeState = new NodeState(emptyList(),
                    state(new HashMap<>()),
                    state(new HashMap<>()),
                    state(new HashMap<>()),
                    Set.of());
            nodeState.putShared("KEY_1", "VALUE_1");

            assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
        }

        @Nested
        @DisplayName(value = "#mergeShared()")
        class Mergeshared {

            @Test
            @DisplayName(value = "adds state to existing container")
            public void testAddsStateToExistingContainer() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(object(field("container1", object()))),
                        Set.of("container1"));
                nodeState.mergeShared(Map.of("container1", Map.of("KEY_1", "VALUE_1")));

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "adds container and state if no container found")
            public void testAddsContainerAndStateIfNoContainerFound() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                nodeState.mergeShared(Map.of("container1", Map.of("KEY_1", "VALUE_1")));

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "adds container to shared state when shared state is a sub-view of a larger object")
            public void testAddsContainerToSharedStateWhenSharedStateIsASubViewOfALargerObject() {
                JsonValue largerObject = json(object(field("sharedState", object())));
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        largerObject.get("sharedState"),
                        Set.of("container1"));
                nodeState.mergeShared(Map.of("container1", Map.of("KEY_1", "VALUE_1")));

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "throws illegal argument exception if illegal container is requested")
            public void testThrowsIllegalArgumentExceptionIfIllegalContainerIsRequested() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container2", Map.of("KEY_1", "VALUE_1"));
                assertThatThrownBy(() -> nodeState.mergeShared(newState))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("State must not contain nested objects unless they are inside registered state "
                                + "containers: container1");
            }

            @Test
            @DisplayName(value = "allows container within valid state container")
            public void testAllowsContainerWithinValidStateContainer() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                nodeState.mergeShared(Map.of("container1", Map.of("KEY_1", Map.of("INNER_KEY_1", "VALUE_1"))));
                assertThat(nodeState.getObject("container1")).hasObject("KEY_1").stringAt("INNER_KEY_1")
                        .isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "adds non-container state when it isn't a Map")
            public void testAddsNonContainerStateWhenItIsnTAMap() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("KEY_1", "VALUE_1");
                nodeState.mergeShared(newState);

                assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "replaces existing state with new state")
            public void testReplacesExistingStateWithNewState() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(object(
                                field("container1", object(field("KEY_1", "VALUE_A"))),
                                field("KEY_2", "VALUE_A"))),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", Map.of("KEY_1", "VALUE_B"), "KEY_2", "VALUE_B");
                nodeState.mergeShared(newState);

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_B");
                assertThat(nodeState.get("KEY_2")).isString().isEqualTo("VALUE_B");
            }

            @Test
            @DisplayName(value = "fails to add container that isn't a map")
            public void testFailsToAddContainerThatIsnTAMap() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", "VALUE_1");
                assertThatThrownBy(() -> nodeState.mergeShared(newState))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("State containers must be a JSON object.");
            }

            @Test
            @DisplayName(value = "overwrites state in transient and secure state")
            public void testOverwritesStateInTransientAndSecureState() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(object(field("container1", object(field("KEY_1", "VALUE_A"),
                                field("KEY_2", "VALUE_A"))))),
                        state(object(field("container1", object(field("KEY_1", "VALUE_B"),
                                field("KEY_3", "VALUE_B"))))),
                        state(object(field("container1", object()))),
                        Set.of("container1"));
                nodeState.mergeShared(Map.of("container1", Map.of("KEY_1", "VALUE_1")));

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
                assertThat(nodeState.getObject("container1")).stringAt("KEY_2").isEqualTo("VALUE_A");
                assertThat(nodeState.getObject("container1")).stringAt("KEY_3").isEqualTo("VALUE_B");
            }
        }

        @Test
        @DisplayName(value = "#putTransient it adds state")
        public void testPuttransientItAddsState() {
            NodeState nodeState = new NodeState(emptyList(),
                    state(new HashMap<>()),
                    state(new HashMap<>()),
                    state(new HashMap<>()),
                    Set.of());
            nodeState.putTransient("KEY_1", "VALUE_1");

            assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
        }

        @Nested
        @DisplayName(value = "#mergeTransient()")
        class Mergetransient {

            @Test
            @DisplayName(value = "adds state to existing container")
            public void testAddsStateToExistingContainer() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(object(field("container1", object()))),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", Map.of("KEY_1", "VALUE_1"));
                nodeState.mergeTransient(newState);

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "adds container and state if no container found")
            public void testAddsContainerAndStateIfNoContainerFound() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", Map.of("KEY_1", "VALUE_1"));
                nodeState.mergeTransient(newState);

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "throws illegal argument exception if illegal container is requested")
            public void testThrowsIllegalArgumentExceptionIfIllegalContainerIsRequested() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container2", Map.of("KEY_1", "VALUE_1"));
                assertThatThrownBy(() -> nodeState.mergeTransient(newState))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("State must not contain nested objects unless they are inside registered state "
                                + "containers: container1");
            }

            @Test
            @DisplayName(value = "allows container within valid state container")
            public void testAllowsContainerWithinValidStateContainer() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                nodeState.mergeTransient(Map.of("container1", Map.of("KEY_1", Map.of("INNER_KEY_1", "VALUE_1"))));
                assertThat(nodeState.getObject("container1")).hasObject("KEY_1").stringAt("INNER_KEY_1")
                        .isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "adds non-container state when it isn't a Map")
            public void testAddsNonContainerStateWhenItIsnTAMap() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("KEY_1", "VALUE_1");
                nodeState.mergeTransient(newState);

                assertThat(nodeState.get("KEY_1")).isString().isEqualTo("VALUE_1");
            }

            @Test
            @DisplayName(value = "replaces existing state with new state")
            public void testReplacesExistingStateWithNewState() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(object(
                                field("container1", object(field("KEY_1", "VALUE_A"))),
                                field("KEY_2", "VALUE_A"))),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", Map.of("KEY_1", "VALUE_B"), "KEY_2", "VALUE_B");
                nodeState.mergeTransient(newState);

                assertThat(nodeState.getObject("container1")).stringAt("KEY_1").isEqualTo("VALUE_B");
                assertThat(nodeState.get("KEY_2")).isString().isEqualTo("VALUE_B");
            }

            @Test
            @DisplayName(value = "fails to add container that isn't a map")
            public void testFailsToAddContainerThatIsnTAMap() {
                NodeState nodeState = new NodeState(emptyList(),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        state(new HashMap<>()),
                        Set.of("container1"));
                Map<String, Object> newState = Map.of("container1", "VALUE_1");
                assertThatThrownBy(() -> nodeState.mergeTransient(newState))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("State containers must be a JSON object.");
            }
        }
    }

    private JsonValue state(Map<String, Object> state) {
        return json(state);
    }

    private NodeState createTransientState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object(field(key, value))),
                json(object()),
                json(object()),
                Set.of());
    }

    private NodeState createSecureState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object()),
                json(object(field(key, value))),
                json(object()),
                Set.of());
    }

    private NodeState createSharedState(List<String> stateFilter, String key, String value) {
        return new NodeState(
                stateFilter,
                json(object()),
                json(object()),
                json(object(field(key, value))),
                Set.of());
    }

}
