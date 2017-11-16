/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.examples;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.openam.oauth2.legacy.CoreToken;
import org.forgerock.openam.oauth2.provider.Scope;

/**
 * Custom scope providers implement the
 * {@link org.forgerock.openam.oauth2.provider.Scope} interface.
 *
 * This custom scope implementation does not support OpenID Connect 1.0,
 * and implements the default mechanisms for the three {@code scope*} methods.
 *
 * This custom scope plugin instead implements simple versions
 * of {@link #evaluateScope(org.forgerock.openam.oauth2.legacy.CoreToken)}
 * and of {@link #getUserInfo(org.forgerock.openam.oauth2.legacy.CoreToken)}.
 *
 * The {@code evaluateScope} method populates scope values returned in the
 * JSON with the token information.
 *
 * The {@code getUserInfo} method populates scope values and the user ID
 * returned in the JSON.
 */
public class DeprecatedCustomScope implements Scope {

    private Map<String,Object> mapScopes(CoreToken token) {
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

    @Override
    public Map<String, Object> evaluateScope(CoreToken token) {
        return mapScopes(token);
    }

    @Override
    public Map<String, String> extraDataToReturnForAuthorizeEndpoint(
            Map parameters,
            Map tokens) {
        return new HashMap<String, String>(); // No special handling
    }

    @Override
    public Map<String, Object> extraDataToReturnForTokenEndpoint(
            Map parameters,
            CoreToken token) {
        return new HashMap<String, Object>(); // No special handling
    }

    @Override
    public UserInfoClaims getUserInfo(CoreToken token) {
        Map<String, Object> response = mapScopes(token);
        response.put("sub", token.getUserID());
        return new UserInfoClaims(response, null);
    }

    @Override
    public Set<String> scopeToPresentOnAuthorizationPage(
            Set<String> requestedScope,
            Set<String> availableScopes,
            Set<String> defaultScopes) {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return defaultScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    @Override
    public Set<String> scopeRequestedForAccessToken(
            Set<String> requestedScope,
            Set<String> availableScopes,
            Set<String> defaultScopes) {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return defaultScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    @Override
    public Set<String> scopeRequestedForRefreshToken(
            Set<String> requestedScope,
            Set<String> availableScopes,
            Set<String> allScopes,
            Set<String> defaultScopes) {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return availableScopes;
        }

        Set<String> scopes = new HashSet<String>(availableScopes);
        scopes.retainAll(requestedScope);
        return scopes;
    }
}
