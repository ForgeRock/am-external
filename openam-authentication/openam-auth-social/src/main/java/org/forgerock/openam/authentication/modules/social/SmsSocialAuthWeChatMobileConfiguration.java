/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttrThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.datastruct.ValueNotFoundException;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.wechat.WeChatClientConfiguration;

/**
 * WeChat Mobile specific configurations for social auth module.
 */
public class SmsSocialAuthWeChatMobileConfiguration extends AbstractSmsSocialAuthConfiguration {

    private static final String AUTHORIZATION_ENDPOINT = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String ACCESS_TOKEN_ENDPOINT = "https://api.wechat.com/sns/oauth2/access_token";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";

    /**
     * Constructs SmsSocialAuthWeChatMobileConfiguration instance.
     *
     * @param options
     *         The configured SMS attributes.
     */
    SmsSocialAuthWeChatMobileConfiguration(Map<String, Set<String>> options) {
        super(options);
    }

    @Override
    public OAuthClientConfiguration getOAuthClientConfiguration() {
        try {
            return WeChatClientConfiguration.weChatClientConfiguration()
                    .withClientId(CLIENT_ID)
                    .withClientSecret(CLIENT_SECRET)
                    .withAuthorizationEndpoint(AUTHORIZATION_ENDPOINT)
                    .withTokenEndpoint(ACCESS_TOKEN_ENDPOINT)
                    .withScope(new ArrayList<>(options.get(CFG_SCOPE)))
                    .withBasicAuth(false)
                    .withUserInfoEndpoint(getMapAttrThrows(options, CFG_USER_INFO_ENDPOINT))
                    .withRedirectUri(getRedirectUri(getMapAttrThrows(options, CFG_PROXY_URL)))
                    .withProvider(getMapAttrThrows(options, CFG_PROVIDER))
                    .withAuthenticationIdKey(getMapAttrThrows(options, CFG_SUBJECT_PROPERTY)).build();
        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}