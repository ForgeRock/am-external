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
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.plugins.UserInfoClaimsPlugin;

/**
 * Custom implementation of the User Info Claims
 * plugin interface {@link org.forgerock.oauth2.core.plugins.UserInfoClaimsPlugin}
 *
 * <li>
 * The {@code getUserInfo} method
 * populates scope values and sets the resource owner ID to return.
 * </li>
 *
 */
public class CustomUserInfoClaimsPlugin implements UserInfoClaimsPlugin {

    @Override
    public UserInfoClaims getUserInfo(ClientRegistration clientRegistration, AccessToken token, OAuth2Request request) {
        Map<String, Object> response = mapScopes(token);
        response.put("sub", token.getResourceOwnerId());
        UserInfoClaims userInfoClaims = new UserInfoClaims(response, null);
        return userInfoClaims;
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
