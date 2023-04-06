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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oidc;

import com.sun.identity.authentication.spi.AuthLoginException;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.oauth.resolvers.exceptions.OpenIdConnectVerificationException;
import org.forgerock.openam.auth.nodes.testsupport.JwtClaimsSetFactory;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.Test;
import org.junit.Before;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.class)
public class OidcIdTokenJwtHandlerTest {
    private static final String VALIDATION_VALUE_CONFIG =
            "https://accounts.google.com/.well-known/openid-configuration";
    private static final String CLIENT_SECRET_CONFIG = "password";

    @Mock
    private OidcResolverCacheImpl oidcResolverCache;
    @Mock
    private OidcNode.Config config;

    private OidcIdTokenJwtHandler jwtHandler;

    @Before
    public void setup() throws Exception {
        OidcIdTokenJwtHandlerFactory oidcIdTokenJwtHandlerFactory = (config, clientSecret)
                -> new OidcIdTokenJwtHandler(oidcResolverCache, config,
                    new JwtReconstruction(), Optional.empty());
        jwtHandler = oidcIdTokenJwtHandlerFactory.createOidcIdTokenJwtHandler(config, setupSecret());
    }

    @Test
    public void shouldCheckAuthorizedPartiesExistsGivenNullAuthorizedParties() {
        //Given
        String authorizedParties = null;

        //When
        boolean result = jwtHandler.authorizedPartiesExists(authorizedParties);

        //Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldCheckIsAuthorizedPartiesValidGivenInvalidAuthorizedParties() {
        //Given
        String authorizedParties = JwtClaimsSetFactory.AUTHORIZED_PARTIES_CONFIG + "not valid";

        //When
        boolean result = jwtHandler.isAuthorizedPartiesValid(authorizedParties);

        //Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldCheckIsAlgorithmDefaultValueGivenNotDefaultAlgorithm() {
        //Given
        JwsAlgorithm algorithm = JwsAlgorithm.HS384;

        //When
        boolean result = jwtHandler.isAlgorithmDefaultValue(algorithm);

        //Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldCheckUsingMacBasedAlgorithmAndNotClientSecretGivenNotMacBasedAlgorithmAndClientSecret() {
        //Given
        JwsAlgorithm algorithm = JwsAlgorithm.HS256;
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.CLIENT_SECRET);

        //When
        boolean result = jwtHandler.usingMacBasedAlgorithmAndNotClientSecret(algorithm);

        //Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldCheckIfMultipleAudiencesExistTheClientShouldVerifyAuthorisedPartiesIsPresent() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwsHeader header = new JwsHeader();
        List<String> audiences = List.of(JwtClaimsSetFactory.AUDIENCE_CONFIG, "second audience");
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet()
                .withCustomAttribute("aud", audiences)
                .withAuthorisedParties(null).build();
        header.setAlgorithm(JwsAlgorithm.RS256);

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(signedJwt.getHeader()).willReturn(header);
        given(config.audienceName()).willReturn(JwtClaimsSetFactory.AUDIENCE_CONFIG);
        given(config.idTokenIssuer()).willReturn(JwtClaimsSetFactory.ISSUER_CONFIG);
        given(config.unreasonableLifetimeLimit()).willReturn(60);

        //When
        boolean result = jwtHandler.isJwtValid(signedJwt);

