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
import org.junit.jupiter.api.Test;

public class NodeIdValidatorTest {

    NodeIdValidator nodeIdValidator = new NodeIdValidator();

    @Test
    void shouldReturnWarningWhenNodeIdIsIncorrect() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                )),
                field(TREE_NODE_FAILURE_ID.toString(), object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeIdValidator.validate(requestBody, null);

        assertThat(result).hasSize(1);
        ValidationFailure failure1 = result.get(0);
        assertThat(failure1.message()).isEqualTo(
                "Node id 'e301438c-0bd0-429c-ab0c-66126501069a' is reserved and cannot be used for a new node");
        assertThat(failure1.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldNotReturnWarningsWhenNodeIdIsFine() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                )),
                field("nodeId2", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", TREE_NODE_SUCCESS_ID.toString())
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeIdValidator.validate(requestBody, null);

        assertThat(result).hasSize(0);
    }

    @Test
    void shouldReturnWarningsWhenStaticNodeIdIncorrect() {
        JsonValue staticNodes = json(object(
                field("staticNodeId1", object(
                        field("x", 0),
                        field("y", 1)
                ))
        ));
        JsonValue requestBody = json(object(
                field("staticNodes", staticNodes)
        ));

        List<ValidationFailure> result = nodeIdValidator.validate(requestBody, null);

        assertThat(result).hasSize(1);

        ValidationFailure failure1 = result.get(0);
        assertThat(failure1.message()).isEqualTo(
                "Static node id 'staticNodeId1' is invalid and cannot be used for a static node");
        assertThat(failure1.severity()).isEqualTo(WARNING);
    }

    @Test
    void shouldNotReturnWarningsWhenStaticNodeIdIsFine() {
        JsonValue staticNodes = json(object(
                field(TREE_NODE_SUCCESS_ID.toString(), object(
                        field("x", 0),
                        field("y", 1)
                )),
                field(TREE_NODE_FAILURE_ID.toString(), object(
                        field("x", 2),
                        field("y", 3)
                )),
                field("startNode", object(
                        field("x", 4),
                        field("y", 5)
                ))
        ));
        JsonValue requestBody = json(object(
                field("staticNodes", staticNodes)
        ));

        List<ValidationFailure> result = nodeIdValidator.validate(requestBody, null);

        assertThat(result).hasSize(0);
    }
}
