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
 * Copyright 2017-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.WILDCARD;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.convertHeadersToModifiableObjects;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.convertParametersToModifiableObjects;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.getAuditEntryDetails;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.getHttpClient;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.getSessionProperties;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Provider;
import javax.script.Bindings;

import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeBindings;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.api.secrets.ScriptedSecrets;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.service.SessionService;

/**
 * A node that executes a script to make a decision.
 *
 * <p>The script is passed the shared state and must set an outcome as a boolean.</p>
 */
@Node.Metadata(outcomeProvider = ScriptedDecisionNode.ScriptedDecisionOutcomeProvider.class,
        configClass = ScriptedDecisionNode.Config.class,
        tags = {"utilities"})
public class ScriptedDecisionNode implements Node {

    private static final String OUTCOME_IDENTIFIER = "outcome";
    private static final String ACTION_IDENTIFIER = "action";
    private final Logger logger = LoggerFactory.getLogger(ScriptedDecisionNode.class);
    private final Config config;
    private final ScriptEvaluator scriptEvaluator;
    private final Provider<SessionService> sessionServiceProvider;
    private final ChfHttpClient httpClient;
    private final Realm realm;
    private final ScriptIdentityRepository scriptIdentityRepo;
    private final ScriptedSecrets secrets;
    private JsonValue auditEntryDetail;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluatorFactory    A script evaluator factory.
     * @param config                    The node configuration.
     * @param sessionServiceProvider    provides Sessions.
     * @param httpClientFactory         provides http clients.
     * @param realm                     The realm the node is in, and that the request is targeting.
     * @param scriptIdentityRepoFactory factory to build access to the identity repo for this node's script
     * @param secretsFactory            provides access to the secrets API for this node's script
     */
    @Inject
    public ScriptedDecisionNode(ScriptEvaluatorFactory scriptEvaluatorFactory,
            @Assisted Config config, Provider<SessionService> sessionServiceProvider,
            ScriptHttpClientFactory httpClientFactory, @Assisted Realm realm,
            ScriptIdentityRepository.Factory scriptIdentityRepoFactory,
            ScriptedSecrets.Factory secretsFactory) {
        this.scriptEvaluator = scriptEvaluatorFactory.create(AUTHENTICATION_TREE_DECISION_NODE);
        this.config = config;
        this.sessionServiceProvider = sessionServiceProvider;
        this.httpClient = getHttpClient(config.script(), httpClientFactory);
        this.realm = realm;
        this.scriptIdentityRepo = scriptIdentityRepoFactory.create(realm);
        this.auditEntryDetail = null;
        this.secrets = secretsFactory.create(realm, singleton("scripted.node."));
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ScriptedDecisionNode started");
        try {
            JsonValue filteredShared = filterInputs(context.sharedState);
            JsonValue filteredTransient = filterInputs(context.transientState);

            Script script = config.script();

            ScriptBindings scriptedDecisionNodeBindings = ScriptedDecisionNodeBindings.builder()
                    .withNodeState(context.getStateFor(this))
                    .withCallbacks(context.getAllCallbacks())
                    .withHeaders(convertHeadersToModifiableObjects(context.request.headers))
                    .withRealm(realm.asPath())
                    .withQueryParameters(convertParametersToModifiableObjects(context.request.parameters))
                    .withHttpClient(httpClient)
                    .withIdentityRepository(scriptIdentityRepo)
                    .withSecrets(secrets)
                    .withAuditEntryDetail(auditEntryDetail)
                    .withResumedFromSuspend(context.hasResumedFromSuspend())
                    .withExistingSession(
                            StringUtils.isNotEmpty(context.request.ssoTokenId)
                                    ? getSessionProperties(sessionServiceProvider.get(), context.request.ssoTokenId)
                                    : null)
                    .withSharedState(filteredShared.getObject())
                    .withTransientState(filteredTransient.getObject())
                    .withLoggerReference(String.format("%s (%s)", script.getName(), script.getId()))
                    .withScriptName(script.getName())
                    .build();

            Bindings binding = scriptedDecisionNodeBindings.convert(script.getEvaluatorVersion());
            scriptEvaluator.evaluateScript(script, binding, realm);
            logger.debug("script {} \n binding {}", script, binding);
            if (!allOutputsArePresent(filteredShared, filteredTransient)) {
                throw new NodeProcessException("Script did not provide all declared outputs");
            }
            checkForUnexpectedOutputs(filteredShared, filteredTransient);
            transferOutputs(filteredShared, context.sharedState);
            transferOutputs(filteredTransient, context.transientState);
            auditEntryDetail = getAuditEntryDetails(binding);
            Object actionResult = binding.get(ACTION_IDENTIFIER);
            if (actionResult != null) {
                if (actionResult instanceof Action) {
                    Action action = (Action) actionResult;
                    if (!action.sendingCallbacks() && !config.outcomes().contains(action.outcome)) {
                        logger.warn("invalid script outcome {} in action", action.outcome);
                        throw new NodeProcessException("Invalid outcome from script, '" + action.outcome + "'");
                    }
                    return action;
                } else {
                    logger.warn("Found an action result from scripted node, but it was not an Action object");
                }
            }
            Object rawResult = binding.get(OUTCOME_IDENTIFIER);
            if (rawResult == null || !(rawResult instanceof String)) {
                logger.warn("script outcome error");
                throw new NodeProcessException("Script must set '" + OUTCOME_IDENTIFIER + "' to a string.");
            }
            String outcome = (String) rawResult;
            if (!config.outcomes().contains(outcome)) {
                logger.warn("invalid script outcome {}", outcome);
                throw new NodeProcessException("Invalid outcome from script, '" + outcome + "'");
            }

            return goTo(outcome).build();
        } catch (javax.script.ScriptException e) {
            logger.warn("error evaluating the script", e);
            throw new NodeProcessException(e);
        }
    }

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

    /**
     * Node Config Declaration.
     */
    public interface Config {
        /**
         * The script configuration.
         *
         * @return The script configuration.
         */
        @Attribute(order = 100)
        @ScriptContext(AUTHENTICATION_TREE_DECISION_NODE_NAME)
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
