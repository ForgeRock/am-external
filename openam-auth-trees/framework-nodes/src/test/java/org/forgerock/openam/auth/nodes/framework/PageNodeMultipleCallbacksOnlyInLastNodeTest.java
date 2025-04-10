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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.auth.node.api.OutcomeProvider.Outcome;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;

import com.iplanet.am.util.SystemPropertiesWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageNodeMultipleCallbacksOnlyInLastNodeTest {

    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private AnnotatedServiceRegistry serviceRegistry;
    @Mock
    private Realm realm;
    @Mock
    private SystemPropertiesWrapper systemProperties;

    private PageNode.MultipleCallbacksOnlyInLastNode validator;

    @BeforeEach
    void setup() throws Exception {
        validator = new PageNode.MultipleCallbacksOnlyInLastNode(serviceRegistry, nodeRegistry, nodeFactory,
                systemProperties);
    }

    @Test
    void shouldNotErrorForEmptyConfig() throws Exception {
        validator.validate(realm, emptyList(), singletonMap("nodes", emptySet()));
    }

    @Test
    void shouldNotErrorIfLastNodeHasMultipleOutcomes() throws Exception {
        // Given
        Set<String> nodes = Set.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", 1, "Display Name", systemProperties),
                "[1]=" + new PageNode.ChildNodeConfig("id2", "LastNodeType", 1, "Display Name", systemProperties));
        given(nodeRegistry.getConfigType("OtherType", 1)).willReturn((Class) Map.class);
        given(nodeRegistry.getConfigType("LastNodeType", 1)).willReturn((Class) List.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(serviceRegistry.getRealmInstance(List.class, realm, "id2")).willReturn(Optional.of(new ArrayList<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType"), eq(1)))
                .willReturn(singletonList(new Outcome("id", "Label")));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id2"), eq("LastNodeType"), eq(1)))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // no error
    }

    @Test
    void shouldErrorIfNonLastNodeHasMultipleOutcomes() throws Exception {
        // Given
        Set<String> nodes = Set.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", 1, "Display Name", systemProperties),
                "[1]=" + new PageNode.ChildNodeConfig("id2", "LastNodeType", 1, "Display Name", systemProperties));
        given(nodeRegistry.getConfigType("OtherType", 1)).willReturn((Class) Map.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType"), eq(1)))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));

        // When
        assertThatThrownBy(() -> validator.validate(realm, emptyList(), singletonMap("nodes", nodes)))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("Only the last node in a page can have more than one outcome");


        // Then
        // no error
    }

    @Test
    void shouldErrorIfNodeHasNoOutcomes() throws Exception {
        // Given
        Set<String> nodes = Set.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", 1, "Display Name", systemProperties));
        given(nodeRegistry.getConfigType("OtherType", 1)).willReturn((Class) Map.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType"), eq(1)))
                .willReturn(emptyList());

        // When
        assertThatThrownBy(() -> validator.validate(realm, emptyList(), singletonMap("nodes", nodes)))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("Node does not have any outcomes: OtherType");
        // Then
        // no error
    }

    public static Object[][] illegalNodeTypes() {
        return new Object[][] {
                {"NodeType", "PageNode"},
                {"NodeType", "InnerTreeEvaluatorNode"},
                {"PageNode", "NodeType"},
                {"InnerTreeEvaluatorNode", "NodeType"},
        };
    }

    @ParameterizedTest
    @MethodSource("illegalNodeTypes")
    public void shouldErrorIfAnyNodeIsIllegalNodeType(String node1Type, String node2Type) throws Exception {
        // Given
        Set<String> nodes = Set.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", node1Type, 1, "Display Name", systemProperties),
                "[1]=" + new PageNode.ChildNodeConfig("id2", node2Type, 1, "Display Name", systemProperties));
        lenient().when(nodeRegistry.getConfigType(node1Type, 1)).thenReturn((Class) Map.class);
        lenient().when(serviceRegistry.getRealmInstance(Map.class, realm, "id1"))
                .thenReturn(Optional.of(new HashMap<>()));
        lenient().when(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq(node1Type), eq(1)))
                .thenReturn(singletonList(new Outcome("id", "Label")));

        // When
        assertThatThrownBy(() -> validator.validate(realm, emptyList(), singletonMap("nodes", nodes)))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessageContaining("Illegal child node type:");

        // Then
        // error
    }

    @Test
    void shouldErrorIfNodeDoesNotExist() throws Exception {
        // Given
        Set<String> nodes = Set.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "NodeType", 1, "Display Name", systemProperties));
        given(nodeRegistry.getConfigType("NodeType", 1)).willReturn((Class) Map.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> validator.validate(realm, emptyList(), singletonMap("nodes", nodes)))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("Node does not exist: id1");

        // Then
        // error
    }
}
