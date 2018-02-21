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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.scripting.ScriptConstants.AUTHENTICATION_TREE_DECISION_NODE_NAME;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_TREE_DECISION_NODE;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.script.Bindings;
import javax.script.SimpleBindings;

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
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A node that executes a script to make a decision.
 *
 * <p>The script is passed the shared state and must set an outcome as a boolean.</p>
 */
@Node.Metadata(outcomeProvider = ScriptedDecisionNode.ScriptedDecisionOutcomeProvider.class,
        configClass = ScriptedDecisionNode.Config.class)
public class ScriptedDecisionNode implements Node {

    interface Config {
        @Attribute(order = 100)
        @Script(AUTHENTICATION_TREE_DECISION_NODE)
        ScriptConfiguration script();

        @Attribute(order = 200)
        List<String> outcomes();
    }

    private static final String SHARED_STATE_IDENTIFIER = "sharedState";
    private static final String OUTCOME_IDENTIFIER = "outcome";

    private final Config config;
    private final ScriptEvaluator scriptEvaluator;

    /**
     * Guice constructor.
     *
     * @param scriptEvaluator A script evaluator.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ScriptedDecisionNode(@Named(AUTHENTICATION_TREE_DECISION_NODE_NAME) ScriptEvaluator scriptEvaluator,
            @Assisted Config config) throws NodeProcessException {
        this.scriptEvaluator = scriptEvaluator;
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {
            Bindings binding = new SimpleBindings();
            binding.put(SHARED_STATE_IDENTIFIER, context.sharedState.getObject());
            ScriptObject script = new ScriptObject(config.script().getName(), config.script().getScript(),
                    config.script().getLanguage());
            scriptEvaluator.evaluateScript(script, binding);

            Object rawResult = binding.get(OUTCOME_IDENTIFIER);
            if (rawResult == null || !(rawResult instanceof String)) {
                throw new NodeProcessException("Script must set '" + OUTCOME_IDENTIFIER + "' to a string.");
            }
            String outcome = (String) rawResult;
            if (!config.outcomes().contains(outcome)) {
                throw new NodeProcessException("Invalid outcome from script, '" + outcome + "'");
            }

            return goTo(outcome).build();
        } catch (javax.script.ScriptException e) {
            throw new NodeProcessException(e);
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
