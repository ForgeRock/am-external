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
