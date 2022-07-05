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
 * Copyright 2021-2022 ForgeRock AS.
 */

package org.forgerock.openam.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.plugins.ScopeEvaluator;

/**
 * Custom implementation of the Scope Evaluator
 * plugin interface {@link org.forgerock.oauth2.core.plugins.ScopeEvaluator}
 *
 * <li>
 * The {@code evaluateScope} method populates scope values to return.
 * </li>
 *
 */
public class CustomScopeEvaluator implements ScopeEvaluator {

    @Override
    public Map<String, Object> evaluateScope(AccessToken token) {
        return mapScopes(token);
    }

    /**
     * Set read and write permissions according to scope.
     *
     * @param token The access token presented for validation.
     * @return The map of read and write permissions,
     *         with permissions set to {@code true} or {@code false},
     *         as appropriate.
     */
    private Map<String, Object> mapScopes(AccessToken token) {
        Set<String> scopes = token.getScope();
        Map<String, Object> map = new HashMap<String, Object>();
        final String[] permissions = {"read", "write"};

        for (String scope : permissions) {
            if (scopes.contains(scope)) {
                map.put(scope, true);
            } else {
                map.put(scope, false);
            }
        }
        return map;
    }
}
