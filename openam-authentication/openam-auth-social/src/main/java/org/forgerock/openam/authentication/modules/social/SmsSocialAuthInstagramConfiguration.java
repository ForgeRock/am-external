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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.shared.datastruct.CollectionHelper.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.instagram.InstagramClientConfiguration;

import com.sun.identity.shared.datastruct.ValueNotFoundException;

/**
 * Instagram specific configurations for social auth module.
 */
public class SmsSocialAuthInstagramConfiguration extends AbstractSmsSocialAuthConfiguration {

    /**
     * Constructs SmsSocialAuthInstagramConfiguration instance.
     *
     * @param options
     *         The configured SMS attributes.
     */
    SmsSocialAuthInstagramConfiguration(Map<String, Set<String>> options) {
        super(options);
    }

    @Override
    public OAuthClientConfiguration getOAuthClientConfiguration() {
        try {
            return InstagramClientConfiguration.instagramClientConfiguration()
                    .withClientId(getMapAttrThrows(options, CFG_CLIENT_ID))
                    .withClientSecret(getMapAttrThrows(options, CFG_CLIENT_SECRET))
                    .withAuthorizationEndpoint(getMapAttrThrows(options, CFG_AUTH_ENDPOINT))
                    .withTokenEndpoint(getMapAttr(options, CFG_TOKEN_ENDPOINT))
                    .withScope(new ArrayList<>(options.get(CFG_SCOPE)))
                    .withBasicAuth(getBooleanMapAttr(options, CFG_BASIC_AUTH, true))
                    .withUserInfoEndpoint(getMapAttrThrows(options, CFG_USER_INFO_ENDPOINT))
                    .withRedirectUri(getRedirectUri(getMapAttrThrows(options, CFG_PROXY_URL)))
                    .withProvider(getMapAttrThrows(options, CFG_PROVIDER))
                    .withAuthenticationIdKey(getMapAttrThrows(options, CFG_SUBJECT_PROPERTY)).build();
        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
