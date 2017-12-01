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
/**
 * jon.knight@forgerock.com
 *
 * A node that returns true if the user's email address is recorded as breached by the HaveIBeenPwned website (http://haveibeenpwned.com)
 * or false if no breach has been recorded
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;



 /**
 * A node that represents choices to the user and collects the chosen one.
 *
 * <p>Choices are configured as a set of values and a default choice can also be configured.
 * Multiple selection of choices are not supported</p>
 */
@Node.Metadata(outcomeProvider = RandomChoiceNode.ChoiceCollectorOutcomeProvider.class,
        configClass = RandomChoiceNode.Config.class)
public class RandomChoiceNode implements Node {

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
    }

    private final Config config;
    private final static String DEBUG_FILE = "RandomChoiceNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);


    /**
     * Guice constructor.
     *
     * @param config The service config for the node.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public RandomChoiceNode(@Assisted Config config)
            throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        // Calculate sum of weighted score over all choices
        int totalWeighted = 0;
        for (String temp : config.choices()) {
            int i = temp.lastIndexOf(':');
            if (i == -1) totalWeighted += 100;
            else totalWeighted += Integer.parseInt(temp.substring(i+1));
        }

        // Choose random scope within total weighted range
        int random = (int) (Math.random() * totalWeighted);

        // Find weighted choice
        int total = 0;
        String randomChoice = "";
        for (String temp : config.choices()) {
            int score = 0;
            int i = temp.lastIndexOf(':');
            if (i == -1) score += 100;
            else score += Integer.parseInt(temp.substring(i+1));
            if ((random >= total) && (random < (total+score))) { randomChoice = temp; break; }
            else total += score;
        }

        debug.message("[" + DEBUG_FILE + "]: Random choice: " + randomChoice);

        return Action.goTo(randomChoice).build();
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

