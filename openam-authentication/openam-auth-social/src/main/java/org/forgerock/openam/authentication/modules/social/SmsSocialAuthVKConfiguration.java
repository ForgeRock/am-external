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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttrThrows;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.datastruct.ValueNotFoundException;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.vk.VKClientConfiguration;

/**
 * VKontakte specific configuration for social auth module.
 */
public class SmsSocialAuthVKConfiguration extends AbstractSmsSocialAuthConfiguration {

    /**
     * Constructs SmsSocialAuthVKConfiguration instance.
     *
     * @param options
     *         The configured SMS attributes.
     */
    SmsSocialAuthVKConfiguration(Map<String, Set<String>> options) {
        super(options);
    }

    @Override
    public OAuthClientConfiguration getOAuthClientConfiguration() {
        try {
            return VKClientConfiguration.vkClientConfiguration()
                .withClientId(getMapAttrThrows(options, CFG_CLIENT_ID))
                .withClientSecret(getMapAttrThrows(options, CFG_CLIENT_SECRET))
                .withAuthorizationEndpoint(getMapAttrThrows(options, CFG_AUTH_ENDPOINT))
                .withTokenEndpoint(getMapAttr(options, CFG_TOKEN_ENDPOINT))
                .withUserInfoEndpoint(getMapAttrThrows(options, CFG_USER_INFO_ENDPOINT))
                .withScope(new ArrayList<>(options.get(CFG_SCOPE)))
                .withRedirectUri(getRedirectUri(getMapAttrThrows(options, CFG_PROXY_URL)))
                .withProvider(getMapAttrThrows(options, CFG_PROVIDER))
                .withAuthenticationIdKey(getMapAttrThrows(options, CFG_SUBJECT_PROPERTY))
                .withBasicAuth(getBooleanMapAttr(options, CFG_BASIC_AUTH, false))
                .withApiVersion(getMapAttrThrows(options, CFG_API_VERSION)).build();
        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
