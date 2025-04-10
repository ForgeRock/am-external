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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.RequestCookies;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.PersistentCookieDecisionNode;
import org.forgerock.openam.auth.nodes.jwt.JwtHeaderUtilities;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.forgerock.openam.auth.nodes.utils.SecretsAndKeyUtils;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.KeyUsage;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdatePersistentCookieTreeHookTest {

    private static final String DEFAULT_COOKIE_NAME = "session-jwt";
    private final PersistentCookieDecisionNode.Config config = new TestConfigWithSigningKey();
    private static final Purpose<SigningKey> NODE_DEFINED_SIGNING_PURPOSE =
            Purpose.purpose("nodeDefinedPurpose", SigningKey.class);
    private static final Purpose<VerificationKey> NODE_DEFINED_VERIFICATION_PURPOSE =
            Purpose.purpose("nodeDefinedPurpose", VerificationKey.class);
    private static final String CONFIG_SIGNING_KEY = "configSigningKey";
    private static final String JWT_STRING = "jwtString";
    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    PersistentJwtStringSupplier persistentJwtStringSupplier;
    @Mock
    PersistentCookieResponseHandler persistentCookieResponseHandler;
    @Mock
    private Realm realm;
    private UpdatePersistentCookieTreeHook treeHook;
    @Mock
    private SigningKey signingKey;
    @Mock
    private SecretCache realmSecretsCache;
    @Mock
    private VerificationKey verificationKey;
    @Mock
    private SecretReferenceCache secretsReferenceCache;

    @BeforeEach
    void setUp() throws Exception {
        given(secretsReferenceCache.realm(realm)).willReturn(realmSecretsCache);
        treeHook = new UpdatePersistentCookieTreeHook(request, response, config, realm, persistentJwtStringSupplier,
                persistentCookieResponseHandler, secretsReferenceCache);
        Cookie mockCookie = mock(Cookie.class);
        given(mockCookie.getName()).willReturn(DEFAULT_COOKIE_NAME);
        RequestCookies requestCookies = mock(RequestCookies.class);
        given(requestCookies.get(DEFAULT_COOKIE_NAME)).willReturn(List.of(mockCookie));
        given(request.getCookies()).willReturn(requestCookies);
        given(requestCookies.containsKey(DEFAULT_COOKIE_NAME)).willReturn(true);
        var mockJwt = mock(SignedJwt.class);
        var mockClaimsSet = mock(JwtClaimsSet.class);
        given(mockJwt.getClaimsSet()).willReturn(mockClaimsSet);
        given(persistentJwtStringSupplier.getUpdatedJwt(any(), any(), any(), any(),
                anyLong())).willReturn(mockJwt);
        given(mockJwt.build()).willReturn(JWT_STRING);
    }

    @Test
    void testSetCookieOnResponseCalledWithNoKid() throws Exception {
        // Given
        given(realmSecretsCache.active(NODE_DEFINED_SIGNING_PURPOSE))
                .willReturn(SecretReference.constant(getTestSigningKey()));
        ValidSecretsReference<VerificationKey, NeverThrowsException> secretsReference =
                new SecretsProvider(Time.getClock())
                .useSpecificSecretForPurpose(NODE_DEFINED_VERIFICATION_PURPOSE, getTestVerificationKey())
                .createValidReference(NODE_DEFINED_VERIFICATION_PURPOSE);

        // When
        try (MockedStatic<PersistentCookieResponseHandler> h =
                     Mockito.mockStatic(PersistentCookieResponseHandler.class);
             MockedStatic<JwtHeaderUtilities> j = Mockito.mockStatic(JwtHeaderUtilities.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            j.when(() -> JwtHeaderUtilities.getHeader(any(), any())).thenReturn(Optional.empty());
            treeHook.accept();
        }

        // Then
        verify(persistentCookieResponseHandler).setCookieOnResponse(response, request, config.persistentCookieName(),
                JWT_STRING, null, config.useSecureCookie(), config.useHttpOnlyCookie());
    }

    @Test
    void testSetCookieOnResponseCalledWithKid() throws Exception {
        // Given

        ValidSecretsReference<VerificationKey, NeverThrowsException> secretsReference =
                new SecretsProvider(Time.getClock())
                        .useSpecificSecretForPurpose(NODE_DEFINED_VERIFICATION_PURPOSE, verificationKey)
                        .createValidReference(NODE_DEFINED_VERIFICATION_PURPOSE);

        given(realmSecretsCache.namedOrValid(config.verificationKeyPurpose().get(), "test-kid"))
                .willReturn(secretsReference);

        // When
        try (MockedStatic<PersistentCookieResponseHandler> h =
                     Mockito.mockStatic(PersistentCookieResponseHandler.class);
             MockedStatic<JwtHeaderUtilities> j = Mockito.mockStatic(JwtHeaderUtilities.class)) {
            h.when(() -> PersistentCookieResponseHandler.getOrgName(response)).thenReturn("orgName");
            j.when(() -> JwtHeaderUtilities.getHeader(any(), any())).thenReturn(Optional.of("test-kid"));
            treeHook.accept();
        }

        // Then
        verify(persistentCookieResponseHandler).setCookieOnResponse(response, request, config.persistentCookieName(),
                JWT_STRING, null, config.useSecureCookie(), config.useHttpOnlyCookie());
    }

    private SigningKey getTestSigningKey() throws NoSuchSecretException {
        return new SigningKey(SecretsAndKeyUtils.getSecretBuilder(
                "my-stable-id", "test-key".getBytes(), KeyUsage.SIGN, "HMAC"));
    }

    private VerificationKey getTestVerificationKey() throws NoSuchSecretException {
        return new VerificationKey(SecretsAndKeyUtils.getSecretBuilder(
                "my-stable-id", "test-key".getBytes(), KeyUsage.VERIFY, "HMAC"));
    }

    private static class TestConfigWithSigningKey implements PersistentCookieDecisionNode.Config {
        @Override
        public Optional<char[]> hmacSigningKey() {
            return Optional.of(CONFIG_SIGNING_KEY.toCharArray());
        }

        @Override
        public Optional<Purpose<SigningKey>> signingKeyPurpose() {
            return Optional.of(NODE_DEFINED_SIGNING_PURPOSE);
        }
    }
}
