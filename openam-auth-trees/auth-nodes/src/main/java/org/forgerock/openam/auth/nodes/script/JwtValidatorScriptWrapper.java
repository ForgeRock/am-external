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
 * Copyright 2023-2025 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static com.sun.identity.shared.Constants.LEGACY_JWT_VALIDATION;
import static org.forgerock.secrets.Purpose.VERIFY;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SecretHmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SecretSigningHandler;
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

import com.iplanet.am.util.SystemProperties;

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
     *      <li>audience</li>
     *      <li>issuer</li>
     *      <li>subject</li>
     *      <li>type</li>
     *      <li>audience</li>
     *      <li>stableId</li>
     *      <li>accountId</li>
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
        if (!validateJwtData(jwtData.get("jwtType"), jwtData.get("jwt"))) {
            logger.error("The jwtType and jwt cannot be null or empty");
            return null;
        }

        JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType = JwtAssertionScriptWrapper.JwtBuilderType.valueOf(
                jwtData.get("jwtType").toString());
        String jwtString = jwtData.get("jwt").toString();
        String issuer = jwtData.getOrDefault("issuer", "").toString();
        String subject = jwtData.getOrDefault("subject", "").toString();
        String audience = jwtData.getOrDefault("audience", "").toString();
        String type = jwtData.getOrDefault("type", "").toString();
        String stableId = jwtData.getOrDefault("stableId", issuer).toString();
        String accountId = jwtData.getOrDefault("accountId", "").toString();

        if (!accountId.isEmpty() && (issuer.isEmpty() && stableId.isEmpty())) {
            issuer = accountId;
            stableId = accountId;
        }

        if (StringUtils.isNotEmpty(accountId)) {
            logger.debug("The accountId is deprecated and has been set to the issuer and subject jwt claims.");
        }

        if (StringUtils.isEmpty(stableId)) {
            logger.error("The stableId and issuer cannot be null or empty");
            return null;
        }

        String signingKey = jwtData.getOrDefault("signingKey", "").toString();
        String encryptionKey = jwtData.getOrDefault("encryptionKey", "").toString();

        SecretKeySpec decryptionKey = null;
        if (!encryptionKey.isEmpty()) {
            decryptionKey = new SecretKeySpec(Base64.decode(encryptionKey), "AES");
        }

        return getClaimsMapFromReconstructedJwt(jwtString, issuer, subject, audience, type, jwtBuilderType,
                stableId, signingKey, decryptionKey);
    }

    private boolean validateJwtData(Object jwtType, Object jwt) {
        if (jwtType == null || StringUtils.isEmpty(jwtType.toString())) {
            logger.error("The jwtType cannot be null or empty");
            return false;
        }
        if (jwt == null || StringUtils.isEmpty(jwt.toString())) {
            logger.error("The jwt cannot be null or empty");
            return false;
        }
        return true;
    }

    private Map<String, Object> getClaimsMapFromReconstructedJwt(String jwtString, String issuer, String subject,
            String audience, String type, JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType,
            String stableId, String signingKey, SecretKeySpec decryptionKey) throws NoSuchSecretException {

        Jwt jwt = reconstructJwt(jwtString, jwtBuilderType, stableId, signingKey, decryptionKey);
        if (jwt == null) {
            logger.error("There was an error reconstructing the jwt");
            return null;
        }

        return validateAndConvertJwtClaimsSetToMap(issuer, subject, audience, type, jwt);
    }

    private Jwt reconstructJwt(String jwtString, JwtAssertionScriptWrapper.JwtBuilderType jwtBuilderType,
            String stableId, String signingKey, SecretKeySpec decryptionKey) throws NoSuchSecretException {

        Jwt jwt;
        SecretSigningHandler verificationHandler; // Declare the variable outside the switch statement

        switch (jwtBuilderType) {
        case SIGNED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, SignedJwt.class);
            if (!useLegacyJwtValidation()) {
                verificationHandler = getVerificationHandler(
                        ((SignedJwt) jwt).getHeader().getAlgorithm(), stableId, signingKey);
                if (!((SignedJwt) jwt).verify(verificationHandler)) {
                    logger.error("The signed JWT does not have a valid signature");
                    return null;
                }
            }
            break;
        case ENCRYPTED_THEN_SIGNED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, EncryptedThenSignedJwt.class);
            verificationHandler = getVerificationHandler(
                    ((EncryptedThenSignedJwt) jwt).getHeader().getAlgorithm(), stableId, signingKey);
            ((EncryptedThenSignedJwt) jwt).decrypt(decryptionKey);
            if (!((EncryptedThenSignedJwt) jwt).verify(verificationHandler)) {
                logger.error("The encrypted then signed JWT does not have a valid signature");
                return null;
            }
            break;
        case SIGNED_THEN_ENCRYPTED:
            jwt = new JwtBuilderFactory().reconstruct(jwtString, SignedThenEncryptedJwt.class);
            ((SignedThenEncryptedJwt) jwt).decrypt(decryptionKey);
            verificationHandler = getVerificationHandler(
                    ((SignedThenEncryptedJwt) jwt).getSignedJwt().getHeader().getAlgorithm(), stableId, signingKey);
            if (!((SignedThenEncryptedJwt) jwt).verify(verificationHandler)) {
                logger.error("The signed then encrypted JWT does not have a valid signature");
                return null;
            }
            break;
        default:
            logger.error("Unsupported JwtType: " + jwtBuilderType);
            return null;
        }
        return jwt;
    }

    private boolean useLegacyJwtValidation() {
        return SystemProperties.getAsBoolean(LEGACY_JWT_VALIDATION, false);
    }

    private Map<String, Object> validateAndConvertJwtClaimsSetToMap(String issuer, String subject, String audience,
            String type, Jwt jwt) {
        JwtClaimsSet jwtClaims = jwt.getClaimsSet();
        String jwtIssuer = jwtClaims.getIssuer();
        List<String> jwtAudience = jwtClaims.getAudience();
        String jwtSubject = jwtClaims.getSubject();
        String jwtType = jwtClaims.getType();
        Date jwtIssuedAt = jwtClaims.getIssuedAtTime();
        Date jwtExpiry = jwtClaims.getExpirationTime();
        Date now = Time.newDate();

        if (!issuer.isEmpty() && !Objects.equals(jwtIssuer, issuer)) {
            logger.error("Issuer in JWT Claims [" + jwtIssuer + "] doesn't match expected issuer [" + issuer + "]");
            return null;
        }

        if (!subject.isEmpty() && !Objects.equals(jwtSubject, subject)) {
            logger.error("Subject in JWT Claims [" + jwtSubject + "] doesn't match expected subject [" + subject + "]");
            return null;
        }

        if (!audience.isEmpty() && !jwtAudience.contains(audience)) {
            logger.error(jwtAudience + ": JWT Claims audience does not match expected audience [" + audience + "]");
            return null;
        }

        if (!type.isEmpty() && !Objects.equals(jwtType, type)) {
            logger.error("Type in JWT Claims [" + jwtType + "] doesn't match expected Type [" + type + "]");
            return null;
        }

        if (jwtIssuedAt != null && jwtIssuedAt.after(now)) {
            logger.error("JWT Claims issued in the future [" + jwtIssuedAt + "]");
            return null;
        }

        if (jwtExpiry != null && jwtExpiry.before(now)) {
            logger.error("JWT Claims expired at [" + jwtExpiry + "]");
            return null;
        }

        return createJwtClaimsMap(jwtClaims);
    }

    private Map<String, Object> createJwtClaimsMap(JwtClaimsSet jwtClaims) {
        Map<String, Object> claimsMap = new HashMap<>();
        for (String claimKey : jwtClaims.keys()) {
            switch (claimKey) {
            case "iss":
                claimsMap.put("issuer", jwtClaims.getClaim(claimKey));
                break;
            case "sub":
                claimsMap.put("subject", jwtClaims.getClaim(claimKey));
                break;
            case "aud":
                claimsMap.put("audience", jwtClaims.getClaim(claimKey));
                break;
            case "exp":
                claimsMap.put("expirationTime", jwtClaims.getClaim(claimKey));
                break;
            case "nbf":
                claimsMap.put("notBefore", jwtClaims.getClaim(claimKey));
                break;
            case "iat":
                claimsMap.put("issuedAt", jwtClaims.getClaim(claimKey));
                break;
            case "jti":
                claimsMap.put("jwtId", jwtClaims.getClaim(claimKey));
                break;
            case "typ":
                claimsMap.put("type", jwtClaims.getClaim(claimKey));
                break;
            default:
                claimsMap.put(claimKey, jwtClaims.getClaim(claimKey));
                break;
            }
        }
        return claimsMap;
    }

    private SecretSigningHandler getVerificationHandler(JwsAlgorithm algorithm, String stableId, String signingKey)
            throws NoSuchSecretException {
        if (useLegacyJwtValidation()) {
            getHmacVerificationHandler(algorithm, stableId, signingKey);
        }
        switch (algorithm) {
        case HS256:
            return getHmacVerificationHandler(algorithm, stableId, signingKey);
        case RS256:
            return getRsaVerificationHandler(algorithm, stableId, signingKey);
        default:
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private SecretHmacSigningHandler getHmacVerificationHandler(JwsAlgorithm algorithm, String stableId,
            String signingKey) throws NoSuchSecretException {
        SecretBuilder secretBuilder = new SecretBuilder()
                .secretKey(new SecretKeySpec(Base64.decode(signingKey), algorithm.name()))
                .stableId(stableId)
                .expiresIn(10, ChronoUnit.SECONDS, Clock.systemUTC());

        return new SecretHmacSigningHandler(new VerificationKey(secretBuilder));
    }

    private SecretRSASigningHandler getRsaVerificationHandler(JwsAlgorithm algorithm, String stableId,
            String signingKey) throws NoSuchSecretException {
        try {
            X509EncodedKeySpec x509publicKey = new X509EncodedKeySpec(Base64.decode(signingKey));
            KeyFactory kf = KeyFactory.getInstance(algorithm.getAlgorithmType().name());
            PublicKey publicKey = kf.generatePublic(x509publicKey);
            VerificationKey rsaVerificationKey = new SecretBuilder()
                    .publicKey(publicKey)
                    .stableId(stableId)
                    .expiresIn(10, ChronoUnit.SECONDS, Clock.systemUTC())
                    .build(VERIFY);
            return new SecretRSASigningHandler(rsaVerificationKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid RSA public key", e);
        }
    }
}
