/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.guava.common.collect.Multimaps.newListMultimap;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.trees.engine.Outcome.FALSE;
import static org.forgerock.openam.auth.trees.engine.Outcome.TRUE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.callback.NameCallback;

import org.forgerock.guava.common.collect.ImmutableListMultimap;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.audit.AuthenticationNodeEventAuditor;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class AuthTreeExecutorTest {
    public static final String TREE_NAME = "treeName";
    @Mock
    NodeRegistry nodeRegistry;
    @Mock
    NodeFactory nodeFactory;
    @Mock
    Node node;
    @RealmTestHelper.RealmHelper
    static Realm realm;
    @Mock
    AuthenticationNodeEventAuditor authenticationNodeEventAuditor;

    @InjectMocks
    AuthTreeExecutor authTreeExecutor;

    @BeforeMethod
    public void setup() {
        authTreeExecutor = null;
        authenticationNodeEventAuditor = null;
        initMocks(this);
    }

    @DataProvider
    public Object[][] outcomes() {
        return new Object[][]{ { "trueOutcome", TRUE }, { "falseOutcome", FALSE } };
    }

    @Test(dataProvider = "outcomes")
    public void whenNodeReturnsOutcomeExecutorSelectsCorrectNode(String outcome, Outcome treeOutcome) throws Exception {
        // Given
        Action action = Action.goTo(outcome).build();
        given(node.process(any(TreeContext.class))).willReturn(action);
        UUID nodeId1 = UUID.randomUUID();
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect("trueOutcome", TREE_NODE_SUCCESS_ID)
                .connect("falseOutcome", TREE_NODE_FAILURE_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList());

        // When
        TreeResult afterTreeState = authTreeExecutor.process(realm, authTree, treeState, emptyList(),
                new Builder().build());

        // Then
        assertThat(afterTreeState.outcome).isEqualTo(treeOutcome);
    }

    @Test
    public void whenTheNodeNeedsInputTheExecutorReturnsTheCallbacks() throws Exception {
        // Given
        NameCallback callback = new NameCallback("fred");
        Action result = Action.send(singletonList(callback))
                .build();
        given(node.process(any(TreeContext.class))).willReturn(result);
        UUID nodeId = UUID.randomUUID();
        given(nodeFactory.createNode(eq("type1"), eq(nodeId), eq(realm), anyObject())).willReturn(node);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name(TREE_NAME)
                .node(nodeId, "type1", "display1")
                .connect("trueOutcome", TREE_NODE_SUCCESS_ID)
                .connect("falseOutcome", TREE_NODE_FAILURE_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList());

        // When
        TreeResult afterTreeState = authTreeExecutor.process(realm, authTree, treeState, emptyList(),
                new Builder().build());

        // Then
        assertThat(afterTreeState.outcome).isEqualTo(Outcome.NEED_INPUT);
        assertThat(afterTreeState.callbacks).hasSize(1).contains(callback);
        assertThat(afterTreeState.treeState.currentNodeId).isEqualTo(nodeId);
    }

    @Test
    public void shouldPassContextToTheNodeWithValuesThatExecutorReceives() throws Exception {
        // Given
        NameCallback callback = new NameCallback("fred");
        Action result = Action.goTo("trueOutcome")
                .build();
        given(node.process(any(TreeContext.class))).willReturn(result);
        UUID nodeId = UUID.randomUUID();
        given(nodeFactory.createNode(eq("type1"), eq(nodeId), eq(realm), anyObject())).willReturn(node);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name(TREE_NAME)
                .node(nodeId, "type1", "display1")
                .connect("trueOutcome", TREE_NODE_SUCCESS_ID)
                .connect("falseOutcome", TREE_NODE_FAILURE_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList());
        ListMultimap<String, String> httpHeaders = ImmutableListMultimap.of("fred", "test");
        Map<String, String> cookies = ImmutableMap.of("badger", "weasel");
        PreferredLocales locales = new PreferredLocales();
        String clientIp = "127.0.0.1";

        // When
        authTreeExecutor.process(realm, authTree, treeState, singletonList(callback), new Builder()
                .headers(httpHeaders).cookies(cookies).locales(locales).clientIp(clientIp).build());

        // Then
        ArgumentCaptor<TreeContext> captor = ArgumentCaptor.forClass(TreeContext.class);
        verify(node).process(captor.capture());
        assertThat(captor.getValue().request.headers).isEqualTo(httpHeaders);
        assertThat(captor.getValue().request.cookies).isEqualTo(cookies);
        assertThat(captor.getValue().request.locales).isSameAs(locales);
        assertThat(captor.getValue().request.clientIp).isSameAs(clientIp);
        assertThat(captor.getValue().hasCallbacks()).isTrue();
        assertThat(captor.getValue().getCallback(NameCallback.class)).contains(callback);
    }

    @Test
    public void shouldKeepProcessingTheTreeTillEndStateIsReached() throws Exception {
        // Given
        Action result = Action.goTo("trueOutcome")
                .replaceSharedState(json(object(field("dummy", "value"))))
                .build();
        given(node.process(any(TreeContext.class))).willReturn(result);
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        given(nodeFactory.createNode(eq("type1"), any(UUID.class), eq(realm), anyObject())).willReturn(node);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect("trueOutcome", nodeId2)
                .done()
                .node(nodeId2, "type1", "display1")
                .connect("trueOutcome", TREE_NODE_SUCCESS_ID)
                .connect("falseOutcome", TREE_NODE_FAILURE_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList());

        // When
        TreeResult afterTreeState = authTreeExecutor.process(realm, authTree, treeState, emptyList(),
                new Builder().build());

        // Then
        assertThat(afterTreeState.outcome).isEqualTo(Outcome.TRUE);
        verify(node, times(2)).process(any(TreeContext.class));
    }

    @Test
    public void shouldPassSharedStateCorrectly() throws Exception {
        // Given
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        MockNode node1 = new MockNode("node1");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node1);
        MockNode node2 = new MockNode("node2");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId2), eq(realm), anyObject())).willReturn(node2);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, nodeId2)
                .done()
                .node(nodeId2, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object(field("initial", "initial"))), null, emptyMap(), emptyList(),
                emptyList());

        // When
        TreeResult afterTreeState = authTreeExecutor.process(realm, authTree, treeState, emptyList(),
                new Builder().build());

        // Then
        assertThat(node1.receivedState).isObject().containsFields("initial");
        assertThat(node2.receivedState).isObject().containsFields("initial", "node1");
        assertThat(afterTreeState.treeState.sharedState).isObject().containsFields("initial", "node1", "node2");
    }

    @Test
    public void shouldPassSessionPropertiesCorrectly() throws Exception {
        // Given
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();

        Action result = Action.goTo("outcome")
                .putSessionProperty("property2", "propertyValue2")
              .build();

        MockNode node1 = new MockNode("node1");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node1);

        given(nodeFactory.createNode(eq("type1"), eq(nodeId2), eq(realm), anyObject())).willReturn(node);
        given(node.process(any(TreeContext.class))).willReturn(result);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, nodeId2)
                .done()
                .node(nodeId2, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null,
                                            ImmutableMap.of("test", "test"), emptyList(), emptyList());

        // When
        TreeResult afterTreeState = authTreeExecutor.process(realm, authTree, treeState, emptyList(),
                new Builder().build());

        // Then
        assertThat(afterTreeState.treeState.sessionProperties).containsKeys("test", "property2");
        assertThat(afterTreeState.treeState.sessionProperties).containsValues("test", "propertyValue2");
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenTheNodeThrowsAnExceptionTheExecutorThrowsTheException() throws Exception {
        // Given
        given(node.process(any(TreeContext.class))).willThrow(new NodeProcessException("fail"));
        UUID nodeId = UUID.randomUUID();
        given(nodeFactory.createNode(eq("type1"), eq(nodeId), eq(realm), anyObject())).willReturn(node);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId)
                .name("something")
                .node(nodeId, "type1", "display1")
                .connect("trueOutcome", TREE_NODE_SUCCESS_ID)
                .connect("falseOutcome", TREE_NODE_FAILURE_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList());

        // When
        authTreeExecutor.process(realm, authTree, treeState, emptyList(), new Builder().build());

        // Then - Exception
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void whenTargetAuthenticationLevelCannotBeReachedInTheMiddleOfTheTreeTheExecutorThrowsTheException()
            throws Exception {
        // Given
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        MockNode node1 = new MockNode("node1");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node1);
        MockNode node2 = new MockNode("node2");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId2), eq(realm), anyObject())).willReturn(node2);
        given(nodeRegistry.getNodeType("type1")).willReturn((Class) Node.class);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry)
                .entryNodeId(nodeId1)
                .realm(realm)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, nodeId2)
                .done()
                .node(nodeId2, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .done()
                .build();
        TreeState treeState = TreeState.createInitial(realm, 20);

        // When
        authTreeExecutor.process(realm, authTree, treeState, emptyList(), new Builder().build());

        // Then - Exception
    }

    @Test
    public void shouldAssembleContextCorrectly() throws Exception {
        // Given
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        MockNode node1 = new MockNode("node1");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node1);
        MockNode node2 = new MockNode("node2");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId2), eq(realm), anyObject())).willReturn(node2);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, nodeId2)
                .done()
                .node(nodeId2, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object(field("initial", "initial"))), null, emptyMap(), emptyList(),
                emptyList());
        ListMultimap<String, String> headers = ImmutableListMultimap.of();

        // When
        authTreeExecutor.process(realm, authTree, treeState, emptyList(), new Builder().headers(headers).build());

        // Then
        assertThat(node1.receivedContext.request.headers).isEqualTo(headers);
        assertThat(node2.receivedContext.request.headers).isEqualTo(headers);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldExposeHeadersAsUnmodifiable() throws Exception {
        // Given
        UUID nodeId1 = UUID.randomUUID();
        MockNode node1 = new MockNode("node1");
        given(nodeFactory.createNode(eq("type1"), eq(nodeId1), eq(realm), anyObject())).willReturn(node1);
        AuthTree authTree = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm)
                .entryNodeId(nodeId1)
                .name(TREE_NAME)
                .node(nodeId1, "type1", "display1")
                .connect(MockNode.OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .done()
                .build();
        TreeState treeState = new TreeState(json(object(field("initial", "initial"))), null, emptyMap(), emptyList(),
                emptyList());
        ListMultimap<String, String> headers = newListMultimap(new HashMap<>(), ArrayList::new);

        // When
        authTreeExecutor.process(realm, authTree, treeState, emptyList(), new Builder().headers(headers).build());
        node1.receivedContext.request.headers.put("a", "test");

        // Then - exception
    }


    private static class MockNode implements Node {

        public static final String OUTCOME_ID = "outcome";
        private final String key;
        private JsonValue receivedState;
        private TreeContext receivedContext;

        private MockNode(String key) {
            this.key = key;
        }

        @Override
        public Action process(TreeContext context) {
            receivedState = context.sharedState.copy();
            receivedContext = context;
            return Action.goTo(OUTCOME_ID)
                    .replaceSharedState(context.sharedState.copy().add(key, key))
                    .build();
        }
    }
}
