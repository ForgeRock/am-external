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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.script;

import static com.sun.identity.shared.Constants.LEGACY_JWT_VALIDATION;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.script.JwtAssertionScriptWrapper.JwtBuilderType.ENCRYPTED_THEN_SIGNED;
import static org.forgerock.openam.auth.nodes.script.JwtAssertionScriptWrapper.JwtBuilderType.SIGNED;
import static org.forgerock.openam.auth.nodes.script.JwtAssertionScriptWrapper.JwtBuilderType.SIGNED_THEN_ENCRYPTED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.script.JwtAssertionScriptWrapper.JwtBuilderType;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.KeyEncryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.encode.Base64;

class JwtValidatorScriptWrapperTest {

    private JwtValidatorScriptWrapper jwtValidatorScriptWrapper;
    private JwtAssertionScriptWrapper jwtAssertionScriptWrapper;
    private MockedStatic<SystemProperties> systemProperties;

    @BeforeEach
    void setUp() {
        jwtAssertionScriptWrapper = new JwtAssertionScriptWrapper();
        jwtValidatorScriptWrapper = new JwtValidatorScriptWrapper();
        systemProperties = mockStatic(SystemProperties.class);
    }

    @AfterEach
    void tearDown() {
        systemProperties.close();
    }

    @Test
    void testCreateAndValidateRSASigned() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK publicRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic()).build();
        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, SIGNED);

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt,
                Base64.encode(publicRSAJwk.toRSAPublicKey().getEncoded()),
                SIGNED);

        // Then
        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    @Test
    void testShouldFailToValidateRSASignedJwtWithInvalidKey() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, SIGNED);

        // When
        assertThatThrownBy(() -> validateJwt(jwt,
                Base64.encode("invalid key which should fail validation".getBytes()),
                SIGNED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid RSA public key");
    }

    @Test
    void testShouldFailToValidateSignedJwtWithExpiredJwt() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK publicRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic()).build();
        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, SIGNED, -1);

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt,
                Base64.encode(publicRSAJwk.toRSAPublicKey().getEncoded()),
                SIGNED);

        // Then
        assertNull(validatedJwtClaims);
    }

    @Test
    void testCreateRSASignedThenEncrypted() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        // When
        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, SIGNED_THEN_ENCRYPTED,
                Base64.encode(encryptionKey.getBytes()));

        // Then
        assertNull(jwt);
    }

    @Test
    void testCreateRSAEncryptedThenSigned() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        // When
        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, ENCRYPTED_THEN_SIGNED,
                Base64.encode(encryptionKey.getBytes()));

        // Then
        assertNull(jwt);
    }

    @Test
    void testValidateRSAEncryptedThenSigned() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK publicRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic()).build();
        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();
        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        KeyEncryptionKey encryptedKey = new KeyEncryptionKey(new SecretBuilder()
                .stableId("my-stable-id")
                .expiresAt(Instant.now().plus(1, DAYS))
                .secretKey(new SecretKeySpec(encryptionKey.getBytes(), "AES")));
        JwtClaimsSet jwtClaims = new JwtClaimsSetBuilder().claims(
                object(
                        field("iss", "myiss"),
                        field("sub", "mysub"),
                        field("aud", "myaud"),
                        field("exp", Instant.now().plus(1, DAYS).getEpochSecond()),
                        field("iat", Instant.now().getEpochSecond())
                )
        ).build();
        SigningHandler signingHandler = new SecretRSASigningHandler(new SigningKey(
                new SecretBuilder()
                        .stableId("my-stable-id")
                        .expiresAt(Instant.now().plus(1, DAYS))
                        .secretKey(privateRSAJwk.toRSAPrivateKey())));
        String jwt = new JwtBuilderFactory()
                .jwe(encryptedKey)
                .headers()
                .alg(JweAlgorithm.DIRECT)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims(jwtClaims)
                .signedWith(signingHandler, JwsAlgorithm.RS256)
                .build();

        // When
        Map<String, Object> validatedJwtClaims =
                validateJwt(jwt, Base64.encode(publicRSAJwk.toRSAPublicKey().getEncoded()), ENCRYPTED_THEN_SIGNED,
                        Base64.encode(encryptionKey.getBytes()));

        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }


    @Test
    void testValidateRSASignedThenEncrypted() throws Exception {
        // Given
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK publicRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic()).build();
        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();
        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        KeyEncryptionKey encryptedKey = new KeyEncryptionKey(new SecretBuilder()
                .stableId("my-stable-id")
                .expiresAt(Instant.now().plus(1, DAYS))
                .secretKey(new SecretKeySpec(encryptionKey.getBytes(), "AES")));
        JwtClaimsSet jwtClaims = new JwtClaimsSetBuilder().claims(
                object(
                        field("iss", "myiss"),
                        field("sub", "mysub"),
                        field("aud", "myaud"),
                        field("exp", Instant.now().plus(1, DAYS).getEpochSecond()),
                        field("iat", Instant.now().getEpochSecond())
                )
        ).build();
        SigningHandler signingHandler = new SecretRSASigningHandler(new SigningKey(
                new SecretBuilder()
                        .stableId("my-stable-id")
                        .expiresAt(Instant.now().plus(1, DAYS))
                        .secretKey(privateRSAJwk.toRSAPrivateKey())));
        String jwt = new JwtBuilderFactory()
                .jws(signingHandler)
                .headers()
                .alg(JwsAlgorithm.RS256)
                .done()
                .encrypt(encryptedKey)
                .headers()
                .alg(JweAlgorithm.DIRECT)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims(jwtClaims)
                .build();

        // When
        Map<String, Object> validatedJwtClaims =
                validateJwt(jwt, Base64.encode(publicRSAJwk.toRSAPublicKey().getEncoded()), SIGNED_THEN_ENCRYPTED,
                        Base64.encode(encryptionKey.getBytes()));

        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    @Test
    void testCreateAndValidateHMACSigned() throws Exception {
        // Given
        String secret = "my-secret-key";

        String jwt = generateJwt(Base64.encode(secret.getBytes()), JwsAlgorithm.HS256, SIGNED);

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt, Base64.encode(secret.getBytes()), SIGNED);

        // Then
        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    @Test
    void testCreateAndValidateHMACSignedThenEncrypted() throws Exception {
        // Given
        String secret = "my-secret-key";
        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        String jwt = generateJwt(Base64.encode(secret.getBytes()), JwsAlgorithm.HS256, SIGNED_THEN_ENCRYPTED,
                Base64.encode(encryptionKey.getBytes()));

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt, Base64.encode(secret.getBytes()),
                SIGNED_THEN_ENCRYPTED, Base64.encode(encryptionKey.getBytes()));

        // Then
        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    @Test
    void testCreateAndValidateHMACEncryptedThenSigned() throws Exception {
        // Given
        String secret = "my-secret-key";
        String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        String jwt = generateJwt(Base64.encode(secret.getBytes()), JwsAlgorithm.HS256, ENCRYPTED_THEN_SIGNED,
                Base64.encode(encryptionKey.getBytes()));

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt, Base64.encode(secret.getBytes()),
                ENCRYPTED_THEN_SIGNED, Base64.encode(encryptionKey.getBytes()));

        // Then
        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    @Test
    void testLegacyBehaviour() throws Exception {
        // Given
        systemProperties.when(() -> SystemProperties.getAsBoolean(LEGACY_JWT_VALIDATION, false))
                .thenReturn(true);
        KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");
        keyPairG.initialize(2048);
        KeyPair keyPair = keyPairG.generateKeyPair();

        RsaJWK privateRSAJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("iEU-")
                .keyUse("sig")
                .rsaPrivateKey((RSAPrivateKey) keyPair.getPrivate()).build();

        String jwt = generateJwt(privateRSAJwk.toJsonValue().asMap(), JwsAlgorithm.RS256, SIGNED);

        KeyPair differentKeyPair = keyPairG.generateKeyPair();
        RsaJWK differentPublicRSAJwk = RsaJWK.builder((RSAPublicKey) differentKeyPair.getPublic()).build();

        // When
        Map<String, Object> validatedJwtClaims = validateJwt(jwt,
                Base64.encode(differentPublicRSAJwk.toRSAPublicKey().getEncoded()),
                SIGNED);

        // Then
        verifySuccessfulValidateJwtResults(validatedJwtClaims);
    }

    private String generateJwt(Object secretKey, JwsAlgorithm jwsAlgorithm, JwtBuilderType jwtType)
            throws NoSuchSecretException {
        return generateJwt(secretKey, jwsAlgorithm, jwtType, null);
    }

    private String generateJwt(Object secretKey, JwsAlgorithm jwsAlgorithm, JwtBuilderType jwtType,
                               String encryptionKey) throws NoSuchSecretException {
        return generateJwt(secretKey, jwsAlgorithm, jwtType, encryptionKey, 10);
    }

    private String generateJwt(Object secretKey, JwsAlgorithm jwsAlgorithm, JwtBuilderType jwtType,
                               int validityMinutes) throws NoSuchSecretException {
        return generateJwt(secretKey, jwsAlgorithm, jwtType, null, validityMinutes);
    }

    private String generateJwt(Object secretKey, JwsAlgorithm jwsAlgorithm, JwtBuilderType jwtType,
            String encryptionKey, int validityMinutes) throws NoSuchSecretException {
        return jwtAssertionScriptWrapper.generateJwt(json(object(
                field("jwtType", jwtType),
                field("jwsAlgorithm", jwsAlgorithm),
                field("issuer", "myiss"),
                field("subject", "mysub"),
                field("audience", "myaud"),
                field("type", "JWT"),
                field("validityMinutes", validityMinutes),
                field(jwsAlgorithm == JwsAlgorithm.HS256 ? "signingKey" : "privateKey", secretKey),
                fieldIfNotNull("encryptionKey", encryptionKey)))
                .asMap());
    }

    private Map<String, Object> validateJwt(String jwt, String signingKey, JwtBuilderType jwtType)
            throws NoSuchSecretException {
        return validateJwt(jwt, signingKey, jwtType, null);
    }

    private Map<String, Object> validateJwt(String jwt, String signingKey, JwtBuilderType jwtType, String encryptionKey)
            throws NoSuchSecretException {
        return jwtValidatorScriptWrapper.validateJwtClaims(json(object(
                field("jwtType", jwtType),
                field("jwt", jwt),
                field("accountId", "myiss"),
                field("audience", "myaud"),
                field("signingKey", signingKey),
                fieldIfNotNull("encryptionKey", encryptionKey)
        )).asMap());
    }

    private void verifySuccessfulValidateJwtResults(Map<String, Object> validatedJwtClaims) {
        assertNotNull(validatedJwtClaims);
        assertThat(validatedJwtClaims).containsKeys("issuer", "subject", "audience", "expirationTime", "issuedAt");
    }
}
