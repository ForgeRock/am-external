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
package org.forgerock.openam.auth.node.api;

import java.util.List;
import java.util.ResourceBundle;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;

/**
 * An abstract node implementation for nodes that result in a simple true-false outcome.
 *
 */
@SupportedAll
public abstract class AbstractDecisionNode implements Node {

    /**
     * true tree outcome.
     */
    public static final String TRUE_OUTCOME_ID = "true";

    /**
     * false tree outcome.
     */
    public static final String FALSE_OUTCOME_ID = "false";

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
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale("amAuthTrees",
                    OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString("trueOutcome")),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString("falseOutcome")));
        }
    }
}
