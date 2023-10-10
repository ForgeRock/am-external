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
package org.forgerock.openam.auth.nodes.script;

import java.security.Key;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.SignedThenEncryptedJwt;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SecretHmacSigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class to support the validation of JWTs within scripts.
 */
@Supported
public class JwtValidatorScriptWrapper {

    private final Logger logger = LoggerFactory.getLogger(JwtValidatorScriptWrapper.class);

    /**
     * Validates a JWT assertion based on the JWT data provided.
     * <p>
     * Supported jwtData fields are:
     *  <ul>
     *      <li>jwtType</li>
     *      <li>jwt</li>
     *      <li>accountId</li>
     *      <li>audience</li>
     *      <li>signingKey</li>
     *      <li>encryptionKey</li>
     *  </ul>
     * </p>
     *
     * @param jwtData an object containing data to validate the JWT
     * @return the JwtClaimsSet for the valid jwt
     * @throws NoSuchSecretException if the given key set does not contain the verification key
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Map<String, Object> validateJwtClaims(Map<String, Object> jwtData) throws NoSuchSecretException {
        if (!validateJwtData(jwtData.get("jwtType"), jwtData.get("jwt"), jwtData.get("accountId"),
                jwtData.get("audience"))) {
            return null;
        }

        JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType = JwtAssertionScriptWrapper.JwtBuilderType.valueOf(
                jwtData.get("jwtType").toString());
        String jwtString = jwtData.get("jwt").toString();
        String accountId = jwtData.get("accountId").toString();
        String audience = jwtData.get("audience").toString();

        String signingKey = jwtData.getOrDefault("signingKey", "").toString();
        String encryptionKey = jwtData.getOrDefault("encryptionKey", "").toString();

        VerificationKey verificationKey = getVerificationKey(accountId,
                new SecretKeySpec(Base64.decode(signingKey), "Hmac"));
        SecretHmacSigningHandler verificationHandler = new SecretHmacSigningHandler(verificationKey);
        SecretKeySpec decryptionKey = new SecretKeySpec(Base64.decode(encryptionKey), "AES");

        return getClaimsMapFromReconstructedJwt(jwtString, accountId, audience, jwtBuilderType,
                verificationHandler, decryptionKey);
    }

    private boolean validateJwtData(Object jwtType, Object jwt, Object accountId, Object audience) {
        if (jwtType == null || StringUtils.isEmpty(jwtType.toString())) {
            logger.warn("The jwtType cannot be null or empty");
            return false;
        }
        if (jwt == null || StringUtils.isEmpty(jwt.toString())) {
            logger.warn("The jwt cannot be null or empty");
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

    private VerificationKey getVerificationKey(String accountId, Key key)
            throws NoSuchSecretException {

        SecretBuilder secretBuilder = new SecretBuilder()
                .secretKey(key)
                .stableId(accountId)
                .expiresIn(5, ChronoUnit.MINUTES, Clock.systemUTC());

        return new VerificationKey(secretBuilder);
    }

    private Map<String, Object> getClaimsMapFromReconstructedJwt(String jwtString, String accountId, String audience,
            JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType, SecretHmacSigningHandler verificationHandler,
            SecretKeySpec decryptionKey) {

        Jwt jwt = reconstructJwt(jwtString, jwtBuilderType, verificationHandler, decryptionKey);
        if (jwt == null) {
            logger.warn("There was an error reconstructing the jwt");
            return null;
        }

        return validateAndConvertJwtClaimsSetToMap(accountId, audience, jwt);
    }

    private Jwt reconstructJwt(String jwtString, JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType,
            SecretHmacSigningHandler verificationHandler, SecretKeySpec decryptionKey) {

        Jwt jwt;
        switch (jwtBuilderType) {
        case SIGNED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, SignedJwt.class);
            break;

        case ENCRYPTED_THEN_SIGNED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, EncryptedThenSignedJwt.class);
            ((EncryptedThenSignedJwt) jwt).decrypt(decryptionKey);

            if (!((EncryptedThenSignedJwt) jwt).verify(verificationHandler)) {
                logger.warn("The encrypted then signed JWT does not have a valid signature");
                return null;
            }
            break;

        case SIGNED_THEN_ENCRYPTED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, SignedThenEncryptedJwt.class);
            ((SignedThenEncryptedJwt) jwt).decrypt(decryptionKey);

            if (!((SignedThenEncryptedJwt) jwt).verify(verificationHandler)) {
                logger.warn("The signed then encrypted JWT does not have a valid signature");
                return null;
            }
            break;

        default:
            logger.warn("Unsupported JwtType: " + jwtBuilderType);
            return null;
        }
        return jwt;
    }

    private Map<String, Object> validateAndConvertJwtClaimsSetToMap(String accountId, String audience, Jwt jwt) {
        JwtClaimsSet jwtClaims = jwt.getClaimsSet();
        String jwtIssuer = jwtClaims.getIssuer();
        List<String> jwtAudience = jwtClaims.getAudience();
        String jwtSubject = jwtClaims.getSubject();
        Date jwtIssuedAt = jwtClaims.getIssuedAtTime();
        Date jwtExpiry = jwtClaims.getExpirationTime();
        Date now = Time.newDate();

        if (!Objects.equals(jwtIssuer, accountId)) {
            logger.warn("Issuer in JWT [" + jwtIssuer + "] doesn't match expected issuer [" + accountId + "]");
            return null;
        }

        if (jwtAudience == null || !jwtAudience.contains(audience)) {
            logger.warn(jwtAudience + ": JWT audience does not match expected audience [" + audience + "]");
            return null;
        }

        if (jwtIssuedAt == null || jwtIssuedAt.after(now)) {
            logger.warn("JWT issued in the future [" + jwtIssuedAt + "]");
            return null;
        }

        if (jwtExpiry == null || jwtExpiry.before(now)) {
            logger.warn("JWT expired at [" + jwtExpiry + "]");
            return null;
        }

        return createJwtClaimsMap(jwtClaims, jwtIssuer, jwtAudience, jwtSubject, jwtIssuedAt, jwtExpiry);
    }

    private Map<String, Object> createJwtClaimsMap(JwtClaimsSet jwtClaims, String jwtIssuer, List<String> jwtAudience,
            String jwtSubject, Date jwtIssuedAt, Date jwtExpiry) {

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("audience", jwtAudience);
        claimsMap.put("expirationTime", jwtExpiry);
        claimsMap.put("issuedAtTime", jwtIssuedAt);
        claimsMap.put("issuer", jwtIssuer);
        claimsMap.put("jwtId", jwtClaims.getJwtId());
        claimsMap.put("subject", jwtSubject);
        return claimsMap;
    }
}
