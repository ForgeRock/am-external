/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.jwt;

import java.io.FileNotFoundException;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.encode.Base64;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Creates and updates jwts.
 */
public class PersistentJwtStringSupplier {

    private static final String TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY = "tokenIdleTimeSeconds";
    private static final JwsAlgorithm SIGNING_ALGORITHM = JwsAlgorithm.HS256;
    private static final String FORGEROCK_AUTH_CONTEXT = "org.forgerock.authentication.context";
    private static final int COOL_OFF_IN_SECONDS = 60;
    private static final String SESSION_ID_CLAIM_KEY = "sessionId";

    private final JwtBuilderFactory jwtBuilderFactory;
    private final PersistentJwtProvider persistentJwtProvider;
    private final AuthKeyFactory authKeyFactory;
    private final AMKeyProvider amKeyProvider;

    /**
     * Constructs a PersistentJwtStringSupplier.
     *
     * @param jwtBuilderFactory Jwt builder factory.
     * @param persistentJwtProvider persistent jwt provider.
     * @param authKeyFactory auth key factory.
     */
    @Inject
    public PersistentJwtStringSupplier(JwtBuilderFactory jwtBuilderFactory, PersistentJwtProvider persistentJwtProvider,
                                       AuthKeyFactory authKeyFactory) {
        this.jwtBuilderFactory = jwtBuilderFactory;
        this.persistentJwtProvider = persistentJwtProvider;
        this.authKeyFactory = authKeyFactory;
        this.amKeyProvider = InjectorHolder.getInstance(AMKeyProvider.class);
    }

    /**
     * Creates a Jwt String. The Jwt is signed and encrypted.
     *
     * @param orgName        the org.
     * @param authContext    auth claims in a context bucket.
     * @param maxLife        the time after which the jwt is expired and invalid.
     * @param idleTimeout    the period time in which a jwt must be used or expire.
     * @param hmacSigningKey the signing key. Cannot be empty or null.
     * @return A Jwt as a String
     * @throws InvalidPersistentJwtException when the jwt is invalid.
     */
    public String createJwtString(String orgName, Map<String, String> authContext, long maxLife, long idleTimeout,
            String hmacSigningKey) throws InvalidPersistentJwtException {
        Map<String, Object> jwtParameters = buildJwtParameters(orgName, authContext);
        JwtClaimsSet claimsSet = buildJwtClaimsSet(jwtParameters, maxLife, idleTimeout);

        return buildEncryptedJwtString(claimsSet, orgName, getSigningHandler(hmacSigningKey));
    }

    /**
     * Update an existing Jwt. Decrypts the jwt, updates the idle timeout claim and encrypts the Jwt.
     *
     * @param jwtCookie      the jwt.
     * @param orgName        the org.
     * @param hmacSigningKey the signing key.
     * @param idleTimeout    the new idle timeout.
     * @return a Jwt with an updated idle timeout.
     * @throws InvalidPersistentJwtException when the jwt is invalid.
     */
    public String getUpdatedJwt(String jwtCookie, String orgName, String hmacSigningKey, long idleTimeout)
            throws InvalidPersistentJwtException {
        String newJwtCookie = null;
        if (jwtCookie != null) {
            Jwt jwt = persistentJwtProvider.getValidDecryptedJwt(jwtCookie, orgName, hmacSigningKey);
            if (hasCoolOffPeriodExpired(jwt)) {
                newJwtCookie = resetJwtIdleTimeout(jwt, orgName, idleTimeout, hmacSigningKey);
            } else {
                newJwtCookie = jwtCookie;
            }
        }
        return newJwtCookie;
    }

    private boolean hasCoolOffPeriodExpired(Jwt jwt) {
        Instant now = Time.newDate().toInstant();
        Instant issuedAtTime = jwt.getClaimsSet().getIssuedAtTime().toInstant();
        Instant issuedAtTimePlusOneMinute = issuedAtTime.plusSeconds(COOL_OFF_IN_SECONDS);
        return now.isAfter(issuedAtTimePlusOneMinute);
    }

    private String resetJwtIdleTimeout(Jwt jwt, String orgName, long idleTimeout, String hmacSigningKey)
            throws InvalidPersistentJwtException {
        final Date now = Time.newDate();
        long idleTimeoutPeriod = TimeUnit.HOURS.toSeconds(idleTimeout);
        Instant nextIdleTimeout = now.toInstant().plusSeconds(idleTimeoutPeriod);
        jwt.getClaimsSet().setIssuedAtTime(now);
        jwt.getClaimsSet().setNotBeforeTime(now);
        jwt.getClaimsSet().setClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, nextIdleTimeout.getEpochSecond());
        return buildEncryptedJwtString(jwt.getClaimsSet(), orgName, getSigningHandler(hmacSigningKey));
    }

    private Map<String, Object> buildJwtParameters(String orgName, Map<String, String> authContext) {
        Map<String, Object> jwtParameters = new HashMap<>();
        String sessionId = UUID.randomUUID().toString();
        jwtParameters.put(FORGEROCK_AUTH_CONTEXT, authContext);
        jwtParameters.put(SESSION_ID_CLAIM_KEY, sessionId);
        return jwtParameters;
    }

    private String buildEncryptedJwtString(JwtClaimsSet claimsSet, String orgName, SigningHandler signingHandler)
            throws InvalidPersistentJwtException {
        Key publicKey;
        String encryptedJwt;
        try {
            publicKey = authKeyFactory.getPublicAuthKey(amKeyProvider, orgName);
        } catch (FileNotFoundException | SMSException | SSOException e) {
            throw new InvalidPersistentJwtException(e);
        }
        encryptedJwt = jwtBuilderFactory
                .jwe(publicKey)
                .headers()
                .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims(claimsSet)
                .sign(signingHandler, SIGNING_ALGORITHM)
                .build();

        return encryptedJwt;
    }

    private JwtClaimsSet buildJwtClaimsSet(Map<String, Object> jwtParameters, long maxLife, long idleTimeout) {
        final Date exp = new Date(Time.currentTimeMillis() + TimeUnit.HOURS.toMillis(maxLife));
        final Date tokenIdleTime = new Date(Time.currentTimeMillis() + TimeUnit.HOURS.toMillis(idleTimeout));
        final Date now = new Date(Time.currentTimeMillis());
        String jwtId = UUID.randomUUID().toString();
        return jwtBuilderFactory.claims()
                .jti(jwtId)
                .exp(exp)
                .nbf(now)
                .iat(now)
                .claim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, tokenIdleTime.getTime() / 1000L)
                .claims(jwtParameters)
                .build();
    }

    private SigningHandler getSigningHandler(String hmacSigningKey) throws InvalidPersistentJwtException {
        if (hmacSigningKey == null || hmacSigningKey.isEmpty()) {
            throw new InvalidPersistentJwtException("Null hmac key provided");
        }
        return new HmacSigningHandler(Base64.decode(hmacSigningKey.getBytes()));
    }
}
