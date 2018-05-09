/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.guava.common.collect.ImmutableSet;
import org.forgerock.openam.auth.node.api.OutcomeProvider.Outcome;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class PageNodeMultipleCallbacksOnlyInLastNodeTest {

    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private AnnotatedServiceRegistry serviceRegistry;
    @RealmTestHelper.RealmHelper
    private static Realm realm;
    private PageNode.MultipleCallbacksOnlyInLastNode validator;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        validator = new PageNode.MultipleCallbacksOnlyInLastNode(serviceRegistry, nodeRegistry, nodeFactory);
    }

    @Test
    public void shouldNotErrorForEmptyConfig() throws Exception {
        validator.validate(realm, emptyList(), singletonMap("nodes", emptySet()));
    }

    @Test
    public void shouldNotErrorIfLastNodeHasMultipleOutcomes() throws Exception {
        // Given
        Set<String> nodes = ImmutableSet.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", "Display Name"),
                "[1]=" + new PageNode.ChildNodeConfig("id2", "LastNodeType", "Display Name"));
        given(nodeRegistry.getConfigType("OtherType")).willReturn((Class) Map.class);
        given(nodeRegistry.getConfigType("LastNodeType")).willReturn((Class) List.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(serviceRegistry.getRealmInstance(List.class, realm, "id2")).willReturn(Optional.of(new ArrayList<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType")))
                .willReturn(singletonList(new Outcome("id", "Label")));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id2"), eq("LastNodeType")))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // no error
    }

    @Test(expectedExceptions = ServiceConfigException.class,
            expectedExceptionsMessageRegExp = "Only the last node in a page can have more than one outcome.*")
    public void shouldErrorIfNonLastNodeHasMultipleOutcomes() throws Exception {
        // Given
        Set<String> nodes = ImmutableSet.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", "Display Name"),
                "[1]=" + new PageNode.ChildNodeConfig("id2", "LastNodeType", "Display Name"));
        given(nodeRegistry.getConfigType("OtherType")).willReturn((Class) Map.class);
        given(nodeRegistry.getConfigType("LastNodeType")).willReturn((Class) List.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(serviceRegistry.getRealmInstance(List.class, realm, "id2")).willReturn(Optional.of(new ArrayList<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType")))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id2"), eq("LastNodeType")))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // no error
    }

    @Test(expectedExceptions = ServiceConfigException.class,
            expectedExceptionsMessageRegExp = "Node does not have any outcomes: OtherType.*")
    public void shouldErrorIfNodeHasNoOutcomes() throws Exception {
        // Given
        Set<String> nodes = ImmutableSet.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "OtherType", "Display Name"));
        given(nodeRegistry.getConfigType("OtherType")).willReturn((Class) Map.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq("OtherType")))
                .willReturn(emptyList());

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // no error
    }

    @DataProvider
    public Object[][] illegalNodeTypes() {
        return new Object[][] {
                {"NodeType", "PageNode"},
                {"NodeType", "InnerTreeEvaluatorNode"},
                {"PageNode", "NodeType"},
                {"InnerTreeEvaluatorNode", "NodeType"},
        };
    }

    @Test(dataProvider = "illegalNodeTypes",
            expectedExceptions = ServiceConfigException.class,
            expectedExceptionsMessageRegExp = "Illegal child node type: (PageNode|InnerTreeEvaluatorNode).*")
    public void shouldErrorIfAnyNodeIsIllegalNodeType(String node1Type, String node2Type) throws Exception {
        // Given
        Set<String> nodes = ImmutableSet.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", node1Type, "Display Name"),
                "[1]=" + new PageNode.ChildNodeConfig("id2", node2Type, "Display Name"));
        given(nodeRegistry.getConfigType(node1Type)).willReturn((Class) Map.class);
        given(nodeRegistry.getConfigType(node2Type)).willReturn((Class) List.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.of(new HashMap<>()));
        given(serviceRegistry.getRealmInstance(List.class, realm, "id2")).willReturn(Optional.of(new ArrayList<>()));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id1"), eq(node1Type)))
                .willReturn(singletonList(new Outcome("id", "Label")));
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("id2"), eq(node2Type)))
                .willReturn(asList(new Outcome("id1", "Label"), new Outcome("id2", "Label")));

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // error
    }

    @Test(expectedExceptions = ServiceConfigException.class,
            expectedExceptionsMessageRegExp = "Node does not exist: id1.*")
    public void shouldErrorIfNodeDoesNotExist() throws Exception {
        // Given
        Set<String> nodes = ImmutableSet.of(
                "[0]=" + new PageNode.ChildNodeConfig("id1", "NodeType", "Display Name"));
        given(nodeRegistry.getConfigType("NodeType")).willReturn((Class) Map.class);
        given(serviceRegistry.getRealmInstance(Map.class, realm, "id1")).willReturn(Optional.empty());

        // When
        validator.validate(realm, emptyList(), singletonMap("nodes", nodes));

        // Then
        // error
    }
}