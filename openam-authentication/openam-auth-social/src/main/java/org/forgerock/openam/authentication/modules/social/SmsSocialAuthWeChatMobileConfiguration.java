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
 * Copyright 2017 ForgeRock AS.
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