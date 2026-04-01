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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_FAILURE_ID;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_SUCCESS_ID;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.WARNING;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodesGraphValidatorTest {

    private NodesGraphValidator nodesGraphValidator;

    @BeforeEach
    void setUp() {
        nodesGraphValidator = new NodesGraphValidator();
    }

    @Test
    void shouldReturnWarningWhenNodeSetIsEmpty() {
        JsonValue nodes = json(object());
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId0"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodesGraphValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);
        ValidationFailure failure = result.get(0);
        assertThat(failure.message()).isEqualTo("The set of nodes is empty");
        assertThat(failure.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldReturnWarningWhenEntryNodeIdDoesntMatchAnyNode() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId0"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodesGraphValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);
        ValidationFailure failure = result.get(0);
        assertThat(failure.message()).isEqualTo("'entryNodeId' 'nodeId0' cannot be found in the set of nodes");
        assertThat(failure.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldReturnWarningWhenSomeNodesCreateDetachedSubTreeNotReachableFromStart() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId3")
                        ))
                )),
                field("nodeId2", object(
                        field("displayName", "displayName2"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                )),
                field("nodeId3", object(
                        field("displayName", "displayName3"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId2"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodesGraphValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(2);

        ValidationFailure failur1 = result.get(0);
        assertThat(failur1.message()).isEqualTo("Node 'nodeId3' is not reachable from the start");
        assertThat(failur1.severity()).isEqualTo(WARNING);

        ValidationFailure failur2 = result.get(1);
        assertThat(failur2.message()).isEqualTo("Node 'nodeId1' is not reachable from the start");
        assertThat(failur2.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldReturnWarningWhenNodeInputIsNotConnected() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId2")
                        ))
                )),
                field("nodeId2", object(
                        field("displayName", "displayName2"),
                        field("nodeType", "nodeType2"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId2"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodesGraphValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);

        ValidationFailure failur = result.get(0);
        assertThat(failur.message()).isEqualTo("Node 'nodeId1' is not reachable from the start");
        assertThat(failur.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldPassWhenSetOfNodesBuildsHealthyTreeWithLoops() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId2")
                        ))
                )),
                field("nodeId2", object(
                        field("displayName", "displayName2"),
                        field("nodeType", "nodeType2"),
                        field("connections", object(
                                field("outcome1", "nodeId4")
                        ))
                )),
                field("nodeId3", object(
                        field("displayName", "displayName3"),
                        field("nodeType", "nodeType3"),
                        field("connections", object(
                                field("outcome1", "nodeId1"),
                                field("outcome2", TREE_NODE_FAILURE_ID.toString())
                        ))
                )),
                field("nodeId4", object(
                        field("displayName", "displayName4"),
                        field("nodeType", "nodeType4"),
                        field("connections", object(
                                field("outcome1", "nodeId3"),
                                field("outcome2", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodesGraphValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(0);
    }
}