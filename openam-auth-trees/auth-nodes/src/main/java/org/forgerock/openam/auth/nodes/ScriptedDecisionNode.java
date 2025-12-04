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
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ACTION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_BUILDER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.OUTCOME_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.WILDCARD;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE_NAME;
import static org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext.SCRIPTED_DECISION_NODE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.script.Bindings;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeBindings;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.secrets.IScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptFeature;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A node that executes a script to make a decision.
 *
 * <p>The script is passed the shared state and must set an outcome as a boolean.</p>
 */
@Node.Metadata(outcomeProvider = ScriptedDecisionNode.ScriptedDecisionOutcomeProvider.class,
        configClass = ScriptedDecisionNode.Config.class,
        tags = {"utilities"})
public class ScriptedDecisionNode implements Node {

    private final Logger logger = LoggerFactory.getLogger(ScriptedDecisionNode.class);
    private final Config config;
    private final ScriptEvaluator<ScriptedDecisionNodeBindings> scriptEvaluator;
    private final ChfHttpClient httpClient;
    private final Realm realm;
    private final ScriptIdentityRepository scriptIdentityRepository;
    private final ScriptedIdentityRepository scriptedIdentityRepository;
    private final IScriptedSecrets secrets;
    private final AuthScriptUtilities authScriptUtils;
    private JsonValue auditEntryDetail;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluatorFactory            A script evaluator factory.
     * @param config                            The node configuration.
     * @param realm                             The realm the node is in, and that the request is targeting.
     * @param scriptIdentityRepositoryFactory   factory to build access to the identity repo for this node's script
     * @param scriptedIdentityRepositoryFactory factory to build access to the identity repo for this node's script
     * @param secretsFactory                    provides access to the secrets API for this node's script
     * @param authScriptUtils                   utilities for scripted nodes
     * @param scriptedDecisionNodeContext       The next-gen context for the scripted decision node.
     */
    @Inject
    public ScriptedDecisionNode(ScriptEvaluatorFactory scriptEvaluatorFactory,
            @Assisted Config config, @Assisted Realm realm,
            ScriptIdentityRepository.Factory scriptIdentityRepositoryFactory,
            ScriptedIdentityRepository.Factory scriptedIdentityRepositoryFactory,
            IScriptedSecrets.Factory secretsFactory, AuthScriptUtilities authScriptUtils,
            ScriptedDecisionNodeContext scriptedDecisionNodeContext) {
        this.scriptEvaluator = scriptEvaluatorFactory
                .create(AUTHENTICATION_TREE_DECISION_NODE, scriptedDecisionNodeContext);
        this.config = config;
        this.authScriptUtils = authScriptUtils;
        this.httpClient = authScriptUtils.getLegacyHttpClient(config.script());
        this.realm = realm;
        this.scriptIdentityRepository = scriptIdentityRepositoryFactory.create(realm);
        this.scriptedIdentityRepository = scriptedIdentityRepositoryFactory.create(realm);
        this.secrets = secretsFactory.create(realm, singleton("scripted.node."));
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ScriptedDecisionNode started");
        try {
            Script script = config.script();
            EvaluatorVersion evaluatorVersion = script.getEvaluatorVersion();

            JsonValue filteredShared = json(null);
            JsonValue filteredTransient = json(null);
            if (evaluatorVersion.hasFeature(ScriptFeature.DIRECT_STATE_ACCESS)) {
                filteredShared = filterInputs(context.sharedState);
                filteredTransient = filterInputs(context.transientState);
            }

            ScriptedDecisionNodeBindings scriptedDecisionNodeBindings = ScriptedDecisionNodeBindings.builder()
                    .withSharedState(filteredShared.getObject())
                    .withTransientState(filteredTransient.getObject())
                    .withHttpClient(httpClient)
                    .withSecrets(secrets)
                    .withScriptIdentityRepository(scriptIdentityRepository)
                    .withSamlApplication(authScriptUtils.getSamlDecisionNodeApplication(context).orElse(null))
                    .withOauthApplication(authScriptUtils.getOauthDecisionNodeApplication(context).orElse(null))
                    .withNodeState(context.getStateFor(this))
                    .withCallbacks(context.getAllCallbacks())
                    .withHeaders(authScriptUtils.convertHeadersToModifiableObjects(context.request.headers))
                    .withQueryParameters(authScriptUtils.convertParametersToModifiableObjects(
                            context.request.parameters))
                    .withScriptedIdentityRepository(scriptedIdentityRepository)
                    .withResumedFromSuspend(context.hasResumedFromSuspend())
                    .withExistingSession(authScriptUtils.getSessionProperties(context.request.ssoTokenId))
                    .withRequestCookies(context.request.cookies)
                    .build();

            ScriptResult<Object> scriptResult = scriptEvaluator.evaluateScript(script,
                    scriptedDecisionNodeBindings, realm);
            Bindings bindings = scriptResult.getBindings();
            logger.debug("script {} \n binding {}", script, bindings);
            if (evaluatorVersion.hasFeature(ScriptFeature.DIRECT_STATE_ACCESS)) {
                if (!allOutputsArePresent(filteredShared, filteredTransient)) {
                    throw new NodeProcessException("Script did not provide all declared outputs");
                }
                checkForUnexpectedOutputs(filteredShared, filteredTransient);
                transferOutputs(filteredShared, context.sharedState);
                transferOutputs(filteredTransient, context.transientState);
            }
            auditEntryDetail = authScriptUtils.getAuditEntryDetails(bindings);
            Object actionResult = bindings.get(ACTION);
            Object callbacksBuilder = bindings.get(CALLBACKS_BUILDER);
            Optional<Action> actionOptional = authScriptUtils.getAction(actionResult, evaluatorVersion,
                    callbacksBuilder);
            if (actionOptional.isPresent()) {
                Action action = actionOptional.get();
                if (!action.sendingCallbacks() && !action.hasSuspensionHandler()
                        && !config.outcomes().contains(action.outcome)) {
                    logger.warn("invalid script outcome {} in action", action.outcome);
                    throw new NodeProcessException("Invalid outcome from script, '" + action.outcome + "'");
                }
                return action;
            }
            return goTo(authScriptUtils.getOutcome(bindings.get(OUTCOME_IDENTIFIER), config.outcomes())).build();
        } catch (javax.script.ScriptException e) {
            logger.warn("error evaluating the script", e);
            throw new NodeProcessException(e);
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        if (auditEntryDetail != null) {
            return auditEntryDetail;
        } else {
            return json(object());
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
