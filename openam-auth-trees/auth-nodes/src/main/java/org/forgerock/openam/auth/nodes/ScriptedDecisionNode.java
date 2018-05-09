/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.scripting.ScriptConstants.AUTHENTICATION_TREE_DECISION_NODE_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.forgerock.openam.scripting.Script;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.factories.ScriptHttpClientFactory;
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

    /** Debug logger instance used by scripts to log error/debug messages. */
    private static final Debug DEBUG = Debug.getInstance("amScript");
    private static final String LOGGER_VARIABLE_NAME = "logger";

    interface Config {
        @Attribute(order = 100)
        @Script(AUTHENTICATION_TREE_DECISION_NODE_NAME)
        ScriptConfiguration script();

        @Attribute(order = 200)
        List<String> outcomes();
    }

    private static final String SHARED_STATE_IDENTIFIER = "sharedState";
    private static final String OUTCOME_IDENTIFIER = "outcome";
    private static final String HTTP_CLIENT_IDENTIFIER = "httpClient";

    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final Config config;
    private final ScriptEvaluator scriptEvaluator;
    private final Provider<SessionService> sessionServiceProvider;
    private final RestletHttpClient httpClient;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluator A script evaluator.
     * @param config The node configuration.
     * @param sessionServiceProvider provides Sessions.
     * @param httpClientFactory provides http clients.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ScriptedDecisionNode(@Named(AUTHENTICATION_TREE_DECISION_NODE_NAME) ScriptEvaluator scriptEvaluator,
            @Assisted Config config, Provider<SessionService> sessionServiceProvider,
            ScriptHttpClientFactory httpClientFactory) throws NodeProcessException {
        this.scriptEvaluator = scriptEvaluator;
        this.config = config;
        this.sessionServiceProvider = sessionServiceProvider;
        this.httpClient = getHttpClient(httpClientFactory);
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
            binding.put(HEADERS_IDENTIFIER, convertHeadersToModifiableObjects(context.request.headers));
            binding.put(LOGGER_VARIABLE_NAME, DEBUG);
            binding.put(HTTP_CLIENT_IDENTIFIER, httpClient);
            if (!StringUtils.isEmpty(context.request.ssoTokenId)) {
                binding.put(EXISTING_SESSION, getSessionProperties(context.request.ssoTokenId));
            }
            scriptEvaluator.evaluateScript(script, binding);
            logger.debug("script {} \n binding {}", script, binding);

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
        Map<String, List<String>> mapCopy = new HashMap<>();
        for (String key : input.keySet()) {
            mapCopy.put(key, new ArrayList(input.get(key)));
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
