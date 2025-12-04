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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.oidc;

import org.forgerock.oauth.resolvers.OpenIdResolverFactory;
import org.forgerock.oauth.resolvers.OpenIdResolver;

import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.openam.core.CoreWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class OidcResolverCacheImplTest {
    private static final String FAUX_CONFIG_URL_STRING = "https://host.com/.well-known/openid-configuration";
    private static final String FAUX_JWK_URL_STRING = "https://host.com/my/jwk";
    private static final String FAUX_CLIENT_SECRET = "shhh";
    private static final String FAUX_ISSUER = "accounts.somecompany.com";
    private static final String FAUX_ISSUER2 = "test.somecompany.com";

    @Mock
    private OpenIdResolverFactory factory;
    @Mock
    private CoreWrapper coreWrapper;
    @InjectMocks
    private OidcResolverCacheImpl resolverCache;

    @BeforeEach
    void initialize() throws FailedToLoadJWKException {
        resolverCache = new OidcResolverCacheImpl(factory, coreWrapper);
    }

    @Test
    void testResolverCreationUsingConfig() throws Exception {
        OpenIdResolver configResolver = mock(OpenIdResolver.class);

        // Given
        given(factory.createFromOpenIDConfigUrl(any(String.class), any(URL.class))).willReturn(configResolver);
        given(configResolver.getIssuer()).willReturn(FAUX_ISSUER);

        // When
        OpenIdResolver localConfigResolver = createConfigResolver();

        // Then
        assertThat(localConfigResolver).isEqualTo(configResolver);
    }

    @Test
    void testResolverCreationUsingJwk() throws Exception {
        OpenIdResolver jwkResolver = mock(OpenIdResolver.class);
        OpenIdResolver jwkResolver2 = mock(OpenIdResolver.class);

        // Given
        given(factory.createJWKResolver(eq(FAUX_ISSUER), any(URL.class))).willReturn(jwkResolver);
        given(factory.createJWKResolver(eq(FAUX_ISSUER2), any(URL.class))).willReturn(jwkResolver2);

        // When
        OpenIdResolver localJwkResolver = createJwtResolver(FAUX_ISSUER);
        OpenIdResolver localJwkResolver2 = createJwtResolver(FAUX_ISSUER2);

        // Then
        assertThat(localJwkResolver).isEqualTo(jwkResolver);
        assertThat(localJwkResolver2).isEqualTo(jwkResolver2);
    }

    @Test
    void testResolverCreationUsingClientSecret() throws FailedToLoadJWKException {
        OpenIdResolver clientSecretResolver = mock(OpenIdResolver.class);

        // Given
        given(factory.createSharedSecretResolver(any(String.class), any(String.class)))
                .willReturn(clientSecretResolver);

        // When
        OpenIdResolver localClientSecretResolver =  createSecretResolver();

        // Then
        assertThat(localClientSecretResolver).isEqualTo(clientSecretResolver);
    }

    @Test
    void testCreateInvalidResolver() throws FailedToLoadJWKException {
        OpenIdResolver configResolver = mock(OpenIdResolver.class);

        // Given
        given(factory.createFromOpenIDConfigUrl(any(String.class), any(URL.class))).willReturn(configResolver);
        given(configResolver.getIssuer()).willReturn(FAUX_ISSUER);

        // When
        Throwable throwable = catchThrowable(() ->
                resolverCache.createResolver("issuer_string_that_does_not_match_resolver",
                        OidcNode.OpenIdValidationType.WELL_KNOWN_URL.name(), FAUX_CONFIG_URL_STRING,
                new URL(FAUX_CONFIG_URL_STRING)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testBasicLookupForSecretResolver() throws Exception {
        OpenIdResolver clientSecretResolver = mock(OpenIdResolver.class);

        // Given
        given(factory.createSharedSecretResolver(any(String.class), any(String.class)))
                .willReturn(clientSecretResolver);

        // When
        createSecretResolver();

        // Then
        OpenIdResolver localClientSecretResolver =
                resolverCache.getResolverForIssuer(FAUX_ISSUER, FAUX_CLIENT_SECRET);
        assertThat(localClientSecretResolver).isEqualTo(clientSecretResolver);
    }

    @Test
    void testBasicLookupForConfigResolver() throws Exception {
        OpenIdResolver configResolver = mock(OpenIdResolver.class);

        // Given
        given(factory.createFromOpenIDConfigUrl(any(String.class), any(URL.class))).willReturn(configResolver);
        given(configResolver.getIssuer()).willReturn(FAUX_ISSUER);

        // When
        createConfigResolver();
        OpenIdResolver localConfigResolver =
                resolverCache.getResolverForIssuer(FAUX_ISSUER, FAUX_CONFIG_URL_STRING);

        // Then
        assertThat(localConfigResolver).isEqualTo(configResolver);
    }

    @Test
    void testBasicLookupForJwkResolver() throws Exception {
        OpenIdResolver jwkResolver = mock(OpenIdResolver.class);
        OpenIdResolver jwkResolver2 = mock(OpenIdResolver.class);

        // Given
        given(factory.createJWKResolver(eq(FAUX_ISSUER), any(URL.class))).willReturn(jwkResolver);
        given(factory.createJWKResolver(eq(FAUX_ISSUER2), any(URL.class))).willReturn(jwkResolver2);


        // When
        createJwtResolver(FAUX_ISSUER);
        createJwtResolver(FAUX_ISSUER2);
        OpenIdResolver localJwkResolver =
                resolverCache.getResolverForIssuer(FAUX_ISSUER, FAUX_JWK_URL_STRING);
        OpenIdResolver localJwkResolver2 =
                resolverCache.getResolverForIssuer(FAUX_ISSUER2, FAUX_JWK_URL_STRING);

        // Then
        assertThat(localJwkResolver).isEqualTo(jwkResolver);
        assertThat(localJwkResolver2).isEqualTo(jwkResolver2);
    }

    private OpenIdResolver createConfigResolver() throws MalformedURLException, FailedToLoadJWKException {
        return resolverCache.createResolver(FAUX_ISSUER, OidcNode.OpenIdValidationType.WELL_KNOWN_URL.name(),
                FAUX_CONFIG_URL_STRING, new URL(FAUX_CONFIG_URL_STRING));
    }

    private OpenIdResolver createJwtResolver(String issuer) throws MalformedURLException, FailedToLoadJWKException {
        return resolverCache.createResolver(issuer, OidcNode.OpenIdValidationType.JWK_URL.name(),
                FAUX_JWK_URL_STRING, new URL(FAUX_JWK_URL_STRING));

    }

    private OpenIdResolver createSecretResolver() throws FailedToLoadJWKException {
        return resolverCache.createResolver(FAUX_ISSUER, OidcNode.OpenIdValidationType.CLIENT_SECRET.name(),
                FAUX_CLIENT_SECRET, null);
    }
}
