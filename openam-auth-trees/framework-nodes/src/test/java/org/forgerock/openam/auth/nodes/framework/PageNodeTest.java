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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.PageNode.PAGE_NODE_CALLBACKS;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.NameCallback;

import org.forgerock.am.trees.api.NodeEventAuditor;
import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.authentication.spi.IdentifiableCallback;

@ExtendWith(MockitoExtension.class)
public class PageNodeTest {
    private static final ExternalRequestContext REQUEST_CONTEXT = new ExternalRequestContext.Builder().build();
    private static final UUID LAST_NODE_UUID = UUID.randomUUID();
    @Mock
    private static Realm realm;
    @Mock
    private PageNode.Config config;
    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private Tree tree;
    private PageNode node;
    @Mock
    private NodeEventAuditor auditor;
    @Mock
    private LocalizedMessageProvider localizationHelper;
    @Mock
    private SystemPropertiesWrapper systemProperties;

    @BeforeEach
    void setup() throws Exception {
        node = new PageNode(config, realm, tree, nodeFactory, auditor, realm -> localizationHelper, systemProperties);
    }

    @Test
    void shouldThrowExceptionWhenNoChildNodes() {
        // Given
        given(config.nodes()).willReturn(emptyList());

        // When/Then
        assertThatThrownBy(() -> node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(),
                Optional.empty()))).isInstanceOfSatisfying(NodeProcessException.class,
                        ex -> assertThat(ex.getMessage()).contains("page has no nodes"));
    }

    @Test
    void shouldReturnActionFromLastNode() throws Exception {
        // Given
        given(config.nodes()).willReturn(singletonList(UUID.randomUUID() + ":Type:1:Display Name"));
        given(config.nodes()).willReturn(singletonList(LAST_NODE_UUID + ":Type:1:Display Name"));
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.goTo("result").build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isFalse();
        assertThat(result.outcome).isEqualTo("result");
    }

    @Test
    void shouldReturnAllCallbacks() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":Type:1:Display Name"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node2")).build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

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
    void shouldReturnTitleAndDescriptionWithCallbacks() throws Exception {
        Map<Locale, String> header = new HashMap<Locale, String>();
        header.put(Locale.ENGLISH, "Node Header");
        Map<Locale, String> desc = new HashMap<Locale, String>();
        desc.put(Locale.ENGLISH, "Node Description");

        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":Type:1:Display Name"));
        given(config.pageHeader()).willReturn(header);
        given(config.pageDescription()).willReturn(desc);
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node2")).build());
        given(localizationHelper.getLocalizedMessage(any(), any(), eq(header), eq(""))).willReturn("Node Header");
        given(localizationHelper.getLocalizedMessage(any(), any(), eq(desc), eq(""))).willReturn("Node Description");

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.returnProperties.values()).contains("Node Header");
        assertThat(result.returnProperties.values()).contains("Node Description");
    }

    @Test
    void shouldReturnExistingCallbacksForNodesWithOutcome() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":Type:1:Display Name"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("outcome").build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree))).willReturn(node2);
        given(node2.process(any())).willReturn(
                Action.send(new NameCallback("Node3"), new NameCallback("Node4")).build());

        // When
        Action result = node.process(new TreeContext(
                json(object(field(PAGE_NODE_CALLBACKS, object(field("0", 0), field("1", 1))))),
                REQUEST_CONTEXT,
                asList(identifiableCallback(0, "Node1"), identifiableCallback(1, "Node2")
                ), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return (IdentifiableCallback) callback;
        }).extracting(callback -> {
            assertThat(callback.callback).isInstanceOf(NameCallback.class);
            return Pair.of(((NameCallback) callback.callback).getPrompt(), callback.id);
        }).containsExactly(Pair.of("Node1", 0), Pair.of("Node3", 2), Pair.of("Node4", 3));

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

    @Test
    void shouldReturnAllCallbacksWithSelectIdPNodeWithIdPCallbackFirst() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node2")).build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return ((IdentifiableCallback) callback).callback;
        }).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node2", "Node1");
    }

    @Test
    void shouldProcessAllNodesWhenSelectIdPReturnsLocalAuthenticationOutcome() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("outcome").build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node2);
        given(node2.process(any())).willReturn(Action.goTo("localAuthentication").build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isFalse();
        assertThat(result.outcome).isEqualTo("localAuthentication");
        verify(node1).process(any());
    }

    @Test
    void shouldReturnCallbacksWhenSelectIdPReturnsLocalAuthenticationAndNodesSendCallbacks() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node2);
        given(node2.process(any())).willReturn(Action.goTo("localAuthentication").build());

        // When
        Action result = node.process(new TreeContext(
                json(object(field(PAGE_NODE_CALLBACKS, object(field("0", 0), field("1", 1))))),
                REQUEST_CONTEXT,
                asList(identifiableCallback(0, "Node1"), identifiableCallback(1, "Node2")), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return ((IdentifiableCallback) callback).callback;
        }).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node2", "Node1");
    }

    @Test
    void shouldSkipAllOtherNodesWhenSelectIdPReturnsSocialAuthenticationOutcome() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node2);
        given(node2.process(any())).willReturn(Action.goTo("socialAuthentication").build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isFalse();
        assertThat(result.outcome).isEqualTo("socialAuthentication");
        verify(node1, never()).process(any());
    }

    @Test
    void shouldProcessWhenSelectIdPReturnsLocalAuthenticationOutcomeWithNoOtherNodes() throws Exception {
        // Given
        given(config.nodes()).willReturn(singletonList(LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("localAuthentication").build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isFalse();
        assertThat(result.outcome).isEqualTo("localAuthentication");
        verify(node1).process(any());
    }

    @Test
    void shouldReturnCallbacksWithOnlySelectIdPNode() throws Exception {
        // Given
        given(config.nodes()).willReturn(singletonList(LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node1);
        given(node1.process(any())).willReturn(Action.send(new NameCallback("Node1")).build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return ((IdentifiableCallback) callback).callback;
        }).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node1");
    }

    @Test
    void shouldReturnCallbacksWhenSelectIdPReturnsCallbacksAndOtherNodesComplete() throws Exception {
        // Given
        given(config.nodes()).willReturn(asList(UUID.randomUUID() + ":Type:1:Display Name",
                LAST_NODE_UUID + ":SelectIdPNode:1:Select IdP"));
        Node node1 = mock(Node.class);
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node1);
        given(node1.process(any())).willReturn(Action.goTo("outcome").build());
        Node node2 = mock(Node.class);
        given(nodeFactory.createNode(eq("SelectIdPNode"), eq(1), eq(LAST_NODE_UUID), eq(realm), eq(tree)))
                .willReturn(node2);
        given(node2.process(any())).willReturn(Action.send(new NameCallback("Node2")).build());

        // When
        Action result = node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks).extracting(callback -> {
            assertThat(callback).isInstanceOf(IdentifiableCallback.class);
            return ((IdentifiableCallback) callback).callback;
        }).extracting(callback -> {
            assertThat(callback).isInstanceOf(NameCallback.class);
            return ((NameCallback) callback).getPrompt();
        }).containsExactly("Node2");
    }

    @Test
    void shouldAuditAllContainedNodes() throws Exception {
        // Given
        var firstUUID = UUID.randomUUID();
        given(config.nodes()).willReturn(asList(firstUUID + ":Type:1:Display Name",
                LAST_NODE_UUID + ":Type:1:Display Name"));
        Node node = mock(Node.class);
        given(node.getAuditEntryDetail()).willReturn(json(object(field("audit", "special"))));
        given(nodeFactory.createNode(eq("Type"), eq(1), any(UUID.class), eq(realm), eq(tree))).willReturn(node);
        var action = Action.goTo("outcome").build();
        given(node.process(any())).willAnswer(env -> {
            TreeContext context = env.getArgument(0);
            var state = context.sharedState;
            state.put("var", "test");
            return action;
        });

        // When
        this.node.process(new TreeContext(json(object()), REQUEST_CONTEXT, emptyList(), Optional.empty()));

        // Then
        verify(auditor).logAuditEvent(
                eq(tree),
                eq(action),
                eq("Type"),
                eq(1), eq(firstUUID),
                eq("Display Name"),
                argThat(a -> a.get("var").asString().equals("test")),
                argThat(a -> a.get("audit").asString().equals("special"))
        );
        assertThat(this.node.getAuditEntryDetail().get("auditInfo").get(0).get("nodeType").asString())
                .isEqualTo("Type");
        assertThat(this.node.getAuditEntryDetail().get("auditInfo").get(0).get("nodeId").asString())
                .isEqualTo(firstUUID.toString());
    }

    private IdentifiableCallback identifiableCallback(int id, String nodePrompt) {
        return new IdentifiableCallback(id, new NameCallback(nodePrompt));
    }

    @Test
    void shouldReturnAllChildrenWhenGetChildren() {
        // given
        var uuid = UUID.randomUUID().toString();
        given(config.nodes()).willReturn(asList(uuid + ":Type 1:1:Display Name 1",
                LAST_NODE_UUID + ":Type 2:1:Display Name 2"));

        // When
        List<PageNode.ChildNodeConfig> children = node.getChildren();

        // Then
        var expectedNode1 = new PageNode.ChildNodeConfig(uuid, "Type 1", 1, "Display Name 1", systemProperties);
        var expectedNode2 = new PageNode.ChildNodeConfig(LAST_NODE_UUID.toString(), "Type 2", 1, "Display Name 2",
                systemProperties);
        assertThat(children).containsExactly(expectedNode1, expectedNode2);

    }
}
