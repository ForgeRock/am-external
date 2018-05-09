/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ChallengeTest {
    private static final String AUDIENCE = "testAudience";
    private static final Date ISSUED_AT = new Date(123456789000L);
    private static final Date EXPIRES_AT = new Date(Long.MAX_VALUE);
    private static final String ORG_DN = "someOrgDn";
    private static final String NONCE = "a completely unguessable nonce";

    private static final String SIGNING_KEY_ID = "signingKeyId";

    private JWK challengeSigningKey;
    private JWK responseEncryptionKey;
    private Challenge challenge;

    @BeforeClass
    public void generateKeys() throws Exception {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(SupportedEllipticCurve.P256.getParameters());
        final KeyPair signingKey = keyPairGenerator.generateKeyPair();

        challengeSigningKey = new EcJWK((ECPublicKey) signingKey.getPublic(), (ECPrivateKey) signingKey.getPrivate(),
                KeyUse.SIG, SIGNING_KEY_ID);

        // Just reuse the signing key for testing purposes
        responseEncryptionKey =  new EcJWK((ECPublicKey) signingKey.getPublic(), (ECPrivateKey) signingKey.getPrivate(),
                KeyUse.ENC, null);
    }

    @BeforeMethod
    public void setup() throws Exception {
        challenge = Challenge.builder()
                .audience(AUDIENCE)
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .orgDn(ORG_DN)
                .nonce(NONCE)
                .signingKey(challengeSigningKey)
                .responseEncryptionKey(responseEncryptionKey)
                .build();
    }

    @Test
    public void shouldIncludeCorrectClaims() throws Exception {
        final JwtClaimsSet claims = challenge.toJwt().getClaimsSet();

        // The subject field should never be set, as we are not making a claim that the subject is who they say they are
        // Failing to respect this could lead to our challenges being valid signed OIDC id tokens.
        assertThat(claims.getSubject()).isNull();
        assertThat(claims.getIssuer()).isEqualTo(ORG_DN);
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        assertThat(claims.getIssuedAtTime()).isEqualToIgnoringMillis(ISSUED_AT);
        assertThat(claims.getExpirationTime()).isEqualToIgnoringMillis(EXPIRES_AT);
        assertThat(claims.getClaim(Challenge.VERIFIER_NONCE_CLAIM)).isEqualTo(NONCE);
    }

    @Test
    public void shouldIndicateCorrectSigningAlgorithm() throws Exception {
        assertThat(challenge.toJwt().getHeader().getAlgorithm()).hasToString(challengeSigningKey.getAlgorithm());
    }

    @Test
    public void shouldIncludeChallengeSigningKeyId() throws Exception {
        assertThat(challenge.toJwt().getHeader().getKeyId()).isEqualTo(SIGNING_KEY_ID);
    }

    @Test
    public void shouldIncludeResponseEncryptionKeyIfEphemeral() throws Exception {
        final EcJWK epk = EcJWK.parse(challenge.toJwt().getClaimsSet().get("response_enc_epk"));
        assertThat(epk).isNotNull();
        assertThat(epk.getD()).isNull(); // Should not include private key components!
        assertThat(epk.getUse()).isEqualTo(KeyUse.ENC);
        assertThat(epk.getCurve()).isEqualTo("P-256");
        assertThat(epk.getX()).isNotBlank();
        assertThat(epk.getY()).isNotBlank();
    }

    @Test
    public void shouldIncludeResponseEncryptionKeyIdIfNotEphemeral() throws Exception {
        // Given
        String keyId = "someKeyId";
        JWK responseEncryptionKey = new OctJWK(KeyUse.ENC, null, keyId, "key_bytes", null, null, null);
        Challenge challenge = Challenge.builder()
                .orgDn(ORG_DN)
                .audience(AUDIENCE)
                .nonce(NONCE)
                .signingKey(challengeSigningKey)
                .responseEncryptionKey(responseEncryptionKey)
                .build();

        assertThat(challenge.toJwt().getClaimsSet().getClaim("response_enc_kid")).isEqualTo(keyId);
    }
}