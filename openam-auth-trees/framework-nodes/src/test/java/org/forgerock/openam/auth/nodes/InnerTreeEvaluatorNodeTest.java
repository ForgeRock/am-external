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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.DESCRIPTION;
import static org.forgerock.openam.auth.node.api.Action.HEADER;
import static org.forgerock.openam.auth.node.api.Action.STAGE;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.CURRENT_NODE_ID;
import static org.forgerock.openam.authentication.modules.scripted.Scripted.SHARED_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.trees.api.Outcome;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.am.trees.model.SessionProperties;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeState;
import org.forgerock.json.JsonValue;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SuspensionHandler;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.InnerTreeEvaluatorNode;
import org.forgerock.openam.auth.nodes.helpers.AuthSessionHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.session.Session;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InnerTreeEvaluatorNodeTest {

    private static final String TEST_STATE_CONTAINER = "testStateContainer";

    @Mock
    private InnerTreeEvaluatorNode.Config serviceConfig;
    @Mock
    private TreeExecutor executor;
    @Mock
    private TreeProvider treeProvider;
    @Mock
    private Realm realm;
    @Mock
    private Tree tree;
    @Mock
    private Session session;
    @Mock
    private AuthSessionHelper authSessionHelper;

    private InnerTreeEvaluatorNode node;

    @BeforeEach
    void before() throws Exception {
        given(serviceConfig.tree()).willReturn("mock_tree");
        given(treeProvider.getTree(realm, "mock_tree")).willAnswer(inv -> Optional.of(tree));
        given(tree.isEnabled()).willReturn(true);
        node = new InnerTreeEvaluatorNode(treeProvider, executor, realm, serviceConfig, Set.of(TEST_STATE_CONTAINER),
                authSessionHelper);
    }

    @Test
    void whenInnerTreeReturnsTrueOutcomeIsTrue() throws Exception {
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().build(), Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    void whenInnerTreeReturnsFalseOutcomeIsFalse() throws Exception {
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().build(), Outcome.FALSE));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    void whenInnerTreeReturnsNeedInputOutcomeIsNeedInput() throws Exception {
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(UUID.randomUUID()).build(),
                        Outcome.NEED_INPUT,
                        null, List.of(new PasswordCallback("prompt", false))));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.outcome).isNull();
    }

    @Test
    void whenInnerTreeReturnsNeedInputCallbacksAreReturnsInNodeResult() throws Exception {
        List<Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(UUID.randomUUID()).build(),
                        Outcome.NEED_INPUT,
                        null, callbacks));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.callbacks).isEqualTo(callbacks);
    }

    @Test
    void whenInnerTreeReturnsTrueInnerSharedStateIsReturnedAsSharedState() throws Exception {
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        TreeState treeState = TreeState.builder()
                .withSharedState(innerTreeSharedState)
                .withCurrentNodeId(innerTreeCurrentNodeId).build();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(treeState, Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.sharedState.getObject()).isSameAs(innerTreeSharedState.getObject());
    }

    @Test
    void whenInnerTreeReturnsNeedInputTreeStateIsReturnedAsSharedState() throws Exception {
        List<Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        TreeState treeState = TreeState.builder()
                .withSharedState(innerTreeSharedState)
                .withCurrentNodeId(innerTreeCurrentNodeId).build();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(treeState, Outcome.NEED_INPUT, null, callbacks));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        AssertJJsonValueAssert.assertThat(result.sharedState).stringAt(CURRENT_NODE_ID)
                .isEqualTo(innerTreeCurrentNodeId.toString());
        assertThat(result.sharedState.get(SHARED_STATE).getObject()).isSameAs(innerTreeSharedState.getObject());
    }

    @Test
    void whenInnerTreeReturnsNeedInputTitleDescriptionAndStageAreReturnedIfPresent() throws Exception {
        List<Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        JsonValue properties = json(object(
                field(HEADER, "header"),
                field(DESCRIPTION, "description"),
                field(STAGE, "stage")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(UUID.randomUUID()).build(),
                        Outcome.NEED_INPUT, properties.asMap(), callbacks));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(json(result.returnProperties).isEqualTo(properties)).isTrue();
    }

    @Test
    void processWithoutCallbacksPassesOuterTreeSharedStateAsInitialInnerTreeSharedState() throws Exception {
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue outerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder()
                        .withSharedState(outerTreeSharedState).withCurrentNodeId(innerTreeCurrentNodeId).build(),
                        Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(emptyList());
        TreeContext context = getContext(emptyList(), outerTreeSharedState, "123");

        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), eq(false), any());
        assertThat(treeState.getValue().currentNodeId).isNull();
        assertThat(treeState.getValue().sharedState.getObject()).isEqualTo(outerTreeSharedState.getObject());
    }

    @Test
    void processWithCallbacksWithoutCurrentNodeIdPassesOuterTreeSharedStateAsInitialInnerTreeSharedState()
            throws Exception {
        List<? extends Callback> callbacks = List.of(new ChoiceCallback("prompt", new String[] {"tree" },
                0, false));
        JsonValue outerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(
                        TreeState.builder().withSharedState(outerTreeSharedState).build(), Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(List.of());
        TreeContext context = getContext(callbacks, outerTreeSharedState, "123");
        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), eq(false), any());
        assertThat(treeState.getValue().currentNodeId).isNull();
        assertThat(treeState.getValue().sharedState.getObject()).isEqualTo(outerTreeSharedState.getObject());
    }

    @Test
    void whenInnerTreeHandlesCallbacksItReconstructsInnerTreeState() throws Exception {
        given(session.getMaxSessionTime()).willReturn(3L);
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        List<? extends Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder()
                        .withSharedState(innerTreeSharedState).withCurrentNodeId(innerTreeCurrentNodeId).build(),
                        Outcome.TRUE));
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState, "123");

        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), eq(false), any());
        assertThat(treeState.getValue().currentNodeId).isEqualTo(innerTreeCurrentNodeId);
        assertThat(treeState.getValue().sharedState.getObject()).isEqualTo(innerTreeSharedState.getObject());
    }

    @Test
    void shouldPassHttpHeadersIntoInnerTree() throws Exception {
        given(session.getMaxSessionTime()).willReturn(3L);
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        List<? extends Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        TreeState treeState = TreeState.builder()
                .withSharedState(innerTreeSharedState)
                .withCurrentNodeId(innerTreeCurrentNodeId).build();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(treeState, Outcome.TRUE));
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState, "123");

        node.process(context);

        verify(executor).process(eq(realm), eq(tree), any(), any(), eq(false), any());
    }

    @Test
    void whenInnerTreeReturnsSuspendTheSuspensionHandlerAndSuspendDurationArePassedThrough() throws Exception {
        SuspensionHandler handler = mock(SuspensionHandler.class);
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(UUID.randomUUID()).build(),
                        Outcome.SUSPENDED, emptyMap(), emptyList(), handler, Duration.ofMinutes(1), null));
        given(tree.visitNodes(any())).willReturn(emptyList());

        Action result = node.process(getContext());

        assertThat(result.suspensionHandler).isEqualTo(handler);
        assertThat(result.suspendDuration).contains(Duration.ofMinutes(1));
    }

    @Test
    void whenExceptionOutcomeAssertThatInnerTreeRethrowsWhenDisplayErrorOutcomeFalse() throws Exception {
        given(serviceConfig.displayErrorOutcome()).willReturn(false);
        var exception = new NodeProcessException("error");
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(UUID.randomUUID()).build(),
                        Outcome.EXCEPTION, emptyMap(), emptyList(), null, Duration.ofMinutes(1), exception));
        given(tree.visitNodes(any())).willReturn(emptyList());

        assertThatThrownBy(() -> node.process(getContext())).isEqualTo(exception);
    }

    @Test
    void shouldGoToErrorOutcomeWhenInnerTreeThrowsExceptionAndDisplayErrorOutcomeTrue() throws Exception {
        // given
        given(serviceConfig.displayErrorOutcome()).willReturn(true);
        var exception = new NodeProcessException(new RuntimeException("Some error"));
        UUID nodeId = UUID.randomUUID();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(nodeId).build(), Outcome.EXCEPTION,
                        emptyMap(), emptyList(), null, Duration.ofMinutes(1), exception));
        given(tree.visitNodes(any())).willReturn(emptyList());
        Tree.Node failedNode = mock(Tree.Node.class);
        given(failedNode.id()).willReturn(nodeId);
        given(failedNode.type()).willReturn("ErrorNode");
        given(failedNode.displayName()).willReturn("My Error Node");
        given(tree.getNodeForId(nodeId)).willReturn(failedNode);

        // when
        var result = node.process(getContext());

        // then
        assertThat(result.outcome).isEqualTo("error");
        Map<String, Object> errorNode = result.sharedState.get("errorNode").asMap();
        String errorMessage = result.sharedState.get("errorMessage").asString();
        assertThat(errorNode).containsExactlyInAnyOrderEntriesOf(Map.of("id", nodeId.toString(),
                "type", "ErrorNode",
                "displayName", "My Error Node"));
        assertThat(errorMessage).isEqualTo("java.lang.RuntimeException: Some error");
    }

    @Test
    void innerTreeReturnsInputs() throws Exception {
        InputState input = new InputState("test");
        given(tree.visitNodes(any())).willReturn(List.of(input));

        InputState[] inputs = node.getInputs();

        assertThat(inputs.length).isEqualTo(1);
        assertThat(inputs[0].name).isEqualTo("test");
    }

    @Test
    void innerTreeReturnsInputsWithNoDuplicates() throws Exception {
        given(tree.visitNodes(any())).willReturn(List.of(new InputState("test")));

        InputState[] inputs = node.getInputs();

        assertThat(inputs.length).isEqualTo(1);
        assertThat(inputs[0].name).isEqualTo("test");
    }

    @Test
    void innerTreeReturnsRequiredInputInsteadOfOptionalInput() throws Exception {
        given(tree.visitNodes(any())).willReturn(List.of(new InputState("test")));

        InputState[] inputs = node.getInputs();

        assertThat(inputs.length).isEqualTo(1);
        assertThat(inputs[0].name).isEqualTo("test");
        assertThat(inputs[0].required).isTrue();
    }

    @Test
    void shouldPassTransientInputsIntoInnerTreeOnlyIfTheyAreInOuterTransientState() throws Exception {
        UUID nodeId = UUID.randomUUID();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(nodeId).build(), Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(List.of(new InputState("test"),  new InputState("nodata")));
        JsonValue outerTransientState = json(object(field("test", "value"), field("ignored", "value")));
        TreeContext context = new TreeContext(json(object()), outerTransientState, json(object()),
                               new Builder().build(), emptyList(), false, Optional.empty());

        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), eq(false), any());
        assertThat(treeState.getValue().transientState.get("test").asString()).isEqualTo("value");
        assertThat(treeState.getValue().transientState.get("ignored").isNotNull()).isFalse();
        assertThat(treeState.getValue().transientState.isDefined("nodata")).isFalse();
    }

    @Test
    void shouldPassTransientInputsIntoInnerTreeFromObjectAttributes() throws Exception {
        UUID nodeId = UUID.randomUUID();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(TreeState.builder().withCurrentNodeId(nodeId).build(), Outcome.TRUE));

        given(tree.visitNodes(any())).willReturn(List.of(new InputState("test")));
        JsonValue outerTransientState = json(object(
                field(TEST_STATE_CONTAINER, object(field("test", "value"))),
                field("ignored", "value")));
        TreeContext context = new TreeContext(json(object()), outerTransientState, json(object()),
                new Builder().build(), emptyList(), false, Optional.empty());

        node.process(context);

        ArgumentCaptor<TreeState> treeState = ArgumentCaptor.forClass(TreeState.class);
        verify(executor).process(eq(realm), eq(tree), treeState.capture(), any(), eq(false), any());
        assertThat(treeState.getValue().transientState.get(TEST_STATE_CONTAINER).get("test").asString())
                .isEqualTo("value");
        assertThat(treeState.getValue().transientState.get("ignored").isNotNull()).isFalse();
    }

    @Test
    void shouldAddSessionPropertiesToResult() throws Exception {
        Map<String, String> properties = Map.of("key1", "value1", "key2", "value2");
        TreeState treeState = TreeState.builder()
                .withCurrentNodeId(UUID.randomUUID())
                .withSessionProperties(new SessionProperties(properties))
                .build();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(treeState, Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(emptyList());
        TreeContext context = getContext();

        Action result = node.process(context);

        assertThat(result.sessionProperties).containsExactlyInAnyOrderEntriesOf(properties);
    }

    @Test
    void shouldAddSessionTimeoutValuesToResult() throws Exception {
        Duration maxSessionTime = Duration.ofMinutes(1);
        Duration maxIdleTime = Duration.ofMinutes(2);
        SessionProperties sessionProperties =
                new SessionProperties(emptyMap(), Optional.of(maxSessionTime), Optional.of(maxIdleTime));
        TreeState treeState = TreeState.builder()
                .withCurrentNodeId(UUID.randomUUID())
                .withSessionProperties(sessionProperties)
                .build();
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(treeState, Outcome.TRUE));
        given(tree.visitNodes(any())).willReturn(emptyList());
        TreeContext context = getContext();

        Action result = node.process(context);

        assertThat(result.maxSessionTime).isEqualTo(Optional.of(maxSessionTime));
        assertThat(result.maxIdleTime).isEqualTo(Optional.of(maxIdleTime));
    }

    @Test
    void shouldPopulateMaxTreeDurationFromContextAfterCallbacks() throws Exception {
        given(session.getMaxSessionTime()).willReturn(3L);
        given(authSessionHelper.getAuthSession(any())).willReturn(session);
        List<? extends Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(
                        TreeState.builder()
                                .withSharedState(innerTreeSharedState)
                                .withCurrentNodeId(innerTreeCurrentNodeId)
                                .withMaxTreeDuration(Duration.ofMinutes(3))
                                .build(),
                        Outcome.TRUE));
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState, "123");

        var result = node.process(context);

        assertThat(result.maxTreeDuration).contains(Duration.ofMinutes(3));
    }

    @Test
    void shouldNotPopulateMaxTreeDurationFromContextAfterCallbacksIfNoAuthId() throws Exception {
        List<? extends Callback> callbacks = List.of(new PasswordCallback("prompt", false));
        UUID innerTreeCurrentNodeId = UUID.randomUUID();
        JsonValue innerTreeSharedState = json(object(field("foo", "bar")));
        given(executor.process(eq(realm), eq(tree), any(), any(), eq(false), any()))
                .willReturn(new TreeResult(
                        TreeState.builder()
                                .withSharedState(innerTreeSharedState)
                                .withCurrentNodeId(innerTreeCurrentNodeId)
                                .build(),
                        Outcome.TRUE));
        JsonValue outerSharedState = json(object(
                field(SHARED_STATE, innerTreeSharedState),
                field(CURRENT_NODE_ID, innerTreeCurrentNodeId.toString())));
        TreeContext context = getContext(callbacks, outerSharedState, "");

        var result = node.process(context);

        assertThat(result.maxTreeDuration).isEmpty();
    }

    @Test
    void shouldHaveAllOutcomesWhenGetAllOutcomes() {
        // given
        InnerTreeEvaluatorNode.OutcomeProvider outcomeProvider = new InnerTreeEvaluatorNode.OutcomeProvider();

        // when
        var outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactlyInAnyOrder("true", "false",
                "error");
    }

    @Test
    void shouldHaveErrorOutcomeWhenDisplayErrorIsTrue() {
        // given
        InnerTreeEvaluatorNode.OutcomeProvider outcomeProvider = new InnerTreeEvaluatorNode.OutcomeProvider();

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(),
                json(object(field("displayErrorOutcome", true))));

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactlyInAnyOrder("true", "false",
                "error");
    }

    @Test
    void shouldHaveErrorOutcomeWhenDisplayErrorIsFalse() {
        // given
        InnerTreeEvaluatorNode.OutcomeProvider outcomeProvider = new InnerTreeEvaluatorNode.OutcomeProvider();

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(),
                json(object(field("displayErrorOutcome", false))));

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactlyInAnyOrder("true", "false");
    }

    private TreeContext getContext() {
        return getContext(emptyList(), json(object()), "123");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue outerSharedState, String authId) {
        return new TreeContext(outerSharedState, json(object()), json(object()), new Builder().authId(authId).build(),
                callbacks, false, Optional.empty());
    }
}
