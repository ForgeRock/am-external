/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.oidc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.forgerock.oauth.resolvers.OpenIdResolverFactory;
import org.forgerock.oauth.resolvers.OpenIdResolver;

import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;



public class OpenIdResolverCacheImplTest {
    private static final String FAUX_CONIFIG_URL_STRING = "https://host.com/.well-known/openid-configuration";
    private static final String FAUX_JWK_URL_STRING = "https://host.com/my/jwk";
    private static final String FAUX_CLIENT_SECRET = "shhh";
    private static final String FAUX_ISSUER = "accounts.somecompany.com";
    private OpenIdResolverCache cache;
    private OpenIdResolverFactory factory;
    private OpenIdResolver configResolver;
    private OpenIdResolver jwkResolver;
    private OpenIdResolver clientSecretResolver;

    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(OpenIdResolverFactory.class).toInstance(factory);
            bind(OpenIdResolverCache.class).to(OpenIdResolverCacheImpl.class);
        }
    }

    @BeforeTest
    public void initialize() throws FailedToLoadJWKException {
        configResolver = mock(OpenIdResolver.class);
        when(configResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        jwkResolver = mock(OpenIdResolver.class);
        when(jwkResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        clientSecretResolver = mock(OpenIdResolver.class);
        when(clientSecretResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        factory = mock(OpenIdResolverFactory.class);
        when(factory.createFromOpenIDConfigUrl(any(URL.class))).thenReturn(configResolver);
        when(factory.createJWKResolver(any(String.class), any(URL.class), anyInt(), anyInt())).thenReturn(jwkResolver);
        when(factory.createSharedSecretResolver(any(String.class), any(String.class))).thenReturn(clientSecretResolver);

        cache = Guice.createInjector(new MyModule()).getInstance(OpenIdResolverCache.class);
    }

    @Test
    public void testBasicCreation() throws MalformedURLException, FailedToLoadJWKException {
        OpenIdResolver localConfigResolver = createConfigResolver();
        assertTrue(localConfigResolver == configResolver);

        OpenIdResolver localJwkResolver = createJwtResolver();
        assertTrue(localJwkResolver == jwkResolver);

        OpenIdResolver localClientSecretResolver =  createSecretResolver();
        assertTrue(localClientSecretResolver == clientSecretResolver);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void createInvalidResolver() throws MalformedURLException, FailedToLoadJWKException {
        cache.createResolver("issuer_string_that_does_not_match_resolver", OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL,
                FAUX_CONIFIG_URL_STRING, new URL(FAUX_CONIFIG_URL_STRING));

    }

    @Test
    public void testBasicLookup() throws MalformedURLException, FailedToLoadJWKException {
        createConfigResolver();
        createJwtResolver();
        createSecretResolver();
        OpenIdResolver localConfigResolver =
                cache.getResolverForIssuer(FAUX_CONIFIG_URL_STRING);
        assertTrue(localConfigResolver == configResolver);

        OpenIdResolver localJwkResolver =
                cache.getResolverForIssuer(FAUX_JWK_URL_STRING);
        assertTrue(localJwkResolver == jwkResolver);

        OpenIdResolver localClientSecretResolver =
                cache.getResolverForIssuer(FAUX_CLIENT_SECRET);
        assertTrue(localClientSecretResolver == clientSecretResolver);

    }

    private OpenIdResolver createConfigResolver() throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL, FAUX_CONIFIG_URL_STRING,
                new URL(FAUX_CONIFIG_URL_STRING));
    }

    private OpenIdResolver createJwtResolver() throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_JWK_URL, FAUX_JWK_URL_STRING,
                new URL(FAUX_JWK_URL_STRING));

    }
    private OpenIdResolver createSecretResolver() throws FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET, FAUX_CLIENT_SECRET,
                null);
    }

}
