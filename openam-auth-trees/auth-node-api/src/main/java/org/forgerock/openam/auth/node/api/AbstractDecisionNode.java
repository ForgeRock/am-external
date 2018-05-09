/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import java.util.List;
import java.util.ResourceBundle;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * An abstract node implementation for nodes that result in a simple true-false outcome.
 *
 * @supported.all.api
 */
public abstract class AbstractDecisionNode implements Node {

    /**
     * true tree outcome.
     */
    public final static String TRUE_OUTCOME_ID = "true";

    /**
     * false tree outcome.
     */
    public final static String FALSE_OUTCOME_ID = "false";

    /**
     * Move on to the next node in the tree that is connected to the given outcome.
     * @param outcome the outcome.
     * @return an action builder to provide additional details.
     */
    protected Action.ActionBuilder goTo(boolean outcome) {
        return Action.goTo(outcome ? TRUE_OUTCOME_ID : FALSE_OUTCOME_ID);
    }

    /**
     * Provides a static set of outcomes for decision nodes.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale("amAuthTrees",
                    OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString("trueOutcome")),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString("falseOutcome")));
        }
    }
}
