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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Collections.singleton;
import static org.forgerock.am.trees.api.NodeRegistry.DEFAULT_VERSION;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.WILDCARD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.CURRENT_NODE_ID;
import static org.forgerock.openam.auth.nodes.framework.script.ConfigProviderNodeContext.CONFIG_PROVIDER_NEXT_GEN_NAME;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesScriptContext.CONFIG_PROVIDER_NODE;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesScriptContext.CONFIG_PROVIDER_NODE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.script.ScriptException;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.Tree;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderNodeBindings;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderNodeContext;
import org.forgerock.openam.auth.nodes.framework.typeadapters.StaticOutcomeNodeType;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that allows execution of a node of another node type by injecting configuration through executing a script.
 */
@Node.Metadata(outcomeProvider = ConfigProviderNode.ConfigProviderOutcomeProvider.class,
        configClass = ConfigProviderNode.Config.class,
        tags = {"utilities"})
public class ConfigProviderNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProviderNode.class);
    private static final String CONFIGURATION_FAILED_OUTCOME_ID = "CONFIGURATION_FAILED";
    private static final String CONFIG_IDENTIFIER = "config";
    private final Config config;
    private final Tree tree;
    private final Realm realm;
    private final NodeFactory nodeFactory;
    private final ScriptEvaluator<ConfigProviderNodeBindings> scriptEvaluator;
    private final ScriptIdentityRepository scriptIdentityRepo;
    private final IScriptedSecrets secrets;
    private final ChfHttpClient httpClient;
    private final UUID nodeId;
    private final AuthScriptUtilities authScriptUtils;
    private final ScriptedIdentityRepository scriptedIdentityRepo;
    private Map<String, Object> scriptOutcome;
    private JsonValue auditEntryDetail;
    private final NodeRegistry nodeRegistry;

    /**
     * Dependency injected constructor.
     *
     * @param config                    The configuration for the node.
     * @param tree                      The tree the node is in.
     * @param realm                     The realm the node is in.
     * @param nodeFactory               A node factory instance that will be used to obtain instances of child nodes.
     * @param scriptEvaluatorFactory    A script evaluator factory.
     * @param scriptIdentityRepoFactory A script identity repository.
     * @param scriptedIdentityRepositoryFactory A scripted identity repository.
     * @param secretsFactory            Provides access to secrets from scripts.
     * @param nodeId                    The UUID of the current authentication tree node.
     * @param authScriptUtils           Utilities for scripted nodes.
     * @param nodeRegistry              The node registry.
     * @param configProviderNodeContext The next-gen context for the config provider node.
     */
    @Inject
    public ConfigProviderNode(@Assisted Config config, @Assisted Tree tree, @Assisted Realm realm,
            NodeFactory nodeFactory, ScriptEvaluatorFactory scriptEvaluatorFactory,
            ScriptIdentityRepository.Factory scriptIdentityRepoFactory,
            ScriptedIdentityRepository.Factory scriptedIdentityRepositoryFactory,
            IScriptedSecrets.Factory secretsFactory,
            @Assisted UUID nodeId, AuthScriptUtilities authScriptUtils,  NodeRegistry nodeRegistry,
            ConfigProviderNodeContext configProviderNodeContext) {
        this.config = config;
        this.tree = tree;
        this.realm = realm;
        this.nodeFactory = nodeFactory;
        this.scriptEvaluator = scriptEvaluatorFactory.create(CONFIG_PROVIDER_NODE, configProviderNodeContext);
        this.scriptOutcome = new HashMap<>();
        this.scriptIdentityRepo = scriptIdentityRepoFactory.create(realm);
        this.scriptedIdentityRepo = scriptedIdentityRepositoryFactory.create(realm);
        this.secrets = secretsFactory.create(realm, singleton("scripted.node."));
        this.authScriptUtils = authScriptUtils;
        this.httpClient = authScriptUtils.getLegacyHttpClient(config.script());
        this.auditEntryDetail = null;
        this.nodeId = nodeId;
        this.nodeRegistry = nodeRegistry;

    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Node node;
        try {
            if (isResumingInnerTreeNode(context)) {
                scriptOutcome = context.sharedState.get(CONFIG_IDENTIFIER).asMap();
            } else {
                executeScript(context);
            }
            // TODO AME-27985: upgrade config provider to understand node versioning
            node = nodeFactory
                    .createDynamicNode(config.nodeType(), DEFAULT_VERSION, scriptOutcome, realm, tree, true, nodeId);
        } catch (ScriptException | NodeProcessException e) {
            logger.error("Failed to configure node: {}", config.nodeType(), e);
            return Action.goTo(CONFIGURATION_FAILED_OUTCOME_ID).build();
        }
        Action action = node.process(context);
        if (isInnerTreeSendingCallbacks(action)) {
            action.sharedState.put(CONFIG_IDENTIFIER, scriptOutcome);
        }
        adaptSessionHooks(action);
        return action;
    }

    private boolean isInnerTreeSendingCallbacks(Action action) {
        return nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class).equals(config.nodeType())
                       && action.sendingCallbacks();
    }

    private boolean isResumingInnerTreeNode(TreeContext context) {
        if (nodeRegistry.getNodeServiceName(InnerTreeEvaluatorNode.class).equals(config.nodeType())) {
            JsonValue currentNodeId = context.sharedState.get(CURRENT_NODE_ID);
            return (context.hasResumedFromSuspend() || context.hasCallbacks())
                    && currentNodeId.isNotNull();
        }
        return false;
    }

    /**
     * Session hooks by default use the node UUID to re-construct the node which initially created them at the end of
     * the tree so that they can use the configuration of that node to configure the hook. This is not possible with a
     * node that has been created by the config provider node as the configuration for this node may no longer exist.
     * Instead, we will remove the node UUID and add the full configuration of the node to the session hook.
     *
     * @param action the action response from the generated node.
     */
    private void adaptSessionHooks(Action action) {
        if (!action.sessionHooks.isEmpty()) {
            action.sessionHooks.forEach(hook -> {
                if (hook.keys().contains(TreeHook.NODE_ID_KEY)) {
                    hook.remove(TreeHook.NODE_ID_KEY);
                    hook.add(TreeHook.NODE_CONFIG_KEY, scriptOutcome);
                }
            });
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        try {
            Node node = nodeFactory.createDynamicNode(config.nodeType(), DEFAULT_VERSION, scriptOutcome, realm, tree,
                    false);
            JsonValue nodeAuditEntry = node.getAuditEntryDetail();
            if (auditEntryDetail != null && nodeAuditEntry != null) {
                return node.getAuditEntryDetail().merge(auditEntryDetail);
            } else if (nodeAuditEntry != null) {
                return nodeAuditEntry;
            }
        } catch (NodeProcessException e) {
            logger.debug("Failed to collect audit entry details of contained node: {}", config.nodeType(), e);
        }
        if (auditEntryDetail != null) {
            return auditEntryDetail;
        }
        return Node.super.getAuditEntryDetail();
    }

    @Override
    public InputState[] getInputs() {
        List<InputState> inputs = config.scriptInputs().stream()
                .filter(StringUtils::isNotBlank)
                .map(input -> new InputState(input, false))
                .collect(Collectors.toList());
        try {
            Node node = nodeFactory.createDynamicNode(config.nodeType(), DEFAULT_VERSION, scriptOutcome, realm, tree,
                    false);
            inputs.addAll(Arrays.stream(node.getInputs())
                    .filter(inputState -> StringUtils.isNotBlank(inputState.name))
                    .collect(Collectors.toList()));
        } catch (NodeProcessException e) {
            // There are times that getInputs is called for a node when the context is not in the expected
            // state to run the script. In these instances try to run getInputs however if getInputs
            // requires access to the configuration then it may fail or return incomplete inputs.
            logger.debug("Failed to collect inputs of contained node: {}", config.nodeType(), e);
        }
        return inputs.toArray(InputState[]::new);
    }

    @Override
    public OutputState[] getOutputs() {
        try {
            Node node = nodeFactory.createDynamicNode(config.nodeType(), DEFAULT_VERSION, scriptOutcome, realm, tree,
                    false);
            return node.getOutputs();
        } catch (NodeProcessException e) {
            logger.debug("Failed to collect outputs of contained node: {}", config.nodeType(), e);
            return Node.super.getOutputs();
        }
    }

    private void executeScript(final TreeContext context) throws ScriptException, NodeProcessException {

        Script script = config.script();

        ConfigProviderNodeBindings scriptBindings = ConfigProviderNodeBindings.builder()
                .withNodeState(context.getStateFor(this))
                .withIdRepo(scriptIdentityRepo)
                .withSecrets(secrets)
                .withHeaders(authScriptUtils.convertHeadersToModifiableObjects(context.request.headers))
                .withHttpClient(httpClient)
                .withQueryParameters(authScriptUtils.convertParametersToModifiableObjects(context.request.parameters))
                .withExistingSession(authScriptUtils.getSessionProperties(context.request.ssoTokenId))
                .withScriptedIdentityRepository(scriptedIdentityRepo)
                .build();

        ScriptResult scriptResult = scriptEvaluator.evaluateScript(script, scriptBindings, realm);
        Object rawConfig = scriptResult.getBindings().get(CONFIG_IDENTIFIER);
        if (!(rawConfig instanceof Map)) {
            throw new NodeProcessException("Script must set '" + CONFIG_IDENTIFIER + "' to a Map.");
        }
        scriptOutcome = (Map) rawConfig;
        auditEntryDetail = authScriptUtils.getAuditEntryDetails(scriptResult.getBindings());
    }

    /**
     * Node Config.
     */
    public interface Config {

        /**
         * The nodeType to be executed.
         *
         * @return The node type.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        @StaticOutcomeNodeType
        String nodeType();

        /**
         * The script that generates the configuration for this node.
         *
         * @return The script.
         */
        @Attribute(order = 200)
        @ScriptContext(legacyContext = CONFIG_PROVIDER_NODE_NAME, nextGenContext = CONFIG_PROVIDER_NEXT_GEN_NAME)
        default Script script() {
            return Script.EMPTY_SCRIPT;
        }


        /**
         * List of state input for the script.
         *
         * @return The list of state inputs required by the script.
         */
        @Attribute(order = 300)
        default List<String> scriptInputs() {
            return List.of(WILDCARD);
        }
    }

    /**
     * Get the underlying node type of the config provider node.
     * @return The node type.
     */
    public String getType() {
        return config.nodeType();
    }

    /**
     * Outcome provider for the ConfigProviderNode.
     */
    static class ConfigProviderOutcomeProvider implements OutcomeProvider {

        static final OutcomeProvider.Outcome CONFIGURATION_FAILED_OUTCOME =
                new OutcomeProvider.Outcome(CONFIGURATION_FAILED_OUTCOME_ID, "Configuration failure");

        private final Realm realm;
        private final NodeFactory nodeFactory;

        @Inject
        ConfigProviderOutcomeProvider(@Assisted Realm realm, NodeFactory nodeFactory) {
            this.realm = realm;
            this.nodeFactory = nodeFactory;
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            if (nodeAttributes.isNull() || nodeAttributes.get("nodeType").isNull()) {
                return List.of(CONFIGURATION_FAILED_OUTCOME);
            }
            List<Outcome> outcomes = new ArrayList<>();
            try {
                String nodeType = nodeAttributes.get("nodeType").asString();
                OutcomeProvider delegateOutcomeProvider = nodeFactory.getOutcomeProvider(realm, nodeType,
                        DEFAULT_VERSION);
                if (delegateOutcomeProvider instanceof StaticOutcomeProvider staticOutcomes) {
                    outcomes.addAll(staticOutcomes.getOutcomes(locales));
                } else if (delegateOutcomeProvider instanceof BoundedOutcomeProvider boundedOutcomes) {
                    outcomes.addAll(boundedOutcomes.getAllOutcomes(locales));
                }
            } catch (NodeProcessException e) {
                logger.warn("Failed to get outcome provider for node type.", e);
            }
            outcomes.add(CONFIGURATION_FAILED_OUTCOME);
            return Collections.unmodifiableList(outcomes);
        }
    }
}
