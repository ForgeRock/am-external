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
 * Copyright 2017-2019 ForgeRock AS.
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
import org.forgerock.json.jose.jwk.KeyUseConstants;
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
                KeyUseConstants.SIG, SIGNING_KEY_ID);

        // Just reuse the signing key for testing purposes
        responseEncryptionKey =  new EcJWK((ECPublicKey) signingKey.getPublic(), (ECPrivateKey) signingKey.getPrivate(),
                KeyUseConstants.ENC, null);
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
        assertThat(epk.getUse()).isEqualTo(KeyUseConstants.ENC);
        assertThat(epk.getCurve()).isEqualTo("P-256");
        assertThat(epk.getX()).isNotBlank();
        assertThat(epk.getY()).isNotBlank();
    }

    @Test
    public void shouldIncludeResponseEncryptionKeyIdIfNotEphemeral() throws Exception {
        // Given
        String keyId = "someKeyId";
        JWK responseEncryptionKey = new OctJWK(KeyUseConstants.ENC, null, keyId, "key_bytes", null, null, null);
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