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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NodeTypeValidatorTest {

    @Mock
    private NodeRegistry nodeRegistry;

    private NodeTypeValidator nodeTypeValidator;

    @BeforeEach
    void setUp() {
        given(nodeRegistry.isNodeService("nodeType1")).willReturn(true);
        given(nodeRegistry.getVersions("nodeType1")).willReturn(List.of(1));
        nodeTypeValidator = new NodeTypeValidator(nodeRegistry);
    }

    @Test
    void shouldPassValidationWhenTypeAndVersionAreCorrect() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("version", 1),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeTypeValidator.validate(requestBody, null);

        assertThat(result).hasSize(0);
    }

    @Test
    void shouldReturnErrorWhenNodeTypeDoesNotExist() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType2"),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeTypeValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);
        ValidationFailure failure = result.get(0);
        assertThat(failure.message()).isEqualTo("'nodeType' 'nodeType2' does not exist");
        assertThat(failure.severity()).isEqualTo(ERROR);
    }

    @Test
    void shouldReturnErrorWhenNodeVersionDoesNotExist() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "displayName1"),
                        field("nodeType", "nodeType1"),
                        field("version", 2),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeTypeValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);
        ValidationFailure failure = result.get(0);
        assertThat(failure.message()).isEqualTo("'nodeType' 'nodeType1' does not have version '2'");
        assertThat(failure.severity()).isEqualTo(ERROR);
    }
}
