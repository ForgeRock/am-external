/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import static org.forgerock.openam.auth.node.api.Action.goTo;

import java.util.List;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Abstract node for nodes that always result in the same single outcome.
 *
 * @supported.all.api
 */
public abstract class SingleOutcomeNode implements Node {
    private final static String OUTCOME_ID = "outcome";

    /**
     * Move on to the next node in the tree.
     * @return an action builder to provide additional details.
     */
    protected Action.ActionBuilder goToNext() {
        return goTo(OUTCOME_ID);
    }

    /**
     * Provides a static single outcome for nodes with a single outcome.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return ImmutableList.of(new Outcome(OUTCOME_ID,
                    locales.getBundleInPreferredLocale("amAuthTrees",
                            OutcomeProvider.class.getClassLoader()).getString("singleOutcome")));
        }
    }
}
