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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.TARGET_AUTH_LEVEL;
import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;
import static org.forgerock.util.LambdaExceptionUtils.rethrowSupplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.security.auth.callback.ChoiceCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeMetaData;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that represents choices to the user and collects the chosen one.
 *
 * <p>Choices are configured as a set of values and a default choice can also be configured.
 * Multiple selection of choices are not supported</p>
 */
@Node.Metadata(outcomeProvider = ChoiceCollectorNode.ChoiceCollectorOutcomeProvider.class,
        configClass = ChoiceCollectorNode.Config.class)
public class ChoiceCollectorNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * An ordered list of choices to be displayed to the user.
         * @return The choices.
         */
        @Attribute(order = 100)
        List<String> choices();

        /**
         * A choice that is selected by default when presented to the user.
         * @return The default choice.
         */
        @Attribute(order = 200)
        String defaultChoice();

        /**
         * Some text to display to the user when displaying the choices.
         * @return some text.
         */
        @Attribute(order = 300)
        String prompt();
    }

    private final Config config;
    private final TreeMetaData metaData;
    private final UUID nodeId;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     *
     * @param config The service config for the node.
     * @param metaData Meta data pertaining to the node and authentication process in progress.
     * @param nodeId The node's ID.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ChoiceCollectorNode(@Assisted Config config, @Assisted TreeMetaData metaData, @Assisted UUID nodeId)
            throws NodeProcessException {
        this.nodeId = nodeId;
        this.config = config;
        this.metaData = metaData;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ChoiceCollectorNode started");
        return context.getCallback(ChoiceCallback.class)
                .map(ChoiceCallback::getSelectedIndexes)
                .filter(indexes -> indexes.length > 0)
                .map(choiceIndexToChoices(context))
                .map(choice -> goTo(choice).build())
                .orElseGet(rethrowSupplier(() -> evaluateChoices(context)));
    }

    private Function<int[], String> choiceIndexToChoices(TreeContext context) throws NodeProcessException {
        return rethrowFunction(indexes -> getChoicesForCurrentState(context.sharedState).get(indexes[0]));
    }

    private Action evaluateChoices(TreeContext context) throws NodeProcessException {
        logger.debug("evaluating choices");
        List<String> choices = getChoicesForCurrentState(context.sharedState);
        switch (choices.size()) {
        case 0:
            String targetAuthLevelErrorMessage = "Node doesn't have outcomes which can give auth level >= "
                    + context.sharedState.get("targetAuthLevel").asInteger();
            logger.debug(targetAuthLevelErrorMessage);
            throw new NodeProcessException(targetAuthLevelErrorMessage);
        case 1:
            logger.debug("One choice is available no UI interaction needed");
            return goTo(choices.get(0)).build();
        default:
            logger.debug("Multiple choices");
            int defaultChoice = choices.indexOf(config.defaultChoice());
            logger.debug("default choice {}", defaultChoice);
            ChoiceCallback choiceCallback = new ChoiceCallback(config.prompt(),
                    choices.toArray(new String[choices.size()]), defaultChoice < 0 ? 0 : defaultChoice, false);
            choiceCallback.setSelectedIndex(defaultChoice < 0 ? 0 : defaultChoice);
            logger.debug("Choice callback sent to UI");
            return send(Collections.singletonList(choiceCallback)).build();
        }
    }

    private List<String> getChoicesForCurrentState(JsonValue sharedState) throws NodeProcessException {
        return sharedState.isDefined(TARGET_AUTH_LEVEL) ? filterByAuthLevel(sharedState) : config.choices();
    }

    private List<String> filterByAuthLevel(JsonValue sharedState) throws NodeProcessException {
        logger.debug("filtering choices that lead to path in the tree where the target auth level is not reachable");
        int currentLevel = sharedState.get(AUTH_LEVEL).asInteger();
        int minimumLevel = sharedState.get(TARGET_AUTH_LEVEL).asInteger();
        logger.debug("currentLevel {} ; minimumLevel {}", currentLevel, minimumLevel);
        List<String> filteredChoices = new ArrayList<>();
        for (String choice : config.choices()) {
            Optional<Integer> maxAuthLevel = metaData.getMaxAuthLevel(nodeId, choice);
            if (maxAuthLevel.isPresent() && maxAuthLevel.get() + currentLevel >= minimumLevel) {
                filteredChoices.add(choice);
            }
        }
        logger.debug("filteredChoices {}", filteredChoices);
        return filteredChoices;
    }

    /**
     * Provides the outcomes for the choice collector node.
     */
    public static class ChoiceCollectorOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            try {
                return nodeAttributes.get("choices").required()
                        .asList(String.class)
                        .stream()
                        .map(choice -> new Outcome(choice, choice))
                        .collect(Collectors.toList());
            } catch (JsonValueException e) {
                return emptyList();
            }
        }
    }
}
