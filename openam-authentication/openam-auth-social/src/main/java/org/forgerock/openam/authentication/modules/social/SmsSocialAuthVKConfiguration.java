/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
