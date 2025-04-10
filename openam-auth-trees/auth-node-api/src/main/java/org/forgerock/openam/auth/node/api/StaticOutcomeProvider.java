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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.node.api;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Describes the outcomes for node instances that have <b>static</b> outcomes.
 *
 */
@SupportedAll
public interface StaticOutcomeProvider extends OutcomeProvider {

    @Override
    default List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) throws NodeProcessException {
        return getOutcomes(locales);
    }

    /**
     * Returns a ordered list of possible node outcomes with localised display names.
     *
     * @param locales The locales for the localised description.
     * @return A non-null list of outcomes. May be empty.
     */
    List<Outcome> getOutcomes(PreferredLocales locales);
}
