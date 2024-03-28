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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SecretHmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
@RunWith(MockitoJUnitRunner.class)
public class PersistentJwtStringSupplierTest {

    public static final Purpose<DataEncryptionKey> DATA_ENCRYPTION_KEY_PURPOSE =
            Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataEncryptionKey.class);
    private static final Purpose<DataDecryptionKey> PCOOKIE_NODES_DECRYPTION_PURPOSE =
            Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
    @Mock
    AuthKeyFactory authKeyFactory;

    @Mock
    AMKeyProvider amKeyProvider;

    @Mock
    SecretReferenceCache secretReferenceCache;

    @Mock
    RealmLookup realmLookup;

    private PersistentJwtStringSupplier persistentJwtStringSupplier;

    private KeyPair keyPair1;

    private KeyPair keyPair2;

    @Mock
    private SigningKey signingKey;

    @Mock
    private Signature signature;

    @Mock
    private VerificationKey verificationKey;

    @Mock
    private SecretCache realmCache;
    @Mock
    private SecretReference<SigningKey> signingKeyReference;
    @Mock
    private ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference;
    @Mock
    private SigningManager signingManager;

    @Before
    public void before() throws Exception {
        PersistentJwtProvider persistentJwtProvider = new PersistentJwtProvider(authKeyFactory,
                new JwtReconstruction(), amKeyProvider, realmLookup, secretReferenceCache, signingManager);
        persistentJwtStringSupplier = new PersistentJwtStringSupplier(new JwtBuilderFactory(),
                persistentJwtProvider, authKeyFactory, amKeyProvider, secretReferenceCache, realmLookup,
                signingManager);
        keyPair1 = getRsaKeyPair();
        keyPair2 = getRsaKeyPair();
        given(secretReferenceCache.realm(any())).willReturn(realmCache);
        given(verificationKey.allowsAlgorithm(any())).willReturn(true);
        given(verificationKey.getSignature(any())).willReturn(signature);
        given(signature.verify(any())).willReturn(true);
        given(signingManager.newSigningHandler(any(SigningKey.class)))
                .willReturn(mock(SigningHandler.class));
    }

    @Test
    public void testCreateJwtStringWithNullHmacKey() {
        assertThatThrownBy(() -> persistentJwtStringSupplier.createJwtString("orgname",
                getEmptyAuthContext(), 40, 20, null, null))
                .isInstanceOfSatisfying(NullPointerException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("Signing key must not be null"));
    }

    @Test
    public void testCreateJwtStringWithEmptyEncryptionKeyAndEmptyDeprecatedEncryptionKey() {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        // When
        assertThatThrownBy(() -> persistentJwtStringSupplier
                .createJwtString("orgName", getEmptyAuthContext(), 40, 20, signingKey,
                        "test-kid"))
                .isInstanceOf(InvalidPersistentJwtException.class)
                .hasMessage("No encryption key found for realm: orgName");
    }

    @Test
    public void testCreateJwtStringWithEmptyEncryptionKeyAndPresentDeprecatedEncryptionKey() throws Exception {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenReturn(keyPair1.getPublic());

        // When
        String jwtString = persistentJwtStringSupplier
                .createJwtString("orgname", getEmptyAuthContext(), 40, 20, signingKey,
                        "test-kid");

        // Then
        assertThat(jwtString).isNotNull();

        EncryptedThenSignedJwt reconstructedJwt = new JwtReconstruction()
                .reconstructJwt(jwtString, EncryptedThenSignedJwt.class);

        reconstructedJwt.decrypt(keyPair1.getPrivate());
        JwtClaimsSet jwtClaimsSet = reconstructedJwt.getClaimsSet();

        assertThat(jwtClaimsSet).isNotNull();
    }

    @Test
    public void testCreateJwtStringWithPresentEncryptionKeyAndEmptyDeprecatedEncryptionKey() throws Exception {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.constant(
                        new DataEncryptionKey(new SecretBuilder().stableId("stableId")
                                                      .expiresAt(Instant.MAX).publicKey(keyPair2.getPublic()))));
        // When
        String jwtString = persistentJwtStringSupplier
                .createJwtString("orgname", getEmptyAuthContext(), 40, 20, signingKey,
                        "test-kid");

        // Then
        assertThat(jwtString).isNotNull();

        EncryptedThenSignedJwt reconstructedJwt = new JwtReconstruction()
                .reconstructJwt(jwtString, EncryptedThenSignedJwt.class);

        reconstructedJwt.decrypt(keyPair2.getPrivate());
        JwtClaimsSet jwtClaimsSet = reconstructedJwt.getClaimsSet();

        assertThat(jwtClaimsSet).isNotNull();
    }

    @Test
    public void testCreateJwtStringWithPresentEncryptionKeyAndPresentDeprecatedEncryptionKey() throws Exception {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.constant(
                        new DataEncryptionKey(new SecretBuilder().stableId("stableId")
                                                      .expiresAt(Instant.MAX).publicKey(keyPair2.getPublic()))));

        // When
        String jwtString = persistentJwtStringSupplier
                .createJwtString("orgname", getEmptyAuthContext(), 40, 20, signingKey,
                        "test-kid");

        // Then
        assertThat(jwtString).isNotNull();

        EncryptedThenSignedJwt reconstructedJwt = new JwtReconstruction()
                .reconstructJwt(jwtString, EncryptedThenSignedJwt.class);

        PrivateKey privateKey = keyPair1.getPrivate();
        assertThatThrownBy(() -> reconstructedJwt.decrypt(privateKey))
                .isInstanceOf(JweDecryptionException.class).hasMessage("Decryption failed");
        reconstructedJwt.decrypt(keyPair2.getPrivate());
        JwtClaimsSet jwtClaimsSet = reconstructedJwt.getClaimsSet();

        assertThat(jwtClaimsSet).isNotNull();
    }

    @Test
    public void testGetUpdatedJwtWithNullJwtCookie() throws Exception {
        Jwt jwt = persistentJwtStringSupplier.getUpdatedJwt(null,
                "orgName", signingKeyReference, verificationKeysReference, 10);
        assertThat(jwt).isNull();
    }

    @Test
    public void testGetUpdatedJwtWithJwtCookie() throws Exception {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        given(realmCache.namedOrValid(eq(PCOOKIE_NODES_DECRYPTION_PURPOSE), any()))
                .willReturn(ValidSecretsReference.valid(new SecretsProvider(), PCOOKIE_NODES_DECRYPTION_PURPOSE,
                        FIXED_CLOCK));
        Promise promise = mock(Promise.class);
        given(signingManager.newVerificationHandler(verificationKeysReference)).willReturn(promise);
        given(promise.getOrThrowIfInterrupted()).willReturn(new MySigningHandler());
        given(signingKeyReference.get()).willReturn(signingKey);
        given(signingKey.getStableId()).willReturn("newActiveKeyId");
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenReturn(keyPair1.getPublic());
        when(authKeyFactory.getPrivateAuthKey(any(), any())).thenReturn(keyPair1.getPrivate());

        // When
        String jwtString = persistentJwtStringSupplier
                .createJwtString("orgname", getEmptyAuthContext(), 40, 20,
                        signingKey, "test-kid");

        Date dateInFuture = Time.newDate(Time.newDate().getTime() + 60000);
        Jwt jwt;

        try (MockedStatic<Time> time = Mockito.mockStatic(Time.class)) {
            time.when(Time::newDate).thenReturn(dateInFuture);
            jwt = persistentJwtStringSupplier.getUpdatedJwt(jwtString,
                    "orgName", signingKeyReference, verificationKeysReference, 10);
        }

        // Then
        assertThat(jwt).isNotNull();
        assertThat(jwt.getHeader().get("kid").asString()).isEqualTo("newActiveKeyId");
        assertThat(jwt).isInstanceOf(EncryptedThenSignedJwt.class);
        String newJwtString = jwt.build();
        EncryptedThenSignedJwt newReconstructedJwt = new JwtReconstruction()
                .reconstructJwt(newJwtString, EncryptedThenSignedJwt.class);
        boolean signatureIsVerified = newReconstructedJwt.verify(new SecretHmacSigningHandler(verificationKey));
        assertThat(signatureIsVerified).isTrue();
    }

    @Test
    public void testBuildEncryptedJwtStringCatchesFileNotFoundException() throws Exception {
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new FileNotFoundException());
        assertThrowsInvalidJwtException(() -> persistentJwtStringSupplier.createJwtString(
                "orgname", getEmptyAuthContext(), 40, 20, signingKey, null));

    }

    @Test
    public void testBuildEncryptedJwtStringCatchesSMSException() throws Exception {
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new SMSException());
        assertThrowsInvalidJwtException(() -> persistentJwtStringSupplier.createJwtString(
                "orgname", getEmptyAuthContext(), 40, 20, signingKey, null));

    }

    @Test
    public void testBuildEncryptedJwtStringCatchesSSOException() throws Exception {
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new SSOException("test exception"));
        assertThrowsInvalidJwtException(() -> persistentJwtStringSupplier.createJwtString(
                "orgname", getEmptyAuthContext(),
                40, 20, signingKey, null));
    }

    @Test
    public void testCreateJwtStringSetsKidOnJwt() throws Exception {
        // Given
        given(realmCache.active(DATA_ENCRYPTION_KEY_PURPOSE))
                .willAnswer(a -> SecretReference.active(new SecretsProvider(), DATA_ENCRYPTION_KEY_PURPOSE,
                        FIXED_CLOCK));
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenReturn(keyPair1.getPublic());
        // When
        String jwtString = persistentJwtStringSupplier.createJwtString("orgname", getEmptyAuthContext(),
                40, 20, signingKey, "test-kid");

        // Then
        String[] jwtParts = jwtString.split("\\.");
        String jwtHeader = jwtParts[0];
        String decodedHeader = new String(Base64url.decode(jwtHeader));
        assertThat(decodedHeader).contains("\"kid\":\"test-kid\"");
    }

    private static KeyPair getRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        return keyPairGenerator.generateKeyPair();
    }

    private Map<String, String> getEmptyAuthContext() {
        return new HashMap<>();
    }

    private void assertThrowsInvalidJwtException(ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(InvalidPersistentJwtException.class);
    }

    private static class MySigningHandler implements SigningHandler {
        @Override
        public byte[] sign(JwsAlgorithm jwsAlgorithm, byte[] bytes) {
            return new byte[0];
        }

        @Override
        public boolean verify(JwsAlgorithm jwsAlgorithm, byte[] bytes, byte[] bytes1) {
            return true;
        }
    }
}
