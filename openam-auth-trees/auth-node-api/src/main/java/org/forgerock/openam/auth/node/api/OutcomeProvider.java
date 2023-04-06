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
 * Copyright 2017-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Describes the outcomes for node instances.
 *
 */
@SupportedAll
public interface OutcomeProvider {

    /**
     * A model object for an outcome.
     */
    final class Outcome {
        /** The outcome id. */
        public final String id;
        /** The outcome display name. */
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
