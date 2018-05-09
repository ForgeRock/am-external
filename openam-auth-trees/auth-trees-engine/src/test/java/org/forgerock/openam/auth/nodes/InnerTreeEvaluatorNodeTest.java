/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.CURRENT_NODE_ID;
import static org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode.SHARED_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeExecutor;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.auth.trees.engine.Outcome;
import org.forgerock.openam.auth.trees.engine.TreeResult;
import org.forgerock.openam.auth.trees.engine.TreeState;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class InnerTreeEvaluatorNodeTest {

    private static final String TREE_NAME = "treename";

    @Mock
    InnerTreeEvaluatorNode.Config serviceConfig;
    @Mock
    AuthTreeExecutor executor;
    @Mock
    AuthTreeService treeService;
    @Mock
    NodeFactory nodeFactory;
    @Mock
    NodeRegistry nodeRegistry;

    @RealmTestHelper.RealmHelper
    static Realm realm;

    @BeforeMethod
    public void before() {
        initMocks(this);
        given(serviceConfig.tree()).willReturn("mock_tree");
    }

    @Test
    public void whenInnerTreeReturnsTrueOutcomeIsTrue() throws Exception {
        AuthTree tree = makeTree();

        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList()),
                        Outcome.TRUE, null));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void whenInnerTreeReturnsFalseOutcomeIsFalse() throws Exception {
        AuthTree tree = makeTree();

        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(json(object()), null, emptyMap(), emptyList(), emptyList()),
                        Outcome.FALSE, null));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void whenInnerTreeReturnsNeedInputOutcomeIsNeedInput() throws Exception {
        AuthTree tree = makeTree();

        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(json(object()), UUID.randomUUID(), emptyMap(), emptyList(),
                        emptyList()),
                        Outcome.NEED_INPUT,
                        ImmutableList.of(new PasswordCallback("prompt", false))));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.outcome).isNull();
    }

    @Test
    public void whenInnerTreeReturnsNeedInputCallbacksAreReturnsInNodeResult() throws Exception {
        AuthTree tree = makeTree();

        ImmutableList<Callback> callbacks = ImmutableList.of(new PasswordCallback("prompt", false));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(json(object()), UUID.randomUUID()),
                        Outcome.NEED_INPUT,
                        callbacks));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.callbacks).isEqualTo(callbacks);
    }

    @Test
    public void whenInnerTreeReturnsTrueInnerSharedStateIsReturnedAsSharedState() throws Exception {
        AuthTree tree = makeTree();

        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(innerTreeSharedState, innerTreeCurrentNodeId),
                        Outcome.TRUE,
                        null));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.sharedState.getObject()).isSameAs(innerTreeSharedState.getObject());
    }

    @Test
    public void whenInnerTreeReturnsNeedInputTreeStateIsReturnedAsSharedState() throws Exception {
        AuthTree tree = makeTree();

        ImmutableList<Callback> callbacks = ImmutableList.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(innerTreeSharedState, innerTreeCurrentNodeId),
                        Outcome.NEED_INPUT,
                        callbacks));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.sharedState).stringAt(CURRENT_NODE_ID).isEqualTo(innerTreeCurrentNodeId.toString());
        assertThat(result.sharedState.get(SHARED_STATE).getObject()).isSameAs(innerTreeSharedState.getObject());
    }

    @Test
    public void processWithoutCallbacksPassesOuterTreeSharedStateAsInitialInnerTreeSharedState() throws Exception {
        AuthTree tree = makeTree();

        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue outerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(outerTreeSharedState, innerTreeCurrentNodeId),
                        Outcome.TRUE,
                        null));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        TreeContext context = getContext(emptyList(), outerTreeSharedState);
        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), any());
        assertThat(treeState.getValue().currentNodeId).isNull();
        assertThat(treeState.getValue().sharedState.getObject()).isEqualTo(outerTreeSharedState.getObject());
    }

    @Test
    public void whenInnerTreeHandlesCallbacksItReconstructsInnerTreeState() throws Exception {
        AuthTree tree = makeTree();

        ImmutableList<? extends Callback> callbacks = ImmutableList.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(innerTreeSharedState, innerTreeCurrentNodeId),
                        Outcome.TRUE,
                        null));

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState);
        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), any());
        assertThat(treeState.getValue().currentNodeId).isEqualTo(innerTreeCurrentNodeId);
        assertThat(treeState.getValue().sharedState.getObject()).isEqualTo(innerTreeSharedState.getObject());
    }

    @Test
    public void shouldPassHttpHeadersIntoInnerTree() throws Exception {
        AuthTree tree = makeTree();

        ImmutableList<? extends Callback> callbacks = ImmutableList.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), any()))
                .willReturn(new TreeResult(new TreeState(innerTreeSharedState, innerTreeCurrentNodeId, emptyMap(),
                        emptyList(), emptyList()),
                        Outcome.TRUE,
                        null));
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState);

        InnerTreeEvaluatorNode node = new InnerTreeEvaluatorNode(treeService, executor, realm, serviceConfig);
        node.process(context);

        verify(executor).process(eq(realm), eq(tree), any(), any(), any());
    }

    private AuthTree makeTree() {
        AuthTree tree = AuthTree.builder(nodeFactory, nodeRegistry)
                .realm(realm)
                .entryNodeId(UUID.randomUUID())
                .name(TREE_NAME)
                .build();
        given(treeService.getTree(realm, "mock_tree")).willReturn(Optional.of(tree));
        return tree;
    }

    private TreeContext getContext() {
        return getContext(emptyList(), json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue outerSharedState) {
        return new TreeContext(outerSharedState, new Builder().build(), callbacks);
    }
}
