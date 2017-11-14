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
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.forgerock.util.Reject.checkNotNull;

import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64url;

import com.sun.identity.authentication.spi.HttpCallback;

/**
 * A proof of possession challenge.
 */
final class Challenge {
    static final int NONCE_LENGTH = 16; // 128-bit nonces

    /**
     * Name of the claim that contains the random challengeNonce contributed by the verifier (OpenAM).
     */
    static final String VERIFIER_NONCE_CLAIM = "rvn";
    /**
     * Name of the claim that contains the random challengeNonce contributed by the prover (subject).
     */
    static final String PROVER_NONCE_CLAIM = "rpn";

    private static final String AUTH_SCHEME = "JWT-PoP";
    private static final String CHALLENGE_HEADER = "WWW-Authenticate";
    private static final String RESPONSE_HEADER = "Authorization";
    private static final int CHALLENGE_STATUS_CODE = HttpURLConnection.HTTP_UNAUTHORIZED;

    private static final JwtBuilderFactory JWT_BUILDER_FACTORY = new JwtBuilderFactory();
    private static final SigningManager SIGNING_MANAGER = new SigningManager();

    private static final String CHALLENGE_HEADER_FORMAT = AUTH_SCHEME + " realm=\"%s\", challenge=\"%s\"";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String orgDn;
    private final Date issuedAt;
    private final Date expiresAt;
    private final String audience;
    private final String challengeNonce;
    private final JWK challengeSignatureKey;
    private final JWK responseEncryptionKey;
    private final EncryptionMethod responseEncryptionCipher;
    private final Map<String, Object> additionalClaims;

