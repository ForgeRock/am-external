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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NodeStateScriptWrapperTest {

    private NodeStateScriptWrapper nodeStateScriptWrapper;
    private JsonValue transientState;
    private JsonValue sharedState;

    @BeforeEach
    void setup() {
        transientState = json(object());
        sharedState = json(object());
        NodeState nodeState = new NodeState(List.of(),
                transientState,
                json(object()),
                sharedState,
                Set.of("objectAttributes"));
        nodeStateScriptWrapper = new NodeStateScriptWrapper(nodeState);
    }

    @Test
    void shouldAddValueToEmptySharedStateWhenMergeSharedCalled() {
        // when
        nodeStateScriptWrapper.mergeShared(Map.of("key", "value"));

        // then
        assertThat(sharedState.asMap()).hasSize(1);
        assertThat(sharedState.asMap()).containsEntry("key", "value");
    }

    @Test
    void shouldAddValueToObjectAttributesInEmptySharedStateWhenMergeSharedCalledOnAllowedContainer() {
        // when
        nodeStateScriptWrapper.mergeShared(Map.of("objectAttributes", Map.of("key", "value")));

        // then
        assertThat(sharedState.asMap()).hasSize(1);
        assertThat(sharedState.asMap()).containsKey("objectAttributes");
        assertThat(sharedState.get("objectAttributes").asMap()).hasSize(1);
        assertThat(sharedState.get("objectAttributes").asMap()).containsEntry("key", "value");
    }

    @Test
    void shouldThrowErrorWhenTryingToAddValueToUnknownContainerInEmptySharedStateWhenMergeSharedCalled() {
        // when
        Map<String, Object> newState = Map.of("unknownContainer", Map.of("key", "value"));
        // then
        assertThatThrownBy(() -> nodeStateScriptWrapper.mergeShared(newState))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("State must not contain nested objects unless they are inside registered state containers: "
                                    + "objectAttributes");
    }

    @Test
    void shouldAddValueToExistingObjectAttributesInSharedStateWhenMergeSharedCalledOnAllowedContainer() {
        // given
        sharedState.put("objectAttributes", json(object(field("anotherKey", "anotherValue"))));

        // when
        nodeStateScriptWrapper.mergeShared(Map.of("objectAttributes", Map.of("key", "value")));

        // then
        assertThat(sharedState.asMap()).hasSize(1);
        assertThat(sharedState.asMap()).containsKey("objectAttributes");
        assertThat(sharedState.get("objectAttributes").asMap()).hasSize(2);
        assertThat(sharedState.get("objectAttributes").asMap()).containsEntry("key", "value");
        assertThat(sharedState.get("objectAttributes").asMap()).containsEntry("anotherKey", "anotherValue");
    }

    @Test
    void shouldAddValueToEmptyTransientStateWhenMergeTransientCalled() {
        // when
        nodeStateScriptWrapper.mergeTransient(Map.of("key", "value"));

        // then
        assertThat(transientState.asMap()).hasSize(1);
        assertThat(transientState.asMap()).containsEntry("key", "value");
    }

    @Test
    void shouldAddValueToObjectAttributesInEmptyTransientStateWhenMergeTransientCalledOnAllowedContainer() {
        // when
        nodeStateScriptWrapper.mergeTransient(Map.of("objectAttributes", Map.of("key", "value")));

        // then
        assertThat(transientState.asMap()).hasSize(1);
        assertThat(transientState.asMap()).containsKey("objectAttributes");
        assertThat(transientState.get("objectAttributes").asMap()).hasSize(1);
        assertThat(transientState.get("objectAttributes").asMap()).containsEntry("key", "value");
    }

    @Test
    void shouldThrowErrorWhenTryingToAddValueToUnknownContainerInEmptyTransientStateWhenMergeTransientCalled() {
        // when
        Map<String, Object> newState = Map.of("unknownContainer", Map.of("key", "value"));
        // then
        assertThatThrownBy(() -> nodeStateScriptWrapper.mergeTransient(newState))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("State must not contain nested objects unless they are inside registered state containers: "
                                    + "objectAttributes");
    }

    @Test
    void shouldAddValueToExistingObjectAttributesInTransientStateWhenMergeTransientCalledOnAllowedContainer() {
        // given
        transientState.put("objectAttributes", json(object(field("anotherKey", "anotherValue"))));

        // when
        nodeStateScriptWrapper.mergeTransient(Map.of("objectAttributes", Map.of("key", "value")));

        // then
        assertThat(transientState.asMap()).hasSize(1);
        assertThat(transientState.asMap()).containsKey("objectAttributes");
        assertThat(transientState.get("objectAttributes").asMap()).hasSize(2);
        assertThat(transientState.get("objectAttributes").asMap()).containsEntry("key", "value");
        assertThat(transientState.get("objectAttributes").asMap()).containsEntry("anotherKey", "anotherValue");
    }

}
