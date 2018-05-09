/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Describes the outcomes for node instances.
 *
 * @supported.all.api
 */
public interface OutcomeProvider {

    /**
     * A model object for an outcome.
     */
    final class Outcome {
        public final String id;
        public final String displayName;

        public Outcome(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    /**
     * Returns a ordered list of possible node outcomes with localised display names.
     *
     * @param locales The locales for the localised description.
     * @param nodeAttributes Unvalidated node attributes submitted in the request.
     * @return A non-null list of outcomes. May be empty.
     * @throws NodeProcessException If outcomes could not be determined.
     */
    List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) throws NodeProcessException;
}
