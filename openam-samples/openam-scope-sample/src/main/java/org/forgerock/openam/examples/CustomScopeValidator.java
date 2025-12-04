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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.examples;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.plugins.ScopeValidator;

/**
 * Custom implementation of the Scope Validator
 * plugin interface {@link org.forgerock.oauth2.core.plugins.ScopeValidator}
 *
 * <li>
 * The {@code validateAuthorizationScope} method
 * adds default scopes, or any allowed scopes provided.
 * </li>
 *
 * <li>
 * The {@code validateAccessTokenScope} method
 * adds default scopes, or any allowed scopes provided.
 * </li>
 *
 * <li>
 * The {@code validateRefreshTokenScope} method
 * adds the scopes from the access token,
 * or any requested scopes provided that are also in the access token scopes.
 * </li>
 *
 *  * <li>
 *  * The {@code validateBackChannelAuthorizationScope} method
 *  * adds default scopes, or any allowed scopes provided.
 *  * </li>
 *
 */
public class CustomScopeValidator implements ScopeValidator {

    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request oAuth2Request) throws ServerException {

        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<String>(clientRegistration.getAllowedScopes());
        scopes.retainAll(scope);
        return scopes;
    }

    @Override
    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration,
            Set<String> scope, OAuth2Request request) throws ServerException {

        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<String>(clientRegistration.getAllowedScopes());
        scopes.retainAll(scope);
        return scopes;
    }

    @Override
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration,
            Set<String> requestedScope, Set<String> tokenScope, OAuth2Request request) {

        if (requestedScope == null || requestedScope.isEmpty()) {
            return tokenScope;
        }

        Set<String> scopes = new HashSet<String>(tokenScope);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    @Override
    public Set<String> validateBackChannelAuthorizationScope(ClientRegistration clientRegistration,
            Set<String> requestedScopes, OAuth2Request request) throws ServerException {

        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        Set<String> scopes = new HashSet<>(clientRegistration.getAllowedScopes());
        scopes.retainAll(requestedScopes);
        return scopes;
    }
}
