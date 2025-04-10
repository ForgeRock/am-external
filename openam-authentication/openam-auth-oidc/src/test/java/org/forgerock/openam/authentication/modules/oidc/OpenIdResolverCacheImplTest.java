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
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.OpenIdResolverFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.inject.AbstractModule;


public class OpenIdResolverCacheImplTest {
    private static final String FAUX_CONFIG_URL_STRING = "https://host.com/.well-known/openid-configuration";
    private static final String FAUX_JWK_URL_STRING = "https://host.com/my/jwk";
    private static final String FAUX_CLIENT_SECRET = "shhh";
    private static final String FAUX_ISSUER = "accounts.somecompany.com";
    private static final String FAUX_ISSUER2 = "test.somecompany.com";
    private static OpenIdResolverCache cache;
    private static OpenIdResolverFactory factory;
    private static OpenIdResolver configResolver;
    private static OpenIdResolver jwkResolver;
    private static OpenIdResolver jwkResolver2;
    private static OpenIdResolver clientSecretResolver;

    @BeforeAll
    static void initialize() throws FailedToLoadJWKException {
        configResolver = mock(OpenIdResolver.class);
        when(configResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        jwkResolver = mock(OpenIdResolver.class);
        when(jwkResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        jwkResolver2 = mock(OpenIdResolver.class);
        when(jwkResolver2.getIssuer()).thenReturn(FAUX_ISSUER2);
        clientSecretResolver = mock(OpenIdResolver.class);
        when(clientSecretResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        factory = mock(OpenIdResolverFactory.class);
        when(factory.createFromOpenIDConfigUrl(any(String.class), any(URL.class))).thenReturn(configResolver);
        when(factory.createJWKResolver(eq(FAUX_ISSUER), any(URL.class), anyInt(), anyInt())).thenReturn(jwkResolver);
        when(factory.createJWKResolver(eq(FAUX_ISSUER2), any(URL.class), anyInt(), anyInt())).thenReturn(jwkResolver2);
        when(factory.createSharedSecretResolver(any(String.class), any(String.class))).thenReturn(clientSecretResolver);

        cache = new OpenIdResolverCacheImpl(factory, null);
    }

    @Test
    void testBasicCreation() throws MalformedURLException, FailedToLoadJWKException {
        OpenIdResolver localConfigResolver = createConfigResolver();
        assertThat(localConfigResolver).isEqualTo(configResolver);

        OpenIdResolver localJwkResolver = createJwtResolver(FAUX_ISSUER);
        assertThat(localJwkResolver).isEqualTo(jwkResolver);

        OpenIdResolver localJwkResolver2 = createJwtResolver(FAUX_ISSUER2);
        assertThat(localJwkResolver2).isEqualTo(jwkResolver2);

        OpenIdResolver localClientSecretResolver = createSecretResolver();
        assertThat(localClientSecretResolver).isEqualTo(clientSecretResolver);
    }

    @Test
    void createInvalidResolver() throws MalformedURLException, FailedToLoadJWKException {

        assertThatThrownBy(() -> cache.createResolver("issuer_string_that_does_not_match_resolver",
                OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL, FAUX_CONFIG_URL_STRING,
                new URL(FAUX_CONFIG_URL_STRING)))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void testBasicLookup() throws MalformedURLException, FailedToLoadJWKException {
        createConfigResolver();
        createJwtResolver(FAUX_ISSUER);
        createSecretResolver();
        OpenIdResolver localConfigResolver =
                cache.getResolverForIssuer(FAUX_ISSUER, FAUX_CONFIG_URL_STRING);
        assertThat(localConfigResolver).isEqualTo(configResolver);

        OpenIdResolver localJwkResolver =
                cache.getResolverForIssuer(FAUX_ISSUER, FAUX_JWK_URL_STRING);
        assertThat(localJwkResolver).isEqualTo(jwkResolver);

        OpenIdResolver localJwkResolver2 =
                cache.getResolverForIssuer(FAUX_ISSUER2, FAUX_JWK_URL_STRING);
        assertThat(localJwkResolver2).isEqualTo(jwkResolver2);

        OpenIdResolver localClientSecretResolver =
                cache.getResolverForIssuer(FAUX_ISSUER, FAUX_CLIENT_SECRET);
        assertThat(localClientSecretResolver).isEqualTo(clientSecretResolver);

    }

    private OpenIdResolver createConfigResolver() throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL,
                FAUX_CONFIG_URL_STRING, new URL(FAUX_CONFIG_URL_STRING));
    }

    private OpenIdResolver createJwtResolver(String issuer) throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(issuer, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_JWK_URL, FAUX_JWK_URL_STRING,
                new URL(FAUX_JWK_URL_STRING));

    }

    private OpenIdResolver createSecretResolver() throws FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET, FAUX_CLIENT_SECRET,
                null);
    }

    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(OpenIdResolverFactory.class).toInstance(factory);
            bind(OpenIdResolverCache.class).to(OpenIdResolverCacheImpl.class);
        }
    }

}
