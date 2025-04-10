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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"DataFlowIssue"})
@ExtendWith(MockitoExtension.class)
class SetStateNodeTest {
    private SetStateNode node;
    private TreeContext treeContext;

    @BeforeEach
    void setUp() {
        treeContext = new TreeContext(
                json(object()),
                new ExternalRequestContext.Builder().build(),
                Collections.emptyList(),
                Optional.empty());

    }

    @Test
    void addsAllAttributesToState() {
        // Given
        node = new SetStateNode(new TestConfig(Map.of("key1", "value1", "key2", "value2")));

        // When
        node.process(treeContext);

        // Then
        var state = treeContext.getStateFor(node);
        assertThat(state.get("key1").asString()).isEqualTo("value1");
        assertThat(state.get("key2").asString()).isEqualTo("value2");
    }

    @Test
    void overwritesExistingAttributesInSharedStateAndRemovesFromTransientAndSecureState() {
        // Given
        treeContext = new TreeContext(
                json(object(field("testKey", "oldValue"))),
                json(object(field("testKey", "oldValue"))),
                json(object(field("testKey", "oldValue"))),
                new ExternalRequestContext.Builder().build(),
                Collections.emptyList(),
                Optional.empty());
        node = new SetStateNode(new TestConfig(Map.of("testKey", "newValue")));

        // When
        node.process(treeContext);

        // Then
        var state = treeContext.getStateFor(node);
        assertThat(state.get("testKey").asString()).isEqualTo("newValue");
    }

    @Test
    void producesCorrectOutputs() {
        // Given
        node = new SetStateNode(new TestConfig(Map.of("key1", "someValue", "key2", "someValue")));

        // When
        var outputs = node.getOutputs();

        // Then
        assertThat(outputs).containsExactlyInAnyOrder(new OutputState("key1"), new OutputState("key2"));
    }

    @Test
    void returnsActionWhichGoesToTheOutcome() {
        // Given
        node = new SetStateNode(new TestConfig(emptyMap()));

        // When
        var process = node.process(treeContext);

        // Then
        assertThat(process.outcome).isEqualTo("outcome");
    }

    private record TestConfig(Map<String, String> attributes) implements SetStateNode.Config {

    }
}
