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
 * Copyright 2017-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.WILDCARD;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE_NAME;
import static org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext.SCRIPTED_DECISION_NODE_NAME;
import static org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.script.Bindings;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.AbstractScriptedNode;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeBindings;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.cache.service.CacheManagerService;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptFeature;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that executes a script to make a decision.
 *
 * <p>The script is passed the shared state and must set an outcome as a boolean.</p>
 */
@Node.Metadata(outcomeProvider = ScriptedDecisionNode.ScriptedDecisionOutcomeProvider.class,
        configClass = ScriptedDecisionNode.Config.class,
        tags = {"utilities"})
public class ScriptedDecisionNode extends
        AbstractScriptedNode<ScriptedDecisionNode.Config, ScriptedDecisionNodeBindings> {
    private final ChfHttpClient httpClient;
    private final IScriptedSecrets secrets;
    private final ScriptIdentityRepository scriptIdentityRepository;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluatorFactory                A script evaluator factory.
     * @param config                                The node configuration.
     * @param realm                                 The realm the node is in, and that the request is targeting.
     * @param legacyScriptIdentityRepoFactory       factory to build access to the identity repo for this node's script
     * @param modernScriptedIdentityRepoFactory     factory to build access to the identity repo for this node's script
     * @param secretsFactory                        provides access to the secrets API for this node's script
     * @param authScriptUtils                       utilities for scripted nodes
     * @param scriptedDecisionNodeContext           The next-gen context for the scripted decision node.
     * @param tree                                  tree
     * @param cacheManagerServiceFactory            the cache manager service factory
     * @param nodeId                                The unique node ID.
     * @param scriptedLocalizedMessageFactory       Factory for scripted localized messages.
     */
    @Inject
    public ScriptedDecisionNode(
            ScriptEvaluatorFactory scriptEvaluatorFactory,
            @Assisted Config config,
            @Assisted Realm realm,
            @Assisted UUID nodeId,
            @Assisted Tree tree,
            ScriptIdentityRepository.Factory legacyScriptIdentityRepoFactory,
            Factory modernScriptedIdentityRepoFactory,
            IScriptedSecrets.Factory secretsFactory,
            AuthScriptUtilities authScriptUtils,
            ScriptedDecisionNodeContext scriptedDecisionNodeContext,
            CacheManagerService.Factory cacheManagerServiceFactory,
            ScriptedLocalizedMessage.Factory scriptedLocalizedMessageFactory) {
        super(realm, nodeId, config, tree,
                scriptEvaluatorFactory
                        .create(AUTHENTICATION_TREE_DECISION_NODE, scriptedDecisionNodeContext),
                modernScriptedIdentityRepoFactory,
                authScriptUtils,
                cacheManagerServiceFactory,
                scriptedLocalizedMessageFactory);

        this.httpClient = authScriptUtils.getLegacyHttpClient(config.script());
        this.secrets = secretsFactory.create(realm, singleton("scripted.node."));
        this.scriptIdentityRepository = legacyScriptIdentityRepoFactory.create(realm);
    }

    @Override
    protected Script getScript() {
        return config.script();
    }

    @Override
    protected List<String> getOutcomes() {
        return config.outcomes();
    }

    @Override
    protected ScriptedDecisionNodeBindings createScriptBindings(TreeContext context) throws NodeProcessException {
        JsonValue filteredShared = json(object());
        JsonValue filteredTransient = json(object());

        if (getEvaluatorVersion().hasFeature(ScriptFeature.DIRECT_STATE_ACCESS)) {
            filteredShared = filterInputs(context.sharedState);
            filteredTransient = filterInputs(context.transientState);
        }

        var uniqueBuilderSteps = ScriptedDecisionNodeBindings.builder()
                .withSharedState(filteredShared.getObject())
                .withTransientState(filteredTransient.getObject())
                .withHttpClient(httpClient)
                .withSecrets(secrets)
                .withScriptIdentityRepository(scriptIdentityRepository)
                .withCacheManager(cacheManagerService);

        return populateCommonBindings(uniqueBuilderSteps, context).build();
    }

    @Override
    protected EvaluatorVersion getEvaluatorVersion() {
        return getScript().getEvaluatorVersion();
    }

    @Override
    protected void afterScriptEvaluation(TreeContext context, Bindings bindings) throws NodeProcessException {
        if (getEvaluatorVersion().hasFeature(ScriptFeature.DIRECT_STATE_ACCESS)) {
            JsonValue scriptSharedState = json(bindings.get("sharedState"));
            JsonValue scriptTransientState = json(bindings.get("transientState"));

            if (!allOutputsArePresent(scriptSharedState, scriptTransientState)) {
                throw new NodeProcessException("Script did not provide all declared outputs");
            }
            checkForUnexpectedOutputs(scriptSharedState, scriptTransientState);
            transferOutputs(scriptSharedState, context.sharedState);
            transferOutputs(scriptTransientState, context.transientState);
        }
    }

    @Override
    public InputState[] getInputs() {
        return config.inputs().stream()
                .map(input -> new InputState(input, true))
                .toArray(InputState[]::new);
    }

    @Override
    public OutputState[] getOutputs() {
        return config.outputs().stream()
                .map(OutputState::new)
                .toArray(OutputState[]::new);
    }


    ////////////////// Private Methods //////////////////////////

    /**
     * Return only those state values declared as inputs.
     *
     * @param state Either shared or transient state
     * @return Filtered state
     */
    private JsonValue filterInputs(JsonValue state) {
        if (config.inputs().contains(WILDCARD)) {
            return state.copy();
        } else {
            JsonValue filtered = json(object());
            config.inputs().forEach(input -> {
                if (state.isDefined(input)) {
                    filtered.put(input, state.get(input));
                }
            });
            return filtered;
        }
    }

    /**
     * Return whether all declared attributes are present in state.
     *
     * @param sharedState    The shared state
     * @param transientState The transient state
     * @return true iff all declared outputs are present in at least on of shared or transient state
     */
    private boolean allOutputsArePresent(JsonValue sharedState, JsonValue transientState) {
        return config.outputs().contains(WILDCARD)
                || config.outputs().stream().allMatch(sharedState.copy().merge(transientState)::isDefined);
    }

    /**
     * Log any undeclared outputs.
     *
     * @param sharedState    The shared state
     * @param transientState The transient state
     */
    private void checkForUnexpectedOutputs(JsonValue sharedState, JsonValue transientState) {
        if (config.outputs().contains(WILDCARD)) {
            return;
        }
        List<String> allIO = new ArrayList<>();
        allIO.addAll(config.inputs());
        allIO.addAll(config.outputs());
        sharedState.copy().merge(transientState).asMap().keySet().forEach(key -> {
            if (!allIO.contains(key)) {
                logger.debug("The script produced an undeclared output: {}", key);
            }
        });
    }

    /**
     * Transfer declared output values from one JsonValue to another.
     *
     * @param source      The json containing the outputs to be transferred
     * @param destination The json destination for the transferred outputs
     */
    private void transferOutputs(JsonValue source, JsonValue destination) {
        if (config.outputs().contains(WILDCARD)) {
            source.keys().forEach(key -> destination.put(key, source.get(key)));
        } else {
            config.outputs().forEach(output -> {
                if (source.isDefined(output)) {
                    destination.put(output, source.get(output));
                }
            });
        }
    }

    /**
     * Node configuration.
     */
    public interface Config {

        /**
         * The script configuration.
         *
         * @return The script configuration.
         */
        @Attribute(order = 100)
        @ScriptContext(legacyContext = AUTHENTICATION_TREE_DECISION_NODE_NAME,
                nextGenContext = SCRIPTED_DECISION_NODE_NAME)
        default Script script() {
            return Script.EMPTY_SCRIPT;
        }

        /**
         * The list of possible outcomes.
         *
         * @return THe possible outcomes.
         */
        @Attribute(order = 200)
        List<String> outcomes();

        /**
         * List of required inputs.
         *
         * @return The list of state inputs required by the script.
         */
        @Attribute(order = 300)
        default List<String> inputs() {
            return singletonList(WILDCARD);
        }

        /**
         * List of outputs produced by the script.
         *
         * @return A list of state outputs produced by the script.
         */
        @Attribute(order = 400)
        default List<String> outputs() {
            return singletonList(WILDCARD);
        }
    }

    /**
     * Provides the outcomes for the scripted decision node.
     */
    public static class ScriptedDecisionOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            try {
                return nodeAttributes.get("outcomes").required()
                        .asList(String.class)
                        .stream()
                        .map(outcome -> new Outcome(outcome, outcome))
                        .collect(Collectors.toList());
            } catch (JsonValueException e) {
                return emptyList();
            }
        }
    }
}
