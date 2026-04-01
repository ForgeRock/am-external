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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.SelectIdPNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageNodeV2Test {

    @Mock
    private AnnotatedServiceRegistry serviceRegistry;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private Realm realm;

    private PageNodeV2.PageNodeV2Validator validator;

    @BeforeEach
    void setup() {
        Class normalNodeClass = NormalNode.class;
        Class selectIdpNodeClass = SelectIdPNode.class;
        lenient().when(nodeRegistry.getNodeType("NormalNode", 1)).thenReturn(normalNodeClass);
        lenient().when(nodeRegistry.getNodeType("SelectIdPNode", 1)).thenReturn(selectIdpNodeClass);
        validator = new PageNodeV2.PageNodeV2Validator(serviceRegistry, nodeRegistry, nodeFactory);
    }

    @Test
    void shouldPassWhenStandaloneNodesAreAtEnd() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":NormalNode:1:Name";
        String node2 = UUID.randomUUID() + ":" + SelectIdPNode.class.getSimpleName() + ":1:Name";
        List<String> nodes = Arrays.asList(node1, node2);

        mockSelectIdPNodeConfig(node2, true);

        // When/Then
        // Using LinkedHashSet to preserve order as validator iterates it
        validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes))));
    }


    @Test
    void shouldPassWhenSelectIdPNodeHasLocalAuthEnabled() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":" + SelectIdPNode.class.getSimpleName() + ":1:Name";
        List<String> nodes = Arrays.asList(node1);

        mockSelectIdPNodeConfig(node1, true);

        // When/Then
        validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes))));
    }

    @Test
    void shouldFailToValidateGivenStandaloneNodesAreNotAtEnd() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":" + SelectIdPNode.class.getSimpleName() + ":1:Name";
        String node2 = UUID.randomUUID() + ":NormalNode:1:Name";
        List<String> nodes = Arrays.asList(node1, node2);

        mockSelectIdPNodeConfig(node1, true);

        // When/Then
        assertThatThrownBy(() -> validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes)))))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("Standalone nodes must be at the end of the node list");
    }

    @Test
    void shouldFailToValidateGivenSelectIdPNodeHasLocalAuthDisabled() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":" + SelectIdPNode.class.getSimpleName() + ":1:Name";
        List<String> nodes = Arrays.asList(node1);

        mockSelectIdPNodeConfig(node1, false);

        // When/Then
        assertThatThrownBy(() -> validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes)))))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("SelectIdPNode must have local authentication enabled");
    }

    @Test
    void shouldFailToValidateGivenNonStandaloneNodeHasMultipleOutcomesAndIsNotLastNonStandalone() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":NormalNode:1:Name1";
        String node2 = UUID.randomUUID() + ":NormalNode:1:Name2";
        List<String> nodes = Arrays.asList(node1, node2);

        mockNodeOutcomes(node1, 2);

        // When/Then
        assertThatThrownBy(() -> validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes)))))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("Only the last non-standalone node in a page can have more than one outcome");
    }

    @Test
    void shouldPassWhenNonStandaloneNodeHasMultipleOutcomesAndIsLastNonStandalone() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":NormalNode:1:Name1";
        String node2 = UUID.randomUUID() + ":NormalNode:1:Name2";
        List<String> nodes = Arrays.asList(node1, node2);

        mockNodeOutcomes(node1, 1);

        // When/Then
        validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes))));
    }

    @Test
    void shouldPassWhenNonStandaloneNodeHasMultipleOutcomesAndIsLastNonStandaloneBeforeStandalone() throws Exception {
        // Given
        String node1 = UUID.randomUUID() + ":NormalNode:1:Name1";
        String node2 = UUID.randomUUID() + ":" + SelectIdPNode.class.getSimpleName() + ":1:Name";
        List<String> nodes = Arrays.asList(node1, node2);

        mockSelectIdPNodeConfig(node2, true);

        // When/Then
        validator.validate(realm, "serviceName", Collections.emptyList(),
                Map.of("nodes", new LinkedHashSet<>(formatNodes(nodes))));
    }

    private List<String> formatNodes(List<String> nodes) {
        java.util.ArrayList<String> formatted = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            formatted.add("[" + i + "]=" + nodes.get(i));
        }
        return formatted;
    }

    private void mockSelectIdPNodeConfig(String nodeString, boolean includeLocalAuth) throws Exception {
        PageNode.ChildNodeConfig node = PageNode.ChildNodeConfig.create(nodeString);
        SelectIdPNode.Config config = mock(SelectIdPNode.Config.class);
        given(config.includeLocalAuthentication()).willReturn(includeLocalAuth);

        Class configClass = SelectIdPNode.Config.class;
        given(nodeRegistry.getConfigType(node.type(), node.version())).willReturn(configClass);
        given(serviceRegistry.getRealmInstance(configClass, realm, node.id().toString()))
                .willReturn(Optional.of(config));
    }

    private void mockNodeOutcomes(String nodeString, int outcomeCount) throws Exception {
        PageNode.ChildNodeConfig node = PageNode.ChildNodeConfig.create(nodeString);
        List<OutcomeProvider.Outcome> outcomes = Collections.nCopies(outcomeCount, mock(OutcomeProvider.Outcome.class));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq(node.id().toString()), eq(node.type()),
                eq(node.version()))).willReturn(outcomes);
    }

    @Node.Metadata(outcomeProvider = PageNodeV2.PageDecisionV2OutcomeProvider.class,
            configClass = PageNodeV2.ConfigV2.class,
            configValidator = PageNodeV2.PageNodeV2Validator.class,
            tags = {"utilities"})
    @Node.VersionMetadata(name = "NormalNode", version = 1)
    private class NormalNode implements Node {

        @Override
        public Action process(TreeContext context) {
            throw new IllegalStateException("This should not be called.");
        }
    }
}
