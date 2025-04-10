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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesScriptContext.CONFIG_PROVIDER_NODE;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextInputCallback;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderNodeBindings;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderNodeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Unit test for ConfigProviderNode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigProviderNodeTest {

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension(ConfigProviderNode.class);

    @Mock
    private Realm realm;
    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ConfigProviderNode.Config config;
    @Mock
    private ScriptEvaluator<ConfigProviderNodeBindings> scriptEvaluator;
    @Mock
    private ScriptIdentityRepository.Factory scriptIdentityRepoFactory;
    @Mock
    private ScriptedIdentityRepository.Factory scriptedIdentityRepoFactory;
    @Mock
    private IScriptedSecrets scriptedSecrets;
    @Mock
    private IScriptedSecrets.Factory secretsFactory;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private ConfigProviderNodeContext configProviderNodeContext;

    private Node nodeToExecute;
    private ConfigProviderNode configProviderNode;
    private Tree tree;
    private UUID nodeId;
    @Mock
    private AuthScriptUtilities authScriptUtilities;

    @Captor
    private ArgumentCaptor<ConfigProviderNodeBindings> bindingsCaptor;

    @BeforeEach
    void setup() throws Exception {
        nodeToExecute = new MyInnerNode(UUID.randomUUID());

        when(config.script()).thenReturn(Script.EMPTY_SCRIPT);
        when(config.nodeType()).thenReturn("MyInnerNode");

        when(nodeFactory.createDynamicNode(
                anyString(), any(), any(), any(Realm.class), any(Tree.class), anyBoolean(), any(UUID.class)))
                .thenReturn(nodeToExecute);
        when(nodeFactory.createDynamicNode(anyString(), any(), any(), any(Realm.class), any(Tree.class), anyBoolean()))
                .thenReturn(nodeToExecute);

        when(secretsFactory.create(any(), any())).thenReturn(scriptedSecrets);

        nodeId = UUID.randomUUID();
        tree = Mockito.mock(Tree.class);
    }

    @Test
    void testProcess() throws NodeProcessException, ScriptException {
        // given
        given(scriptEvaluatorFactory.create(CONFIG_PROVIDER_NODE, configProviderNodeContext)).willReturn(
                scriptEvaluator);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ConfigProviderNodeBindings.class),
                any(Realm.class)))
                .will(invocation -> {
                    Bindings bindings = new SimpleBindings();
                    bindings.put("config", Map.of("test", "banana"));
                    ScriptResult scriptResult = mock(ScriptResult.class);
                    given(scriptResult.getBindings()).willReturn(bindings);
                    return scriptResult;
                });
        given(nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class)).willReturn("InnerTreeEvaluatorNode");
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);
        OutcomeProvider.Outcome expectedOutcome = new MyInnerNode.MyOutcomeProvider()
                .getOutcomes(null, null).get(0);

        // when
        Action action = configProviderNode.process(new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo(expectedOutcome.id);
        assertThat(action.sessionHooks).hasSize(1);
        JsonValue sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.keys().contains(TreeHook.NODE_ID_KEY)).isFalse();
        assertThat(sessionHook.keys().contains(TreeHook.NODE_CONFIG_KEY)).isTrue();
        assertThat(sessionHook.get(TreeHook.NODE_CONFIG_KEY).get("test").asString()).isEqualTo("banana");
    }

    @Test
    void testInput() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);

        // then
        assertThat(configProviderNode.getInputs()).isEqualTo(nodeToExecute.getInputs());
    }

    @Test
    void testOutput() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);

        // then
        assertThat(configProviderNode.getOutputs()).isEqualTo(nodeToExecute.getOutputs());
    }

    @Test
    void testAuditEntryDetail() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);

        // then
        assertThat(configProviderNode.getAuditEntryDetail()).isEqualTo(nodeToExecute.getAuditEntryDetail());
    }

    @Test
    void testOutcomes() throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);
        MyInnerNode.MyOutcomeProvider nodeToExecuteOutcomeProvider = new MyInnerNode.MyOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", 1)).willReturn(nodeToExecuteOutcomeProvider);
        List<OutcomeProvider.Outcome> expectedOutcomes = nodeToExecuteOutcomeProvider
                .getOutcomes(null, null);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes = configProviderOutcomeProvider
                .getOutcomes(null, json(object(field("nodeType", "MyInnerNode"))));

        // then
        assertThat(actualOutcomes).containsOnly(expectedOutcomes.get(0),
                ConfigProviderNode.ConfigProviderOutcomeProvider.CONFIGURATION_FAILED_OUTCOME);

    }

    @Test
    void testBoundedOutcomes() throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext);
        BoundedOutcomeProvider nodeToExecuteOutcomeProvider = new MyBoundedOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", 1)).willReturn(nodeToExecuteOutcomeProvider);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes = configProviderOutcomeProvider
                .getOutcomes(null,
                        json(object(field("nodeType", "MyInnerNode"))));

        // then
        assertThat(actualOutcomes.stream().map(outcome -> outcome.id))
                .containsOnly("myBoundedOutcome", "CONFIGURATION_FAILED");

    }

    @Test
    void testProcessInnerTreeNodeStateFirstPass() throws ScriptException, NodeProcessException {
        // given
        when(config.script()).thenReturn(Script.EMPTY_SCRIPT);
        when(config.nodeType()).thenReturn(InnerTreeEvaluatorNode.class.getSimpleName());
        given(scriptEvaluatorFactory.create(CONFIG_PROVIDER_NODE, configProviderNodeContext)).willReturn(
                scriptEvaluator);
        given(nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class)).willReturn("InnerTreeEvaluatorNode");
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ConfigProviderNodeBindings.class),
                any(Realm.class)))
                .will(invocation -> {
                    Bindings bindings = new SimpleBindings();
                    bindings.put("config", Map.of("tree", "banana"));
                    ScriptResult scriptResult = mock(ScriptResult.class);
                    given(scriptResult.getBindings()).willReturn(bindings);
                    return scriptResult;
                });

        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId, authScriptUtilities,
                nodeRegistry, configProviderNodeContext);

        // when
        var action = configProviderNode.process(new TreeContext(json(object(field("tree", "banana"))),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty()));

        // then
        verify(scriptEvaluator).evaluateScript(any(Script.class), bindingsCaptor.capture(), any(Realm.class));

        ConfigProviderNodeBindings bindings = bindingsCaptor.getValue();
        var nodeState = bindings.legacyBindings().get("nodeState");
        assertThat(nodeState).isInstanceOfSatisfying(NodeState.class, state -> {
            assertThat(state.isDefined("tree")).isTrue();
            assertThat(state.get("tree").asString()).isEqualTo("banana");
        });

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.sharedState.isDefined("config")).isTrue();
        assertThat(action.sharedState.get("config").asMap().get("tree")).isEqualTo("banana");
    }

    @Test
    void testProcessInnerTreeNodeStateSecondPass() throws ScriptException, NodeProcessException {
        // given
        when(config.script()).thenReturn(Script.EMPTY_SCRIPT);
        when(config.nodeType()).thenReturn(InnerTreeEvaluatorNode.class.getSimpleName());
        given(scriptEvaluatorFactory.create(CONFIG_PROVIDER_NODE, configProviderNodeContext)).willReturn(
                scriptEvaluator);
        given(nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class)).willReturn("InnerTreeEvaluatorNode");

        JsonValue sharedState = json(object(
                field("sharedState", object(field("tree", "banana"))),
                field("secureState", object()),
                field("currentNodeId", UUID.randomUUID().toString()),
                field("sessionProperties", object()),
                field("webhooks", array()),
                field("config", object(field("tree", "banana")))
        ));
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId, authScriptUtilities,
                nodeRegistry, configProviderNodeContext);

        // when
        var action = configProviderNode.process(new TreeContext(sharedState,
                new ExternalRequestContext.Builder().build(), List.of(new TextInputCallback("some text")),
                Optional.empty()));

        // then
        verify(scriptEvaluator, never()).evaluateScript(any(Script.class), any(), any(Realm.class));
        assertThat(action.outcome).isEqualTo("myInnerNodeOutcome");
    }

    @Test
    void shouldLogDebugMessageWhenGetInputsFails() throws NodeProcessException {
        // given
        when(nodeFactory.createDynamicNode(anyString(), any(), any(), any(Realm.class), any(Tree.class), anyBoolean()))
                .thenThrow(new NodeProcessException("Test exception"));
        when(config.scriptInputs()).thenReturn(List.of("input1", "input2"));
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId, authScriptUtilities,
                nodeRegistry, configProviderNodeContext);

        // when
        var inputs = configProviderNode.getInputs();

        // then
        assertThat(inputs.length).isEqualTo(2);
        assertThat(loggerExtension.getDebug(ILoggingEvent::getFormattedMessage))
                .contains("Failed to collect inputs of contained node: MyInnerNode");
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getFormattedMessage)).isEmpty();
    }

    @Node.Metadata(outcomeProvider = MyInnerNode.MyOutcomeProvider.class, configClass = MyInnerNode.Config.class)
    static class MyInnerNode implements Node {

        private final UUID nodeId;
        InputState[] inputs = new InputState[] {new InputState("myInput"), new InputState("tree")};
        OutputState[] outputs = new OutputState[] {new OutputState("myOutput")};
        JsonValue auditEntryDetails = json(object(field("myField", "myValue")));

        MyInnerNode(UUID nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public Action process(TreeContext context) throws NodeProcessException {
            if (context.getStateFor(this).isDefined("tree")) {
                return Action.send(new NameCallback("Test")).replaceSharedState(json(object())).build();
            }
            return Action.goTo("myInnerNodeOutcome")
                    .addSessionHook(MySessionHook.class, nodeId, MyInnerNode.class.getSimpleName())
                    .build();
        }

        @Override
        public JsonValue getAuditEntryDetail() {
            return auditEntryDetails;
        }

        @Override
        public InputState[] getInputs() {
            return inputs;
        }

        @Override
        public OutputState[] getOutputs() {
            return outputs;
        }

        private interface Config {
            String test();
        }

        static class MyOutcomeProvider implements StaticOutcomeProvider {
            OutcomeProvider.Outcome myOutcome = new OutcomeProvider.Outcome("myInnerNodeOutcome",
                    "My Inner Node Outcome");

            @Override
            public List<Outcome> getOutcomes(PreferredLocales locales) {
                return List.of(myOutcome);
            }
        }

        private class MySessionHook implements TreeHook {
            @Override
            public void accept() throws TreeHookException {

            }
        }
    }

    private class MyBoundedOutcomeProvider implements BoundedOutcomeProvider {
        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) throws NodeProcessException {
            return List.of(new Outcome("myBoundedOutcome", "My Bounded Outcome"));
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            return List.of();
        }
    }
}
