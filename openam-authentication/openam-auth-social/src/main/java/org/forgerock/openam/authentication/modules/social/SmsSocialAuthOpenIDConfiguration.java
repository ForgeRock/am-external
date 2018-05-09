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
import org.forgerock.oauth.clients.oidc.OpenIDConnectClientConfiguration;


/**
 * OpenID specific configurations for social auth module.
 */
public class SmsSocialAuthOpenIDConfiguration extends AbstractSmsSocialAuthConfiguration {

    private static final String CFG_CRYPTO_CONTEXT_TYPE = "cryptoContextType";
    private static final String CFG_CRYPTO_CONTEXT_TYPE_WELL_KNOWN_CONFIG_URL = ".well-known/openid-configuration_url";
    private static final String CFG_CRYPTO_CONTEXT_TYPE_JWK_URL = "jwk_url";
    private static final String CFG_CRYPTO_CONTEXT_VALUE = "cryptoContextValue";

    /**
     * Constructs SmsSocialAuthOpenIDConfiguration instance.
     *
     * @param options
     *         The configured SMS attributes.
     */
    SmsSocialAuthOpenIDConfiguration(Map<String, Set<String>> options) {
        super(options);
    }

    @Override
    public OAuthClientConfiguration getOAuthClientConfiguration() {
        try {
            OpenIDConnectClientConfiguration.Builder<?, OpenIDConnectClientConfiguration> builder =
                    OpenIDConnectClientConfiguration.openIdConnectClientConfiguration();
            builder
                .withClientId(getMapAttrThrows(options, CFG_CLIENT_ID))
                .withClientSecret(getMapAttrThrows(options, CFG_CLIENT_SECRET))
                .withAuthorizationEndpoint(getMapAttrThrows(options, CFG_AUTH_ENDPOINT))
                .withTokenEndpoint(getMapAttr(options, CFG_TOKEN_ENDPOINT))
                .withScope(new ArrayList<>(options.get(CFG_SCOPE)))
                .withScopeDelimiter(getMapAttr(options, CFG_SCOPE_DELIMITER, " "))
                .withBasicAuth(getBooleanMapAttr(options, CFG_BASIC_AUTH, true))
                .withUserInfoEndpoint(getMapAttrThrows(options, CFG_USER_INFO_ENDPOINT))
                .withRedirectUri(getRedirectUri(getMapAttrThrows(options, CFG_PROXY_URL)))
                .withProvider(getMapAttrThrows(options, CFG_PROVIDER))
                .withAuthenticationIdKey(getMapAttrThrows(options, CFG_SUBJECT_PROPERTY))
                .withIssuer(getMapAttr(options, CFG_ISSUER_NAME));

            if(getMapAttrThrows(options, CFG_CRYPTO_CONTEXT_TYPE).equals(CFG_CRYPTO_CONTEXT_TYPE_JWK_URL)) {
                builder.withJwk(getMapAttrThrows(options,CFG_CRYPTO_CONTEXT_VALUE));
            } else if (getMapAttrThrows(options, CFG_CRYPTO_CONTEXT_TYPE)
                    .equals(CFG_CRYPTO_CONTEXT_TYPE_WELL_KNOWN_CONFIG_URL)) {
                builder.withWellKnownEndpoint(getMapAttrThrows(options,CFG_CRYPTO_CONTEXT_VALUE));
            }
            return builder.build();
        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}