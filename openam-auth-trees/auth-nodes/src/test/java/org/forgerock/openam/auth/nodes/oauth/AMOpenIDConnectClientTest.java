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
 * Copyright 2021-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.oauth2.OAuth2Constants.ClientPurpose.RP_ID_TOKEN_DECRYPTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClientConfiguration;
import org.forgerock.oauth.resolvers.JWKOpenIdResolverImpl;
import org.forgerock.oauth.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.oauth.resolvers.exceptions.InvalidSignatureException;
import org.forgerock.oauth.resolvers.service.OpenIdResolverService;
import org.forgerock.oauth.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.UserInfoResponseType;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AMOpenIDConnectClientTest {

    @Mock
    private Handler httpHandler;

    @Mock
    private Clock clock;

    private SecureRandom random;

    @Mock
    private OpenIdResolverService resolverService;

    @Mock
    private OpenIdResolverServiceConfigurator serviceConfigurator;

    @Mock
    private OpenIDConnectClientConfiguration config;

    @Mock
    private OpenIDConnectClientConfig amConfig;

    @Mock
    private SecretsProviderFacade secretsProviderFacade;

    @Mock
    private DataStore dataStore;

    @Mock
    JWKOpenIdResolverImpl jwkOpenIdResolver;

    @Mock
    SharedSecretOpenIdResolverImpl sharedSecretOpenIdResolver;

    private JsonValue claims;
    private KeyPair keyPair;

    private static final String NAME_CLAIM_KEY = "name";
    private static final String NAME_CLAIM_VALUE = "demo";
    private static final String MAIL_CLAIM_KEY = "mail";
    private static final String MAIL_CLAIM_VALUE = "demo@demo.com";

    @DataProvider
    public Object[][] incorrectExpectations() throws Exception {
        return new Object[][]{
                {buildSignedJwt(), UserInfoResponseType.JSON},
                {buildSignedJwt(), UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT},
                {buildSignedThenEncryptedJwt(), UserInfoResponseType.SIGNED_JWT},
                {buildSignedThenEncryptedJwt(), UserInfoResponseType.JSON},
                {claims, UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT},
                {claims, UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT},
        };
    }

    @BeforeSuite
    public void setUp() throws Exception {
        openMocks(this);
        random = SecureRandom.getInstanceStrong();
        keyPair = createTestKeyPair();
        claims = new JsonValue(Map.of(NAME_CLAIM_KEY, NAME_CLAIM_VALUE, MAIL_CLAIM_KEY, MAIL_CLAIM_VALUE));
    }

    @Test
    public void shouldReturnUserInfoFromIdTokenJwtClaimsIfNoUserInfoUri() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        when(config.getUserInfoEndpoint()).thenReturn(null);

        //when
        UserInfo userInfo = amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();

        //then
        assertThat(userInfo.getRawProfile().get(NAME_CLAIM_KEY).asString()).isEqualTo(NAME_CLAIM_VALUE);
        assertThat(userInfo.getRawProfile().get(MAIL_CLAIM_KEY).asString()).isEqualTo(MAIL_CLAIM_VALUE);
    }

    @Test
    public void shouldReturnUserInfoFromIdTokenJwtClaimsIfNoAccessToken() throws Exception {
        //given
        dataStoreContainsClaimsSetButNoAccessToken();
        userInfoEndpointConfigured();

        //when
        UserInfo userInfo = amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();

        //then
        assertThat(userInfo.getRawProfile().get(NAME_CLAIM_KEY).asString()).isEqualTo(NAME_CLAIM_VALUE);
        assertThat(userInfo.getRawProfile().get(MAIL_CLAIM_KEY).asString()).isEqualTo(MAIL_CLAIM_VALUE);
    }

    @Test
    public void shouldReturnUserInfoFromValidJsonResponse() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, claims);
        clientConfiguredForResponseType(UserInfoResponseType.JSON);

        //when
        UserInfo userInfo = amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();

        //then
        assertThat(userInfo.getRawProfile().get(NAME_CLAIM_KEY).asString()).isEqualTo(NAME_CLAIM_VALUE);
        assertThat(userInfo.getRawProfile().get(MAIL_CLAIM_KEY).asString()).isEqualTo(MAIL_CLAIM_VALUE);
    }

    @Test
    public void shouldReturnUserInfoFromValidSignedJwt() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_JWT);
        resolverServiceReturnsExpectedResolver();

        //when
        UserInfo userInfo = amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();

        //then
        assertThat(userInfo.getRawProfile().get(NAME_CLAIM_KEY).asString()).isEqualTo(NAME_CLAIM_VALUE);
        assertThat(userInfo.getRawProfile().get(MAIL_CLAIM_KEY).asString()).isEqualTo(MAIL_CLAIM_VALUE);
    }

    @Test
    public void shouldReturnUserInfoFromValidSignedThenEncryptedJwt() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedThenEncryptedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT);
        resolverServiceReturnsExpectedResolver();
        secretsProviderReturnsKey(validDataDecryptionKey());

        //when
        UserInfo userInfo = amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();

        //then
        assertThat(userInfo.getRawProfile().get(NAME_CLAIM_KEY).asString()).isEqualTo(NAME_CLAIM_VALUE);
        assertThat(userInfo.getRawProfile().get(MAIL_CLAIM_KEY).asString()).isEqualTo(MAIL_CLAIM_VALUE);
    }

    @Test(expectedExceptions = OAuthException.class)
    public void shouldThrowExceptionIfCannotDecryptSignedThenEncryptedJwt() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedThenEncryptedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT);
        resolverServiceReturnsExpectedResolver();
        secretsProviderReturnsKey(invalidDataDecryptionKey());

        //when
        amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();
    }

    @Test(expectedExceptions = OAuthException.class)
    public void shouldThrowExceptionIfCannotVerifySignedJwt() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_JWT);
        resolverServiceReturnsExpectedResolver();
        expectedResolverThrowsInvalidSignatureException();

        //when
        amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();
    }

    @Test(expectedExceptions = OAuthException.class)
    public void shouldThrowExceptionIfCannotVerifySignedThenEncryptedJwt() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedThenEncryptedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT);
        resolverServiceReturnsExpectedResolver();
        secretsProviderReturnsKey(validDataDecryptionKey());
        expectedResolverThrowsInvalidSignatureException();

        //when
        amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();
    }

    @Test(expectedExceptions = OAuthException.class)
    public void shouldThrowExceptionIfProcessingJwtWithoutJWKOpenIdResolverImplResolver() throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, buildSignedJwt());
        clientConfiguredForResponseType(UserInfoResponseType.SIGNED_JWT);
        resolverServiceReturnsUnexpectedResolver();

        //when
        amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();
    }

    @Test(dataProvider = "incorrectExpectations", expectedExceptions = OAuthException.class)
    public void shouldThrowExceptionIfExpectedResponseTypeNotReceived(Object responseEntity,
            UserInfoResponseType expectedType) throws Exception {
        //given
        dataStoreContainsClaimsSetAndAccessToken();
        userInfoEndpointConfigured();
        authenticationIdProperlyConfigured();
        userInfoEndpointResponse(Status.OK, responseEntity);
        clientConfiguredForResponseType(expectedType);
        resolverServiceReturnsExpectedResolver();
        secretsProviderReturnsKey(validDataDecryptionKey());

        //when
        amOpenIdConnectClient().getUserInfo(dataStore).getOrThrow();
    }

    /* HELPER METHODS */

    private AMOpenIDConnectClient amOpenIdConnectClient() {
        return new AMOpenIDConnectClient(httpHandler, config, clock, random, resolverService,
                serviceConfigurator, amConfig, secretsProviderFacade);
    }

    private void dataStoreContainsClaimsSetButNoAccessToken() throws OAuthException {
        JsonValue dataStoreData = new JsonValue(Map.of("claims_set", claims));
        when(dataStore.retrieveData()).thenReturn(dataStoreData);
    }

    private void userInfoEndpointConfigured() throws URISyntaxException {
        when(config.getUserInfoEndpoint()).thenReturn(new URI("http://some.url.com"));
    }

    private void dataStoreContainsClaimsSetAndAccessToken() throws OAuthException {
        JsonValue dataStoreData = new JsonValue(Map.of("claims_set", claims, "access_token",
                "some access token"));
        when(dataStore.retrieveData()).thenReturn(dataStoreData);
    }

    private void authenticationIdProperlyConfigured() {
        when(config.getAuthenticationIdKey()).thenReturn(NAME_CLAIM_KEY);
    }

    private void userInfoEndpointResponse(Status status, Object entity) {
        when(httpHandler.handle(any(), any())).thenReturn(
                Promises.newPromise(() -> new Response(status).setEntity(entity)));
    }

    private void clientConfiguredForResponseType(UserInfoResponseType userInfoResponseType) {
        when(amConfig.userInfoResponseType()).thenReturn(userInfoResponseType);
    }

    private void resolverServiceReturnsExpectedResolver() {
        when(resolverService.getResolverForIssuer(any())).thenReturn(Optional.of(jwkOpenIdResolver));
    }

    private void resolverServiceReturnsUnexpectedResolver() {
        when(resolverService.getResolverForIssuer(any())).thenReturn(Optional.of(sharedSecretOpenIdResolver));
    }

    private void secretsProviderReturnsKey(DataDecryptionKey key) throws Exception {
        when(secretsProviderFacade.getValidSecrets(RP_ID_TOKEN_DECRYPTION))
                .thenReturn(Promises.newResultPromise(Stream.of(key)));
    }

    private void expectedResolverThrowsInvalidSignatureException() throws InvalidSignatureException,
            FailedToLoadJWKException {
        doThrow(new InvalidSignatureException()).when(jwkOpenIdResolver).verifySignature(any());
    }

    private String buildSignedJwt() throws Exception {
        SigningKey signingKey = new SigningKey(new SecretBuilder()
                .secretKey(keyPair.getPrivate())
                .publicKey(keyPair.getPublic())
                .expiresAt(Instant.MAX)
                .stableId("some-id"));

        return new SignedJwtBuilderImpl(new SecretRSASigningHandler(signingKey))
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(new JwtClaimsSetBuilder().claims(claims.asMap()).build())
                .build();
    }

    private String buildSignedThenEncryptedJwt() throws Exception {
        SigningKey signingKey = new SigningKey(new SecretBuilder()
                .secretKey(keyPair.getPrivate())
                .publicKey(keyPair.getPublic())
                .expiresAt(Instant.MAX)
                .stableId("some-id"));

        return new SignedJwtBuilderImpl(new SecretRSASigningHandler(signingKey))
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(new JwtClaimsSetBuilder().claims(claims.asMap()).build())
                .encrypt(keyPair.getPublic()).headers().alg(JweAlgorithm.RSA_OAEP_256).enc(EncryptionMethod.A128GCM)
                .cty("JWT").done().build();
    }

    private KeyPair createTestKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private DataDecryptionKey validDataDecryptionKey() throws Exception {
        return new SecretBuilder()
                .secretKey(keyPair.getPrivate())
                .expiresAt(Instant.MAX)
                .build(RP_ID_TOKEN_DECRYPTION);
    }

    private DataDecryptionKey invalidDataDecryptionKey() throws Exception {
        return new SecretBuilder()
                .secretKey(createTestKeyPair().getPrivate())
                .expiresAt(Instant.MAX)
                .build(RP_ID_TOKEN_DECRYPTION);
    }
}