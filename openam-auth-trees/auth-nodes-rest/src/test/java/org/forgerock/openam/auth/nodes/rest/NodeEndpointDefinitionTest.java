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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodeEndpointDefinitionTest {

    @Mock
    private NodeRegistry nodeRegistry;

    private NodeEndpointDefinition nodeEndpointDefinition;

    @BeforeEach
    void setUp() {
        nodeEndpointDefinition = new NodeEndpointDefinition(nodeRegistry);
    }

    @Test
    void shouldReturnTrueWhenHandlesWithNodeType() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(true);

        // When
        boolean handles = nodeEndpointDefinition.handles(serviceName);

        // Then
        assertThat(handles).isTrue();
    }

    @Test
    void shouldReturnFalseWhenHandlesWithNonNodeType() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(false);

        // When
        boolean handles = nodeEndpointDefinition.handles(serviceName);

        // Then
        assertThat(handles).isFalse();
    }

    @Test
    void shouldReturnTreeFilterWhenOverrideTreeBuilder() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(true);

        // When
        SmsRouteTreeBuilder treeBuilder = nodeEndpointDefinition.overrideTreeBuilder();

        // Then
        var tree = SmsRouteTreeBuilder.tree(Map.of(), null, treeBuilder);
        var nodeTree = tree.handles("testService");
        assertThat(nodeTree).isNotNull();
    }
}