        //Then
        assertThat(result).isFalse();

    }

    @Test
    public void shouldFailToHandleResolverGivenClientSecretAndNullClientSecretConfig() {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.CLIENT_SECRET);

        //When
        Throwable throwable = catchThrowable(() -> jwtHandler.handleResolver(signedJwt));

        //Then
        assertThat(throwable).isInstanceOf(AuthLoginException.class)
                .hasMessageContaining("ValidationType is Client Secret but Client Secret parameter is null");
    }

    @Test
    public void shouldFailToHandleResolverGivenInvalidValidationValueConfig() {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn("not_a_url");

        //When
        Throwable throwable = catchThrowable(() -> jwtHandler.handleResolver(signedJwt));

        //Then
        assertThat(throwable).isInstanceOf(AuthLoginException.class)
                .hasMessageContaining("Failed to create URL Object from validation type");
    }

    @Test
    public void shouldFailToHandleResolverGivenCacheCannotCreateAResolver() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn(VALIDATION_VALUE_CONFIG);
        URL validationValueUrl = new URL(VALIDATION_VALUE_CONFIG);
        given(oidcResolverCache.createResolver(JwtClaimsSetFactory.ISSUER_CONFIG, "WELL_KNOWN_URL",
                VALIDATION_VALUE_CONFIG, validationValueUrl)).willThrow(FailedToLoadJWKException.class);

        //When
        Throwable throwable = catchThrowable(() -> jwtHandler.handleResolver(signedJwt));

        //Then
        assertThat(throwable).isInstanceOf(AuthLoginException.class)
                .hasMessageContaining("Could not create a new OpenIdResolver");
    }

    @Test
    public void shouldHandleResolverGivenNoResolverInCache() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();
        OpenIdResolver openIdResolver = Mockito.mock(OpenIdResolver.class);

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn(VALIDATION_VALUE_CONFIG);
        URL validationValueUrl = new URL(VALIDATION_VALUE_CONFIG);
        given(oidcResolverCache.createResolver(JwtClaimsSetFactory.ISSUER_CONFIG, "WELL_KNOWN_URL",
                VALIDATION_VALUE_CONFIG, validationValueUrl)).willReturn(openIdResolver);

        //When
        OpenIdResolver resolver = jwtHandler.handleResolver(signedJwt);

        //Then
        assertThat(resolver).isEqualTo(openIdResolver);
    }

    @Test
    public void shouldHandleResolverGivenValidationTypeIsClientSecret() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        this.jwtHandler = setupJwtHandlerWithNonNullClientSecret();
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.CLIENT_SECRET);

        //When
        OpenIdResolver resolver = jwtHandler.handleResolver(signedJwt);

        //Then
        assertThat(resolver.getClass()).isEqualTo(SharedSecretOpenIdResolverImpl.class);
    }

    @Test
    public void shouldHandleResolverGivenResolverExistsInCache() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        OpenIdResolver openIdResolver = Mockito.mock(OpenIdResolver.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn(VALIDATION_VALUE_CONFIG);
        given(oidcResolverCache.getResolverForIssuer(JwtClaimsSetFactory.ISSUER_CONFIG, VALIDATION_VALUE_CONFIG))
                .willReturn(openIdResolver);

        //When
        OpenIdResolver resolver = jwtHandler.handleResolver(signedJwt);

        //Then
        assertThat(resolver).isEqualTo(openIdResolver);
    }

    @Test
    public void shouldFailIsJwtValidGivenResolverCannotValidateIdentity() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        OpenIdResolver openIdResolver = Mockito.mock(OpenIdResolver.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        JwsHeader header = new JwsHeader();
        header.setAlgorithm(JwsAlgorithm.RS256);
        given(signedJwt.getHeader()).willReturn(header);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn(VALIDATION_VALUE_CONFIG);
        given(config.authorisedParties()).willReturn(Set.of(JwtClaimsSetFactory.AUTHORIZED_PARTIES_CONFIG));
        given(config.audienceName()).willReturn(JwtClaimsSetFactory.AUDIENCE_CONFIG);
        given(config.idTokenIssuer()).willReturn(JwtClaimsSetFactory.ISSUER_CONFIG);
        given(config.unreasonableLifetimeLimit()).willReturn(60);
        given(jwtHandler.handleResolver(signedJwt)).willReturn(openIdResolver);
        willThrow(new OpenIdConnectVerificationException()).given(openIdResolver).validateIdentity(signedJwt);

        //When
        boolean result = jwtHandler.isJwtValid(signedJwt);

        //Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldCheckIsJwtValidGivenValidJwt() throws Exception {
        SignedJwt signedJwt = Mockito.mock(SignedJwt.class);
        OpenIdResolver openIdResolver = Mockito.mock(OpenIdResolver.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        //Given
        JwsHeader header = new JwsHeader();
        header.setAlgorithm(JwsAlgorithm.RS256);
        given(signedJwt.getHeader()).willReturn(header);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.oidcValidationValue()).willReturn(VALIDATION_VALUE_CONFIG);
        given(config.authorisedParties()).willReturn(Set.of(JwtClaimsSetFactory.AUTHORIZED_PARTIES_CONFIG));
        given(config.audienceName()).willReturn(JwtClaimsSetFactory.AUDIENCE_CONFIG);
        given(config.idTokenIssuer()).willReturn(JwtClaimsSetFactory.ISSUER_CONFIG);
        given(config.unreasonableLifetimeLimit()).willReturn(60);
        given(jwtHandler.handleResolver(signedJwt)).willReturn(openIdResolver);
        doNothing().when(openIdResolver).validateIdentity(signedJwt);

        //When
        boolean result = jwtHandler.isJwtValid(signedJwt);

        //Then
        assertThat(result).isTrue();
    }

    private Optional<SecretReference<GenericSecret>> setupSecret() {
        String name = "clientsecret";
        SecretsProvider secretsProvider = Mockito.mock(SecretsProvider.class);
        GenericSecret genericSecret = GenericSecret.password(CLIENT_SECRET_CONFIG.toCharArray());

        Purpose<GenericSecret> purpose = Purpose.purpose(name, GenericSecret.class);
        Promise<GenericSecret, NoSuchSecretException> promises = Promises.newResultPromise(genericSecret);
        given(secretsProvider.getNamedSecret(purpose, name)).willReturn(promises);
        return Optional.of(SecretReference.named(secretsProvider, purpose, name,
                Clock.systemUTC()));
    }

    private OidcIdTokenJwtHandler setupJwtHandlerWithNonNullClientSecret() {
        OidcIdTokenJwtHandlerFactory oidcIdTokenJwtHandlerFactory =
                (config, clientSecret) -> new OidcIdTokenJwtHandler(oidcResolverCache, config,
                        new JwtReconstruction(), clientSecret);
        return jwtHandler = oidcIdTokenJwtHandlerFactory.createOidcIdTokenJwtHandler(config, setupSecret());
    }
}
