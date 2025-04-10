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

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtProvider;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sun.identity.idm.IdType;
import com.sun.identity.shared.encode.Base64;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersistentCookieDecisionNodeTest {

    private static final String COOKIE_VALUE = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload";

    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private Realm realm;
    @Mock
    private PersistentJwtProvider persistentJwtProvider;
    @Mock
    private PersistentJwtClaimsHandler persistentJwtClaimsHandler;
    @Mock
    private EncryptedThenSignedJwt mockJwt;
    @RegisterExtension
    public LoggerExtension loggerExtension = new LoggerExtension(PersistentCookieDecisionNode.class);

    private PersistentCookieDecisionNode persistentCookieDecisionNode;
    private static final Purpose<SigningKey> NODE_DEFINED_SIGNING_PURPOSE = Purpose.purpose("nodeDefinedPurpose",
            SigningKey.class);
    private static final Purpose<VerificationKey> NODE_DEFINED_VERIFICATION_PURPOSE =
            Purpose.purpose("nodeDefinedPurpose", VerificationKey.class);
    private final PersistentCookieDecisionNode.Config config = new PersistentCookieDecisionNodeTest
            .TestConfigWithSigningKey();
    @Mock
    private ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference;

    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private SecretCache secretCache;

    @BeforeEach
    void setup() throws InvalidPersistentJwtException {
        given(secretReferenceCache.realm(realm)).willReturn(secretCache);
        persistentCookieDecisionNode = new PersistentCookieDecisionNode(config, coreWrapper, identityService,
                UUID.randomUUID(), realm,
                persistentJwtProvider, persistentJwtClaimsHandler, secretReferenceCache);
        given(secretCache.namedOrValid(any(), any()))
                .willAnswer(inv -> verificationKeysReference);
        given(coreWrapper.convertRealmPathToRealmDn(any())).willReturn("ou=config");
        given(persistentJwtProvider
                .getValidDecryptedJwt(COOKIE_VALUE, "ou=config", verificationKeysReference))
                .willReturn(mockJwt);
    }

    @Test
    void givenUsernameFoundInCookieThenUserIsIdentified()
            throws NodeProcessException, InvalidPersistentJwtException {
        //given
        given(persistentJwtClaimsHandler.getUsername(any(), any())).willReturn("demo");
        ExternalRequestContext externalRequestContext = new ExternalRequestContext.Builder()
                .cookies(Map.of("session-jwt", COOKIE_VALUE)).build();

        // when
        Action action = persistentCookieDecisionNode.process(new TreeContext(json(object()),
                externalRequestContext, List.of(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(action.identifiedIdentity).contains(new IdentifiedIdentity("demo", IdType.USER));
    }

    @Test
    void givenASigningKeyPurposeIsProvidedThenUserIsIdentified()
            throws InvalidPersistentJwtException, NodeProcessException {

        //given
        given(persistentJwtClaimsHandler.getUsername(any(), any())).willReturn("demo");
        ExternalRequestContext externalRequestContext = new ExternalRequestContext.Builder()
                .cookies(Map.of("session-jwt", COOKIE_VALUE))
                .build();

        //when
        Action action = persistentCookieDecisionNode.process(new TreeContext(json(object()),
                externalRequestContext, List.of(), Optional.empty()));

        //then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(action.identifiedIdentity).contains(new IdentifiedIdentity("demo", IdType.USER));
    }

    @Test
    void givenNoCookieFoundThenNoUserIsIdentified() throws NodeProcessException {
        //given
        ExternalRequestContext externalRequestContext = new ExternalRequestContext.Builder()
                                                                .build();

        // when
        Action action = persistentCookieDecisionNode.process(new TreeContext(json(object()),
                externalRequestContext, List.of(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("false");
        assertThat(action.identifiedIdentity).isEmpty();
    }

    @Test
    void givenKidOnJwtUsesKidToRetrieveKeys() throws InvalidPersistentJwtException, NodeProcessException {
        // Given
        String header = "{\"kid\":\"test-kid\"}";
        String encodedHeader = Base64.encode(header.getBytes(StandardCharsets.UTF_8));
        String jwtString = encodedHeader + "." + "payload";
        ExternalRequestContext externalRequestContext =
                new ExternalRequestContext.Builder().cookies(Map.of("session-jwt", jwtString)).build();
        given(persistentJwtProvider.getValidDecryptedJwt(jwtString, "ou=config", verificationKeysReference))
                .willReturn(mockJwt);
        given(persistentJwtClaimsHandler.getUsername(any(), any())).willReturn("demo");

        // When
        persistentCookieDecisionNode.process(
                new TreeContext(json(object()),
                        externalRequestContext, List.of(), Optional.empty()));

        // Then
        verify(secretCache).namedOrValid(any(), eq("test-kid"));
        verify(persistentJwtProvider).getValidDecryptedJwt(jwtString, "ou=config", verificationKeysReference);
    }


    private static class TestConfigWithSigningKey implements PersistentCookieDecisionNode.Config {

        @Override
        public Optional<char[]> hmacSigningKey() {
            return Optional.empty();
        }

        @Override
        public Optional<Purpose<SigningKey>> signingKeyPurpose() {
            return Optional.of(NODE_DEFINED_SIGNING_PURPOSE);
        }
    }

}
