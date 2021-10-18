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
 * Copyright 2017-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.scripting.ScriptConstants.AUTHENTICATION_TREE_DECISION_NODE_NAME;
import static org.forgerock.openam.scripting.ScriptContext.AUTHENTICATION_TREE_DECISION_NODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.Script;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.factories.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;

/**
 * A node that executes a script to make a decision.
 *
 * <p>The script is passed the shared state and must set an outcome as a boolean.</p>
 */
@Node.Metadata(outcomeProvider = ScriptedDecisionNode.ScriptedDecisionOutcomeProvider.class,
        configClass = ScriptedDecisionNode.Config.class)
public class ScriptedDecisionNode implements Node {

    private static final String HEADERS_IDENTIFIER = "requestHeaders";
    private static final String EXISTING_SESSION = "existingSession";

    private static final String SHARED_STATE_IDENTIFIER = "sharedState";
    private static final String SHARED_STATE_CRYPTO = "sharedStateCrypto";
    private static final String TRANSIENT_STATE_IDENTIFIER = "transientState";
    private static final String OUTCOME_IDENTIFIER = "outcome";
    private static final String ACTION_IDENTIFIER = "action";
    private static final String HTTP_CLIENT_IDENTIFIER = "httpClient";
    private static final String LOGGER_VARIABLE_NAME = "logger";
    private static final String REALM_IDENTIFIER = "realm";
    private static final String CALLBACKS_IDENTIFIER = "callbacks";
    private static final String QUERY_PARAMETER_IDENTIFIER = "requestParameters";
    private static final String ID_REPO_IDENTIFIER = "idRepository";
    private static final String AUDIT_ENTRY_DETAIL = "auditEntryDetail";

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
        @Script(AUTHENTICATION_TREE_DECISION_NODE_NAME)
        ScriptConfiguration script();

        /**
         * The list of possible outcomes.
         *
         * @return THe possible outcomes.
         */
        @Attribute(order = 200)
        List<String> outcomes();
    }

    private final Logger logger = LoggerFactory.getLogger(ScriptedDecisionNode.class);
    private final Config config;
    private final ScriptEvaluator scriptEvaluator;
    private final Provider<SessionService> sessionServiceProvider;
    private final RestletHttpClient httpClient;
    private final Realm realm;
    private final ScriptIdentityRepository scriptIdentityRepo;
    private final NodeSharedStateCrypto crypto;
    private JsonValue auditEntryDetail;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluator A script evaluator.
     * @param config The node configuration.
     * @param sessionServiceProvider provides Sessions.
     * @param httpClientFactory provides http clients.
     * @param realm The realm the node is in, and that the request is targeting.
     * @param scriptIdentityRepoFactory factory to build access to the identity repo for this node's script
     * @param crypto the {@link NodeSharedStateCrypto} ... // FIXME
     */
    @Inject
    public ScriptedDecisionNode(@Named(AUTHENTICATION_TREE_DECISION_NODE_NAME) ScriptEvaluator scriptEvaluator,
            @Assisted Config config, Provider<SessionService> sessionServiceProvider,
            ScriptHttpClientFactory httpClientFactory, @Assisted Realm realm,
            ScriptIdentityRepository.Factory scriptIdentityRepoFactory, NodeSharedStateCrypto crypto) {
        this.scriptEvaluator = scriptEvaluator;
        this.config = config;
        this.sessionServiceProvider = sessionServiceProvider;
        this.httpClient = getHttpClient(httpClientFactory);
        this.realm = realm;
        this.scriptIdentityRepo = scriptIdentityRepoFactory.create(realm);
        this.crypto = crypto;
        this.auditEntryDetail = null;
    }

    private RestletHttpClient getHttpClient(ScriptHttpClientFactory httpClientFactory) {
        SupportedScriptingLanguage scriptType = config.script().getLanguage();

        if (scriptType == null) {
            return null;
        }

        return httpClientFactory.getScriptHttpClient(scriptType);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ScriptedDecisionNode started");
        try {
            ScriptObject script = new ScriptObject(config.script().getName(), config.script().getScript(),
                    config.script().getLanguage());
            Bindings binding = new SimpleBindings();
            binding.put(SHARED_STATE_IDENTIFIER, context.sharedState.getObject());
            binding.put(SHARED_STATE_CRYPTO, crypto);
            binding.put(TRANSIENT_STATE_IDENTIFIER, context.transientState.getObject());
            binding.put(CALLBACKS_IDENTIFIER, context.getAllCallbacks());
            binding.put(ID_REPO_IDENTIFIER, scriptIdentityRepo);
            binding.put(HEADERS_IDENTIFIER, convertHeadersToModifiableObjects(context.request.headers));
            binding.put(LOGGER_VARIABLE_NAME, Debug.getInstance("scripts." + AUTHENTICATION_TREE_DECISION_NODE.name()
                    + "." + config.script().getId()));
            binding.put(HTTP_CLIENT_IDENTIFIER, httpClient);
            binding.put(REALM_IDENTIFIER, realm.asPath());
            if (!StringUtils.isEmpty(context.request.ssoTokenId)) {
                binding.put(EXISTING_SESSION, getSessionProperties(context.request.ssoTokenId));
            }
            binding.put(QUERY_PARAMETER_IDENTIFIER, convertParametersToModifiableObjects(context.request.parameters));
            binding.put(AUDIT_ENTRY_DETAIL, auditEntryDetail);
            scriptEvaluator.evaluateScript(script, binding);
            logger.debug("script {} \n binding {}", script, binding);
            Object rawAuditEntryDetail = binding.get(AUDIT_ENTRY_DETAIL);
            if (rawAuditEntryDetail != null) {
                if (rawAuditEntryDetail instanceof String || rawAuditEntryDetail instanceof Map) {
                    auditEntryDetail = json(object(field("auditInfo", rawAuditEntryDetail)));
                } else {
                    logger.warn("script auditEntryDetail not type String or Map");
                    throw new NodeProcessException("Invalid auditEntryDetail type from script");
                }
            }
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
     * The request headers are unmodifiable, this prevents them being converted into javascript. This method
     * iterates the underlying collections, adding the values to modifiable collections.
     *
     * @param input the headers.
     * @return the headers in modifiable collections.
     */
    private Map<String, List<String>> convertHeadersToModifiableObjects(ListMultimap<String, String> input) {
        Map<String, List<String>> mapCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : input.keySet()) {
            mapCopy.put(key, new ArrayList<>(input.get(key)));
        }
        return mapCopy;
    }

    /**
     * The request parameters are unmodifiable, this prevents them being converted into javascript. This method
     * copies unmofifiable to modifiable collections.
     *
     * @param input the parameters.
     * @return the parameters in modifiable collections.
     */
    private Map<String, List<String>> convertParametersToModifiableObjects(Map<String, List<String>> input) {
        Map<String, List<String>> mapCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            mapCopy.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }
        return mapCopy;
    }

    private Map<String, String> getSessionProperties(String ssoTokenId) {
        Map<String, String> properties = null;
        try {
            Session session = sessionServiceProvider.get().getSession(new SessionID(ssoTokenId));
            if (session != null) {
                properties = new HashMap<>(session.getProperties());
            }
        } catch (SessionException e) {
            logger.error("Failed to get existing session", e);
        }
        return properties;
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        if (auditEntryDetail != null) {
            return auditEntryDetail;
        } else {
            return json(object());
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
