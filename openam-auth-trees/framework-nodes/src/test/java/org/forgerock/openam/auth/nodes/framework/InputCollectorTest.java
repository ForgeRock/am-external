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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputCollectorTest {

    private InputCollector collector;
    private final UUID nodeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        collector = new InputCollector();
    }

    @Test
    void shouldReturnEmptyListWhenNodeIsNull() throws NodeProcessException {
        // Given
        Supplier<Node> nodeSupplier = () -> null;

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                nodeSupplier, Stream.empty());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCollectInputsFromCurrentNode() throws NodeProcessException {
        // Given
        InputState input = new InputState("state", true);
        Node node = mock(Node.class);
        when(node.getInputs()).thenReturn(new InputState[]{input});

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                () -> node, Stream.empty());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name).isEqualTo("state");
    }

    @Test
    void shouldPromoteToRequiredWhenChildIsRequired() throws NodeProcessException {
        // Given
        InputState optionalState = new InputState("state", false);
        InputState requiredState = new InputState("state", true);

        Node node = mock(Node.class);
        when(node.getInputs()).thenReturn(new InputState[]{optionalState});

        Supplier<List<InputState>> childResult = () -> List.of(requiredState);

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                () -> node, Stream.of(childResult));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name).isEqualTo("state");
        assertThat(result.get(0).required).isTrue();
    }

    @Test
    void shouldKeepRequiredStatusEvenIfChildIsOptional() throws NodeProcessException {
        // Given
        InputState requiredState = new InputState("state", true);
        InputState optionalState = new InputState("state", false);

        Node node = mock(Node.class);
        when(node.getInputs()).thenReturn(new InputState[]{requiredState});

        Supplier<List<InputState>> childResult = () -> List.of(optionalState);

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                () -> node, Stream.of(childResult));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).required).isTrue();
    }

    @Test
    void shouldIgnoreNullInputs() throws NodeProcessException {
        // Given
        Node node = mock(Node.class);
        when(node.getInputs()).thenReturn(new InputState[]{null, new InputState("state", true)});

        Supplier<List<InputState>> childWithNulls = () -> {
            java.util.List<InputState> list = new java.util.ArrayList<>();
            list.add(null);
            return list;
        };

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                () -> node, Stream.of(childWithNulls));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name).isEqualTo("state");
    }

    @Test
    void shouldCollectFromMultipleBranches() throws NodeProcessException {
        // Given
        Node node = mock(Node.class);
        when(node.getInputs()).thenReturn(new InputState[]{});

        Supplier<List<InputState>> branch1 = () -> List.of(new InputState("state1", true));
        Supplier<List<InputState>> branch2 = () -> List.of(new InputState("state2", true));

        // When
        List<InputState> result = collector.visit(nodeId, Optional.empty(), Optional.empty(),
                () -> node, Stream.of(branch1, branch2));

        // Then
        assertThat(result).extracting(i -> i.name).containsExactlyInAnyOrder("state1", "state2");
    }
}