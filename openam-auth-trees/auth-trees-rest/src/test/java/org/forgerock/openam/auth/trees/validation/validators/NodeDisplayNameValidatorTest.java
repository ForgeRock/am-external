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
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.WARNING;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeDisplayNameValidatorTest {

    private NodeDisplayNameValidator nodeDisplayNameValidator;

    @BeforeEach
    void setUp() {
        nodeDisplayNameValidator = new NodeDisplayNameValidator();
    }

    @Test
    void shouldPassWhenSetOfNodesBuildsHealthyTree() {
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
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeDisplayNameValidator.validate(requestBody, null);

        assertThat(result).hasSize(0);
    }

    @Test
    void shouldReturnWarningWhenDisplayNameContainsInvalidCharacters() {
        JsonValue nodes = json(object(
                field("nodeId1", object(
                        field("displayName", "\ndisplay\rName1\t"),
                        field("nodeType", "nodeType1"),
                        field("connections", object(
                                field("outcome1", "nodeId1")
                        ))
                ))
        ));
        JsonValue requestBody = json(object(
                field("entryNodeId", "nodeId1"),
                field("nodes", nodes)
        ));

        List<ValidationFailure> result = nodeDisplayNameValidator.validate(requestBody, null);

        assertThat(result.size()).isEqualTo(1);
        ValidationFailure failure = result.get(0);
        assertThat(failure.message()).isEqualTo("'displayName' for node 'nodeId1' contains invalid characters");
        assertThat(failure.severity()).isEqualTo(WARNING);
    }
}
