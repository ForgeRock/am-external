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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Provides outcomes for nodes that have a variable set of predefined outcomes.
 * <p>
 * The outcomes returned by {@link #getOutcomes(PreferredLocales, JsonValue)} must always be a
 * subset of the configuration-agnostic list of predefined outcomes provided by
 * {@link #getAllOutcomes(PreferredLocales)}.
 */
@Supported
public interface BoundedOutcomeProvider extends OutcomeProvider {

    /**
     * Returns a list of all possible outcomes for the node.
     * @param locales The locales for the localised description.
     * @return A non-null list of outcomes. May be empty.
     * @throws NodeProcessException If outcomes could not be determined.
     */
    @Supported
    List<Outcome> getAllOutcomes(PreferredLocales locales) throws NodeProcessException;
}
