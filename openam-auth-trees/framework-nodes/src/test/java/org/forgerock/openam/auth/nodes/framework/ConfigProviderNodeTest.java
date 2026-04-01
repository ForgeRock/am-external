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
 * Copyright 2021-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.ConfigProviderNode.ConfigProviderOutcomeProvider.CONFIGURATION_FAILED_OUTCOME;
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
import java.util.UUID;
import java.util.stream.Stream;

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
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.nodes.designer.domain.AttributeConfiguration;
import org.forgerock.openam.auth.nodes.designer.domain.NodeType;
import org.forgerock.openam.auth.nodes.designer.persistence.NodeTypeStore;
import org.forgerock.openam.auth.nodes.designer.persistence.NodeTypeStoreFactory;
import org.forgerock.openam.auth.nodes.designer.registry.NodeDesignerNodeTypeRegistry;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    @Mock
    private NodeTypeStoreFactory nodeTypeStoreFactory;
    @Mock
    private NodeTypeStore nodeTypeStore;
    @Mock
    private AuthScriptUtilities authScriptUtilities;
    @Mock
    private NodeType nodeType;
    @Mock
    private AttributeConfiguration testAttribute;
    @Mock
    private AttributeConfiguration alreadyUnderscoredAttribute;
    @Mock
    private AttributeConfiguration anotherKeyAttribute;
    @Mock
    private NodeDesignerNodeTypeRegistry nodeDesignerNodeTypeRegistry;
    @Mock
    private ScriptedLocalizedMessage.Factory scriptedLocalizedMessageFactory;

    private Node nodeToExecute;
    private ConfigProviderNode configProviderNode;
    private Tree tree;
    private UUID nodeId;

    @Captor
    private ArgumentCaptor<ConfigProviderNodeBindings> bindingsCaptor;

    @BeforeEach
    void setup() throws Exception {
        nodeId = UUID.randomUUID();
        nodeToExecute = new MyInnerNode(nodeId);

        when(config.script()).thenReturn(Script.EMPTY_SCRIPT);
        when(config.nodeType()).thenReturn("MyInnerNode");
        when(config.nodeVersion()).thenReturn(1);
        when(config.staticConfig()).thenReturn(json(object(field("rest", "katana"))));

        when(nodeFactory.createDynamicNode(
                anyString(), any(), any(), any(Realm.class), any(Tree.class), anyBoolean(), any(UUID.class)))
                .thenReturn(nodeToExecute);
        when(nodeFactory.createDynamicNode(anyString(), any(), any(), any(Realm.class), any(Tree.class), anyBoolean()))
                .thenReturn(nodeToExecute);

        when(secretsFactory.create(any(), any())).thenReturn(scriptedSecrets);
        when(nodeTypeStoreFactory.create()).thenReturn(nodeTypeStore);

        tree = Mockito.mock(Tree.class);
    }

    public static Stream<Arguments> nodeVersions() {
        return Stream.of(
                Arguments.of(1),
                Arguments.of(2)
        );
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
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        OutcomeProvider.Outcome expectedOutcome = new MyInnerNode.MyOutcomeProvider()
                .getOutcomes(null, null).get(0);

        // when
        Action action = configProviderNode.process(TreeContext.builder().build());

        // then
        assertThat(action.outcome).isEqualTo(expectedOutcome.id);
        assertThat(action.sessionHooks).hasSize(1);
        JsonValue sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.keys().contains(TreeHook.NODE_ID_KEY)).isFalse();
        assertThat(sessionHook.keys().contains(TreeHook.NODE_CONFIG_KEY)).isTrue();
        assertThat(sessionHook.get(TreeHook.NODE_CONFIG_KEY).get("test").asString()).isEqualTo("banana");
        assertThat(sessionHook.get(TreeHook.NODE_CONFIG_KEY).get("rest").asString()).isEqualTo("katana");
    }

    @Test
    void testInput() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

        // then
        assertThat(configProviderNode.getInputs()).isEqualTo(nodeToExecute.getInputs());
    }

    @Test
    void testOutput() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

        // then
        assertThat(configProviderNode.getOutputs()).isEqualTo(nodeToExecute.getOutputs());
    }

    @Test
    void testAuditEntryDetail() {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

        // then
        assertThat(configProviderNode.getAuditEntryDetail()).isEqualTo(nodeToExecute.getAuditEntryDetail());
    }

    @ParameterizedTest
    @MethodSource("nodeVersions")
    void testOutcomes(int nodeVersion) throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        MyInnerNode.MyOutcomeProvider nodeToExecuteOutcomeProvider = new MyInnerNode.MyOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", nodeVersion))
                .willReturn(nodeToExecuteOutcomeProvider);
        List<OutcomeProvider.Outcome> expectedOutcomes = nodeToExecuteOutcomeProvider
                .getOutcomes(null, null);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory, nodeRegistry);
        given(nodeRegistry.isNodeService("MyInnerNode", nodeVersion)).willReturn(true);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes = configProviderOutcomeProvider
                .getOutcomes(null, json(object(field("nodeType", "MyInnerNode"), field("nodeVersion", nodeVersion))));

        // then
        assertThat(actualOutcomes).containsOnly(expectedOutcomes.get(0), CONFIGURATION_FAILED_OUTCOME);

    }

    @Test
    void getOutcomesShouldReturnConfigFailedForInvalidNodeType() throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        MyInnerNode.MyOutcomeProvider nodeToExecuteOutcomeProvider = new MyInnerNode.MyOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", 1))
                .willReturn(nodeToExecuteOutcomeProvider);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory, nodeRegistry);
        given(nodeRegistry.isNodeService("MyInnerNode", 1)).willReturn(false);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes =
                configProviderOutcomeProvider
                        .getOutcomes(null, json(object(field("nodeType", "MyInnerNode"), field("nodeVersion", 1))));

        // then
        assertThat(actualOutcomes).containsOnly(CONFIGURATION_FAILED_OUTCOME);
    }

    @Test
    void getOutcomesShouldReturnLatestVersionWhenNodeVersionMissing() throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        MyInnerNode.MyOutcomeProvider nodeToExecuteOutcomeProvider = new MyInnerNode.MyOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", 2))
                .willReturn(nodeToExecuteOutcomeProvider);
        List<OutcomeProvider.Outcome> expectedOutcomes = nodeToExecuteOutcomeProvider
                                                                 .getOutcomes(null, null);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory, nodeRegistry);
        given(nodeRegistry.getVersions("MyInnerNode")).willReturn(List.of(1, 2));
        given(nodeRegistry.isNodeService("MyInnerNode", 2)).willReturn(true);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes =
                configProviderOutcomeProvider
                        .getOutcomes(null, json(object(field("nodeType", "MyInnerNode"))));

        // then
        assertThat(actualOutcomes).containsOnly(expectedOutcomes.get(0), CONFIGURATION_FAILED_OUTCOME);
    }

    @ParameterizedTest
    @MethodSource("nodeVersions")
    void testBoundedOutcomes(int nodeVersion) throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        BoundedOutcomeProvider nodeToExecuteOutcomeProvider = new MyBoundedOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", nodeVersion))
                .willReturn(nodeToExecuteOutcomeProvider);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory, nodeRegistry);
        given(nodeRegistry.isNodeService("MyInnerNode", nodeVersion)).willReturn(true);

        // when
        List<OutcomeProvider.Outcome> actualOutcomes = configProviderOutcomeProvider
                .getOutcomes(null,
                        json(object(field("nodeType", "MyInnerNode"), field("nodeVersion", nodeVersion))));

        // then
        assertThat(actualOutcomes.stream().map(outcome -> outcome.id))
                .containsOnly("myBoundedOutcome", "CONFIGURATION_FAILED");

    }

    @Test
    void testConfigProviderEnabledVariableOutcomes() throws NodeProcessException {
        // given
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, nodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        OutcomeProvider nodeToExecuteOutcomeProvider = new MyVariableOutcomeProvider();
        given(nodeFactory.getOutcomeProvider(realm, "MyInnerNode", 1))
                .willReturn(nodeToExecuteOutcomeProvider);
        ConfigProviderNode.ConfigProviderOutcomeProvider configProviderOutcomeProvider =
                new ConfigProviderNode.ConfigProviderOutcomeProvider(realm, nodeFactory, nodeRegistry);
        given(nodeRegistry.isNodeService("MyInnerNode", 1)).willReturn(true);
        JsonValue configuration = json(object(field("nodeType", "MyInnerNode"),
                field("nodeVersion", 1),
                field("staticConfig", object(
                        field("outcomes", array("outcome1", "outcome2")))
                )));

        // when
        List<OutcomeProvider.Outcome> actualOutcomes = configProviderOutcomeProvider
                                                               .getOutcomes(null, configuration);

        // then
        assertThat(actualOutcomes.stream().map(outcome -> outcome.id))
                .containsOnly("outcome1", "outcome2", "CONFIGURATION_FAILED");

    }

    @Test
    void testProcessDesignerNodeConfigAdaptation() throws Exception {
        // given
        given(scriptEvaluatorFactory.create(CONFIG_PROVIDER_NODE, configProviderNodeContext)).willReturn(
                scriptEvaluator);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(ConfigProviderNodeBindings.class),
                any(Realm.class)))
                .will(invocation -> {
                    Bindings bindings = new SimpleBindings();
                    bindings.put("config", Map.of("test", "banana", "_alreadyUnderscored", "apple", "anotherKey", 123));
                    ScriptResult scriptResult = mock(ScriptResult.class);
                    given(scriptResult.getBindings()).willReturn(bindings);
                    return scriptResult;
                });
        given(nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class)).willReturn("InnerTreeEvaluatorNode");
        given(nodeTypeStore.get("MyInnerNode-1")).willReturn(nodeType);
        given(nodeType.attributes()).willReturn(List.of(testAttribute, alreadyUnderscoredAttribute,
                anotherKeyAttribute));
        given(testAttribute.methodName()).willReturn("_test");
        given(testAttribute.binding()).willReturn("test");
        given(alreadyUnderscoredAttribute.methodName()).willReturn("_alreadyUnderscored");
        given(anotherKeyAttribute.methodName()).willReturn("_anotherKey");
        given(anotherKeyAttribute.binding()).willReturn("anotherKey");
        given(nodeDesignerNodeTypeRegistry.isRegistered("designer-MyInnerNode")).willReturn(true);
        given(nodeDesignerNodeTypeRegistry.getNodeTypeId("designer-MyInnerNode", 1))
                .willReturn("MyInnerNode-1");

        when(config.nodeType()).thenReturn("designer-MyInnerNode"); // Simulate a designer node
        UUID innerTreeNodeId = UUID.randomUUID();
        configProviderNode = new ConfigProviderNode(config, tree, realm, nodeFactory, scriptEvaluatorFactory,
                scriptIdentityRepoFactory, scriptedIdentityRepoFactory, secretsFactory, innerTreeNodeId,
                authScriptUtilities, nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);
        OutcomeProvider.Outcome expectedOutcome = new MyInnerNode.MyOutcomeProvider()
                .getOutcomes(null, null).get(0);

        // when
        Action action = configProviderNode.process(TreeContext.builder().build());

        // then
        assertThat(action.outcome).isEqualTo(expectedOutcome.id);
        assertThat(action.sessionHooks).hasSize(1);
        JsonValue sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.keys().contains(TreeHook.NODE_ID_KEY)).isTrue();
        assertThat(sessionHook.keys().contains(TreeHook.NODE_CONFIG_KEY)).isFalse();
    }

    @Test
    void testProcessInnerTreeNodeStateFirstPass() throws ScriptException, NodeProcessException {
        // given
        when(config.script()).thenReturn(Script.EMPTY_SCRIPT);
        when(config.nodeType()).thenReturn(InnerTreeEvaluatorNode.class.getSimpleName());
        when(config.nodeVersion()).thenReturn(1);
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
                nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

        // when
        var action = configProviderNode.process(TreeContext.builder()
                .sharedState(json(object(field("tree", "banana"))))
                .build());

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
                nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

        // when
        var action = configProviderNode.process(TreeContext.builder()
                .sharedState(sharedState)
                .callbacks(List.of(new TextInputCallback("some text")))
                .build());

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
                nodeRegistry, configProviderNodeContext, nodeTypeStoreFactory,
                nodeDesignerNodeTypeRegistry, scriptedLocalizedMessageFactory);

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
            public void accept() {

            }
        }
    }

    private static class MyBoundedOutcomeProvider implements BoundedOutcomeProvider {
        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            return List.of(new Outcome("myBoundedOutcome", "My Bounded Outcome"));
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return List.of();
        }
    }

    private static class MyVariableOutcomeProvider implements OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            if (nodeAttributes.isDefined("outcomes")) {
                return nodeAttributes.get("outcomes").asList(String.class)
                               .stream()
                               .map(s -> new Outcome(s, s))
                               .toList();
            }
            return List.of();
        }
    }
}
