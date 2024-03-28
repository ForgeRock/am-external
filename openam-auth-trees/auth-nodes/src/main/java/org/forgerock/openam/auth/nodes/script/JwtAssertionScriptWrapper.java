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
package org.forgerock.openam.auth.nodes.script;

import java.security.Key;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.SecretHmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SecretSigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.KeyEncryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class to support the generation of JWT assertions within scripts.
 */
@Supported
public class JwtAssertionScriptWrapper {

    private final Logger logger = LoggerFactory.getLogger(JwtAssertionScriptWrapper.class);

    enum JwtBuilderType {
        SIGNED,
        SIGNED_THEN_ENCRYPTED,
        ENCRYPTED_THEN_SIGNED;

        boolean isEncrypted() {
            return this == SIGNED_THEN_ENCRYPTED || this == ENCRYPTED_THEN_SIGNED;
        }
    }

    /**
     * Generates a JWT assertion based on the JWT data provided.
     * <p>
     * Supported jwtData fields are:
     *  <ul>
     *      <li>jwtType</li>
     *      <li>jwsAlgorithm</li>
     *      <li>accountId</li>
     *      <li>audience</li>
     *      <li>validityMinutes</li>
     *      <li>privateKey</li>
     *      <li>signingKey</li>
     *      <li>encryptionKey</li>
     *  </ul>
     * </p>
     *
     * @param jwtData an object containing data to generate the JWT
     * @return a string representation of the JWT
     * @throws NoSuchSecretException if the given key set does not contain the verification key
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String generateJwt(Map<String, Object> jwtData) throws NoSuchSecretException {
        if (!validateJwtData(jwtData.get("jwtType"), jwtData.get("jwsAlgorithm"), jwtData.get("accountId"),
                jwtData.get("audience"))) {
            return null;
        }

        JwtBuilderType jwtBuilderType = JwtBuilderType.valueOf(jwtData.get("jwtType").toString());
        JwsAlgorithm jwsAlgorithm = JwsAlgorithm.valueOf(jwtData.get("jwsAlgorithm").toString());
        String accountId = jwtData.get("accountId").toString();
        String audience = jwtData.get("audience").toString();

        int validity = Double.valueOf(jwtData.getOrDefault("validityMinutes", 0).toString()).intValue();
        JsonValue privateKey = JsonValue.json(jwtData.getOrDefault("privateKey", ""));
        String signingKey = jwtData.getOrDefault("signingKey", "").toString();
        String encryptionKey = jwtData.getOrDefault("encryptionKey", "").toString();

        JwtClaimsSet jwtClaims = createJwtClaimsSet(accountId, audience, validity);

        return buildJwtForAlgorithm(jwtBuilderType, jwsAlgorithm, accountId, privateKey, signingKey, encryptionKey,
                jwtClaims);
    }

    private boolean validateJwtData(Object jwtType, Object jwsAlgorithm, Object accountId, Object audience) {
        if (jwtType == null || StringUtils.isEmpty(jwtType.toString())) {
            logger.warn("The jwtType cannot be null or empty");
            return false;
        }
        if (jwsAlgorithm == null || StringUtils.isEmpty(jwsAlgorithm.toString())) {
            logger.warn("The jwsAlgorithm cannot be null or empty");
            return false;
        }
        if (accountId == null || StringUtils.isEmpty(accountId.toString())) {
            logger.warn("The accountId cannot be null or empty");
            return false;
        }
        if (audience == null || StringUtils.isEmpty(audience.toString())) {
            logger.warn("The audience cannot be null or empty");
            return false;
        }
        return true;
    }

    private SecretBuilder getSecretBuilderForKey(String accountId, Key key) {
        return new SecretBuilder()
                .secretKey(key)
                .stableId(accountId)
                .expiresIn(5, ChronoUnit.MINUTES, Clock.systemUTC());
    }

    private JwtClaimsSet createJwtClaimsSet(String accountId, String audience, long validity) {
        long iatTime = Time.currentTimeMillis();
        Date expiration = Time.newDate(iatTime + (validity * 60 * 1000));

        JwtClaimsSet jwtClaimsSet = new JwtClaimsSet();
        jwtClaimsSet.setIssuer(accountId);
        jwtClaimsSet.setSubject(accountId);
        jwtClaimsSet.addAudience(audience);
        jwtClaimsSet.setIssuedAtTime(Time.newDate());
        jwtClaimsSet.setExpirationTime(expiration);
        jwtClaimsSet.setJwtId(UUID.randomUUID().toString());
        return jwtClaimsSet;
    }

    private String buildJwtForAlgorithm(JwtBuilderType jwtBuilderType, JwsAlgorithm jwsAlgorithm, String accountId,
            JsonValue privateKey, String signingKey, String encryptionKey, JwtClaimsSet jwtClaims)
            throws NoSuchSecretException {

        switch (jwsAlgorithm) {
        case HS256:
            SigningKey hmacKey = new SigningKey(getSecretBuilderForKey(accountId,
                    new SecretKeySpec(Base64.decode(signingKey), "Hmac")));
            SecretHmacSigningHandler hmacSigningHandler = new SecretHmacSigningHandler(hmacKey);

            KeyEncryptionKey encryptedKey = null;
            if (jwtBuilderType.isEncrypted()) {
                encryptedKey = new KeyEncryptionKey(getSecretBuilderForKey(accountId,
                        new SecretKeySpec(Base64.decode(encryptionKey), "AES")));
            }

            return buildJwt(jwsAlgorithm, jwtBuilderType, hmacSigningHandler, encryptedKey, jwtClaims);
        case RS256:
            if (jwtBuilderType.isEncrypted()) {
                logger.warn("The jwtType " + jwtBuilderType + " is not supported for algorithm: " + jwsAlgorithm);
                return null;
            }

            SecretRSASigningHandler rsaSigningHandler = new SecretRSASigningHandler(
                    new SigningKey(getSecretBuilderForKey(accountId,
                            RsaJWK.parse(privateKey).toRSAPrivateKey())));

            return buildSignedJwt(jwsAlgorithm, rsaSigningHandler, jwtClaims);
        default:
            logger.warn("The JwsAlgorithm is not supported: " + jwsAlgorithm);
            return null;
        }
    }

    private String buildJwt(JwsAlgorithm jwsAlgorithm, JwtBuilderType jwtBuilderType,
            SecretSigningHandler signingHandler, KeyEncryptionKey encryptedKey, JwtClaimsSet jwtClaims) {

        switch (jwtBuilderType) {
        case SIGNED:
            return buildSignedJwt(jwsAlgorithm, signingHandler, jwtClaims);
        case SIGNED_THEN_ENCRYPTED:
            return new JwtBuilderFactory()
                    .jws(signingHandler)
                    .headers()
                    .alg(JwsAlgorithm.HS256)
                    .done()
                    .encrypt(encryptedKey)
                    .headers()
                    .alg(JweAlgorithm.DIRECT)
                    .enc(EncryptionMethod.A128CBC_HS256)
                    .done()
                    .claims(jwtClaims)
                    .build();
        case ENCRYPTED_THEN_SIGNED:
            return new JwtBuilderFactory()
                    .jwe(encryptedKey)
                    .headers()
                    .alg(JweAlgorithm.DIRECT)
                    .enc(EncryptionMethod.A128CBC_HS256)
                    .done()
                    .claims(jwtClaims)
                    .signedWith(signingHandler, JwsAlgorithm.HS256)
                    .build();
        default:
            logger.warn("Unsupported JwtType: " + jwtBuilderType);
            return null;
        }
    }

    private String buildSignedJwt(JwsAlgorithm jwsAlgorithm, SecretSigningHandler signingHandler,
            JwtClaimsSet jwtClaims) {

        return new JwtBuilderFactory()
                .jws(signingHandler)
                .headers()
                .alg(jwsAlgorithm)
                .done()
                .claims(jwtClaims)
                .build();
    }
}
