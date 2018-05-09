/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttrThrows;

import java.util.Map;
import java.util.Set;

import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.twitter.TwitterClientConfiguration;

import com.sun.identity.shared.datastruct.ValueNotFoundException;

/**
 * Twitter specific configurations for social auth module.
 */
public class SmsSocialAuthTwitterConfiguration extends AbstractSmsSocialAuthConfiguration {

    /**
     * Constructs SmsSocialAuthTwitterConfiguration instance.
     *
     * @param options The configured SMS attributes.
     */
    SmsSocialAuthTwitterConfiguration(Map<String, Set<String>> options) {
        super(options);
    }

    @Override
    public OAuthClientConfiguration getOAuthClientConfiguration() {
        try {
            return TwitterClientConfiguration.twitterClientConfiguration()
                    .withClientId(getMapAttrThrows(options, CFG_CLIENT_ID))
                    .withClientSecret(getMapAttrThrows(options, CFG_CLIENT_SECRET))
                    .withAuthorizationEndpoint(getMapAttrThrows(options, CFG_AUTH_ENDPOINT))
                    .withTokenEndpoint(getMapAttr(options, CFG_TOKEN_ENDPOINT))
                    .withRequestTokenEndpoint(getMapAttr(options, CFG_REQUEST_TOKEN_ENDPOINT))
                    .withUserInfoEndpoint(getMapAttrThrows(options, CFG_USER_INFO_ENDPOINT))
                    .withRedirectUri(getRedirectUri(getMapAttrThrows(options, CFG_PROXY_URL)))
                    .withProvider(getMapAttrThrows(options, CFG_PROVIDER))
                    .withAuthenticationIdKey(getMapAttrThrows(options, CFG_SUBJECT_PROPERTY)).build();
        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
