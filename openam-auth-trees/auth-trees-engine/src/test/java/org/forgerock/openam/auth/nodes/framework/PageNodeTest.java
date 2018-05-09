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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.PageNode.PAGE_NODE_CALLBACKS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import javax.security.auth.callback.NameCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.util.Pair;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.IdentifiableCallback;

@Listeners(RealmTestHelper.RealmFixture.class)
public class PageNodeTest {
    private static final ExternalRequestContext REQUEST_CONTEXT = new ExternalRequestContext.Builder().build();
    private static final UUID LAST_NODE_UUID = UUID.randomUUID();
    @RealmTestHelper.RealmHelper
    private static Realm realm;
    @Mock
    private PageNode.Config config;
    @Mock
    private NodeFactory nodeFactory;
    private AuthTree tree;
    private PageNode node;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        tree = AuthTree.builder(nodeFactory, mock(NodeRegistry.class))
                .realm(realm)
                .entryNodeId(UUID.randomUUID())
                .name("Test tree")
                .build();
        node = new PageNode(config, realm, tree, nodeFactory);
    }

    @Test(expectedExceptions = NodeProcessException.class, expectedExceptionsMessageRegExp = ".*page has no nodes.*")
    public void shouldThrowExceptionWhenNoChildNodes() throws Exception {
        // Given
        given(config.nodes()).willReturn(emptyList());

        // When
        node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList()));
    }

    @Test
    public void shouldReturnActionFromLastNode() throws Exception {
        // Given
        given(config.nodes()).willReturn(singletonList(UUID.randomUUID() + ":Type:Display Name"));
        given(config.nodes()).willReturn(singletonList(LAST_NODE_UUID + ":Type:Display Name"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("outcome").build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.goTo("result").build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList()));

        // Then
        assertThat(result.sendingCallbacks()).isFalse();
        assertThat(result.outcome).isEqualTo("result");
    }

    @Test
    public void shouldReturnAllCallbacks() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:Display Name",
                LAST_NODE_UUID + ":Type:Display Name"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node2")).build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return ((IdentifiableCallback) callback).callback;
        }).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node1", "Node2");
    }

    @Test
    public void shouldReturnExistingCallbacksForNodesWithOutcome() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:Display Name",
                LAST_NODE_UUID + ":Type:Display Name"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("outcome").build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node3")).build());

        // When
        Action result = node.process(new TreeContext(
                json(object(field(PAGE_NODE_CALLBACKS, object(field("0", 0), field("1", 1))))),
                REQUEST_CONTEXT,
                asList(identifiableCallback(0, "Node1"), identifiableCallback(1, "Node2")
        )));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            IdentifiableCallback identifiableCallback = (IdentifiableCallback) callback;
            return identifiableCallback;
        }).extracting(callback -> {
            assertThat(callback.callback).isInstanceOf(NameCallback.class);
            return Pair.of(((NameCallback) callback.callback).getPrompt(), callback.id);
        }).containsExactly(Pair.of("Node1", 0), Pair.of("Node3", 2));

        ArgumentCaptor<TreeContext> contextCaptor = ArgumentCaptor.forClass(TreeContext.class);
        verify(node1).process(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getAllCallbacks()).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node1");
        verify(node2).process(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getAllCallbacks()).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node2");
    }

    private IdentifiableCallback identifiableCallback(int id, String nodePrompt) {
        return new IdentifiableCallback(id, new NameCallback(nodePrompt));
    }
}