    Challenge(String challengeNonce, String orgDn, Date issuedAt, Date expiresAt, String audience,
              JWK challengeSignatureKey, JWK responseEncryptionKey, EncryptionMethod responseEncryptionCipher,
              Map<String, Object> additionalClaims) {
        this.challengeNonce = checkNotNull(challengeNonce);
        this.orgDn = checkNotNull(orgDn);
        this.issuedAt = checkNotNull(issuedAt);
        this.expiresAt = checkNotNull(expiresAt);
        this.audience = checkNotNull(audience);
        this.responseEncryptionKey = responseEncryptionKey;
        this.responseEncryptionCipher = checkNotNull(responseEncryptionCipher);
        this.challengeSignatureKey = checkNotNull(challengeSignatureKey);
        this.additionalClaims = additionalClaims;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAuthScheme() {
        return AUTH_SCHEME;
    }

    public String getOrganisationDn() {
        return orgDn;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public String getAudience() {
        return audience;
    }

    public String getChallengeNonce() {
        return challengeNonce;
    }

    public JWK getResponseEncryptionKey() {
        return responseEncryptionKey;
    }

    public EncryptionMethod getResponseEncryptionCipher() {
        return responseEncryptionCipher;
    }

    public JWK getChallengeSignatureKey() {
        return challengeSignatureKey;
    }

    public Map<String, Object> getAdditionalClaims() {
        return additionalClaims;
    }

    public SignedJwt toJwt() {
        final JwtClaimsSet claims = JWT_BUILDER_FACTORY.claims()
                .iss(orgDn)
                .aud(Collections.singletonList(audience))
                .iat(issuedAt)
                .exp(expiresAt)
                .claim(VERIFIER_NONCE_CLAIM, challengeNonce)
                .claims(additionalClaims)
                .build();

        if (responseEncryptionKey != null) {
            if (responseEncryptionKey.getJwaAlgorithm() != null) {
                claims.put("response_enc_alg", responseEncryptionKey.getJwaAlgorithm().getJwaAlgorithmName());
                claims.put("response_enc_enc", responseEncryptionCipher.getJweStandardName());
            }
            if (responseEncryptionKey.getKeyType() == KeyType.EC) {
                // Ephemeral ECDH: add the public key as the epk claim
                final EcJWK ephemeralPublicKey = new EcJWK(((EcJWK) responseEncryptionKey).toECPublicKey(),
                        KeyUse.ENC, null);
                claims.put("response_enc_epk", ephemeralPublicKey.toJsonValue().asMap());
            } else if (responseEncryptionKey.getKeyId() != null) {
                claims.put("response_enc_kid", responseEncryptionKey.getKeyId());
            }
        }

        final SigningHandler signingHandler = SIGNING_MANAGER.newSigningHandler(challengeSignatureKey);
        return JWT_BUILDER_FACTORY.jws(signingHandler)
                .headers()
                    .alg(JwsAlgorithm.parseAlgorithm(challengeSignatureKey.getAlgorithm()))
                    .headerIfNotNull("kid", challengeSignatureKey.getKeyId())
                .done()
                .claims(claims)
                .asJwt();
    }

    public HttpCallback toCallback(String realm) {
        final String challengeJwt = toJwt().build();
        final String challengeHeaderValue = String.format(Locale.ROOT, CHALLENGE_HEADER_FORMAT, realm, challengeJwt);

        return new HttpCallback(RESPONSE_HEADER, CHALLENGE_HEADER, challengeHeaderValue, CHALLENGE_STATUS_CODE);
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "orgDn='" + orgDn + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", audience='" + audience + '\'' +
                ", challengeNonce='" + challengeNonce + '\'' +
                ", challengeSignatureKey=" + challengeSignatureKey +
                ", responseEncryptionKey=" + responseEncryptionKey +
                ", additionalClaims=" + additionalClaims +
                '}';
    }

    private static String randomNonce() {
        final byte[] nonce = new byte[NONCE_LENGTH];
        SECURE_RANDOM.nextBytes(nonce);
        return Base64url.encode(nonce);
    }

    static class Builder {
        private String challengeNonce = randomNonce();
        private String orgDn;
        private Date issuedAt = Time.newDate();
        private Date expiresAt = Time.newDate(Time.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30));
        private String audience;
        private JWK challengeSigningKey;
        private JWK responseEncryptionKey;
        private EncryptionMethod responseEncryptionCipher = EncryptionMethod.A128GCM;
        private final Map<String, Object> additionalClaims = new TreeMap<>();

        Builder orgDn(final String orgDn) {
            this.orgDn = checkNotNull(orgDn);
            return this;
        }

        Builder issuedAt(Date issuedAt) {
            this.issuedAt = checkNotNull(issuedAt);
            return this;
        }

        Builder expiresAt(Date expiresAt) {
            this.expiresAt = checkNotNull(expiresAt);
            return this;
        }

        Builder audience(String audience) {
            this.audience = checkNotNull(audience);
            return this;
        }

        public Builder nonce(String nonce) {
            this.challengeNonce = checkNotNull(nonce);
            return this;
        }

        Builder signingKey(JWK signingKey) {
            Reject.ifTrue(signingKey.getUse() != null && signingKey.getUse() != KeyUse.SIG);
            this.challengeSigningKey = signingKey;
            return this;
        }

        Builder responseEncryptionKey(JWK responseEncryptionKey) {
            Reject.ifTrue(responseEncryptionKey.getUse() != null && responseEncryptionKey.getUse() != KeyUse.ENC);
            this.responseEncryptionKey = responseEncryptionKey;
            return this;
        }

        Builder responseEncryptionCipher(EncryptionMethod responseEncryptionCipher) {
            this.responseEncryptionCipher = checkNotNull(responseEncryptionCipher);
            return this;
        }

        Builder claimIfNotNull(String claim, Object value) {
            if (value != null) {
                this.additionalClaims.put(claim, value);
            }
            return this;
        }

        Challenge build() {
            return new Challenge(challengeNonce, orgDn, issuedAt, expiresAt, audience, challengeSigningKey,
                    responseEncryptionKey, responseEncryptionCipher, additionalClaims);
        }
    }
}
