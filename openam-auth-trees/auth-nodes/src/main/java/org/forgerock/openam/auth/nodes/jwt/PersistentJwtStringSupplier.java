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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.jwt;

import static org.forgerock.openam.auth.nodes.HmacSigningKeyConfig.DEPRECATED_STABLE_ID;

import java.io.FileNotFoundException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.EncryptedThenSignedJwtBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.secrets.keys.EncryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;

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
    private static final Purpose<DataEncryptionKey> PCOOKIE_NODES_ENCRYPTION_PURPOSE =
            Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataEncryptionKey.class);

    private final JwtBuilderFactory jwtBuilderFactory;
    private final PersistentJwtProvider persistentJwtProvider;
    private final AuthKeyFactory authKeyFactory;
    private final AMKeyProvider amKeyProvider;
    private final RealmLookup realmLookup;
    private final SecretReferenceCache secretReferenceCache;
    private final SigningManager signingManager;

    /**
     * Constructs a PersistentJwtStringSupplier.
     *
     * @param jwtBuilderFactory     Jwt builder factory.
     * @param persistentJwtProvider persistent jwt provider.
     * @param authKeyFactory        auth key factory.
     * @param amKeyProvider         The AM key provider.
     * @param secretReferenceCache  The secrets API.
     * @param realmLookup           The realm lookup
     * @param signingManager        The signing manager.
     */
    @Inject
    public PersistentJwtStringSupplier(JwtBuilderFactory jwtBuilderFactory, PersistentJwtProvider persistentJwtProvider,
            AuthKeyFactory authKeyFactory, AMKeyProvider amKeyProvider, SecretReferenceCache secretReferenceCache,
            RealmLookup realmLookup, @Named("EmptySigningManager") SigningManager signingManager) {
        this.jwtBuilderFactory = jwtBuilderFactory;
        this.persistentJwtProvider = persistentJwtProvider;
        this.authKeyFactory = authKeyFactory;
        this.secretReferenceCache = secretReferenceCache;
        this.realmLookup = realmLookup;
        this.amKeyProvider = amKeyProvider;
        this.signingManager = signingManager;
    }

    /**
     * Creates a Jwt String. The Jwt is signed and encrypted.
     *
     * @param orgName        the org.
     * @param authContext    auth claims in a context bucket.
     * @param maxLife        the time after which the jwt is expired and invalid.
     * @param idleTimeout    the period time in which a jwt must be used or expire.
     * @param hmacSigningKey the signing key which was used to sign the jwt. Cannot be empty or null.
     * @param kid            the key which will be used to sign the jwt.
     * @return A Jwt as a String
     * @throws InvalidPersistentJwtException when the jwt is invalid.
     */
    public String createJwtString(String orgName, Map<String, String> authContext, long maxLife, long idleTimeout,
            SigningKey hmacSigningKey, String kid) throws InvalidPersistentJwtException {
        Reject.ifNull(hmacSigningKey, "Signing key must not be null");
        Map<String, Object> jwtParameters = buildJwtParameters(authContext);
        JwtClaimsSet claimsSet = buildJwtClaimsSet(jwtParameters, maxLife, idleTimeout);
        return buildEncryptedJwtString(claimsSet, orgName, signingManager.newSigningHandler(hmacSigningKey), kid);
    }

    /**
     * Update an existing Jwt. Decrypts the jwt, updates the idle timeout claim and encrypts the Jwt.
     *
     * @param jwtCookie                 the jwt.
     * @param orgName                   the org.
     * @param signingKeyReference       the signing key reference.
     * @param verificationKeysReference the verification key.
     * @param idleTimeout               the new idle timeout.
     * @return a Jwt with an updated idle timeout.
     * @throws InvalidPersistentJwtException when the jwt is invalid.
     * @throws NoSuchSecretException         when the secret is not found.
     */
    public Jwt getUpdatedJwt(String jwtCookie, String orgName, SecretReference<SigningKey> signingKeyReference,
            ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference, long idleTimeout)
            throws InvalidPersistentJwtException, NoSuchSecretException {
        if (jwtCookie == null) {
            return null;
        }
        Jwt jwt = persistentJwtProvider.getValidDecryptedJwt(jwtCookie, orgName, verificationKeysReference);
        if (hasCoolOffPeriodExpired(jwt)) {
            jwt = resetJwtIdleTimeout(jwt, idleTimeout);
        }
        SigningKey signingKey = signingKeyReference.get();
        var stableId = DEPRECATED_STABLE_ID.equals(signingKey.getStableId()) ? null : signingKey.getStableId();
        return buildEncryptedJwt(jwt.getClaimsSet(), orgName,
                signingManager.newSigningHandler(signingKey),
                stableId);
    }

    private boolean hasCoolOffPeriodExpired(Jwt jwt) {
        Instant now = Time.newDate().toInstant();
        Instant issuedAtTime = jwt.getClaimsSet().getIssuedAtTime().toInstant();
        Instant issuedAtTimePlusOneMinute = issuedAtTime.plusSeconds(COOL_OFF_IN_SECONDS);
        return now.isAfter(issuedAtTimePlusOneMinute);
    }

    private Jwt resetJwtIdleTimeout(Jwt jwt, long idleTimeout) {
        final Date now = Time.newDate();
        long idleTimeoutPeriod = TimeUnit.HOURS.toSeconds(idleTimeout);
        Instant nextIdleTimeout = now.toInstant().plusSeconds(idleTimeoutPeriod);
        jwt.getClaimsSet().setIssuedAtTime(now);
        jwt.getClaimsSet().setNotBeforeTime(now);
        jwt.getClaimsSet().setClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, nextIdleTimeout.getEpochSecond());
        return jwt;
    }

    private Map<String, Object> buildJwtParameters(Map<String, String> authContext) {
        Map<String, Object> jwtParameters = new HashMap<>();
        String sessionId = UUID.randomUUID().toString();
        jwtParameters.put(FORGEROCK_AUTH_CONTEXT, authContext);
        jwtParameters.put(SESSION_ID_CLAIM_KEY, sessionId);
        return jwtParameters;
    }

    private String buildEncryptedJwtString(JwtClaimsSet claimsSet, String orgName, SigningHandler signingHandler,
            String signingKeyId) throws InvalidPersistentJwtException {
        return getEncryptedJwtBuilder(claimsSet, orgName, signingHandler, signingKeyId).build();
    }

    private SignedJwt buildEncryptedJwt(JwtClaimsSet claimsSet, String orgName, SigningHandler signingHandler,
            String newSigningKeyId) throws InvalidPersistentJwtException {
        return getEncryptedJwtBuilder(claimsSet, orgName, signingHandler, newSigningKeyId).asJwt();
    }

    private EncryptedThenSignedJwtBuilder getEncryptedJwtBuilder(JwtClaimsSet claimsSet, String orgName,
            SigningHandler signingHandler,
            String signingKeyId) throws InvalidPersistentJwtException {
        Realm realm;
        try {
            realm = realmLookup.lookup(orgName);
        } catch (RealmLookupException e) {
            throw new InvalidPersistentJwtException("Realm not found: " + orgName, e);
        }

        EncryptionKey<DataEncryptionKey> encryptionKey = secretReferenceCache.realm(realm)
                .active(PCOOKIE_NODES_ENCRYPTION_PURPOSE)
                .getAsync()
                .then(s -> s, e -> null)
                .getOrThrowIfInterrupted();

        EncryptedJwtBuilder builder;
        if (encryptionKey != null) {
            builder = jwtBuilderFactory.jwe(encryptionKey);
        } else {
            PublicKey publicDeprecatedKey;
            try {
                publicDeprecatedKey = authKeyFactory.getPublicAuthKey(amKeyProvider, orgName);
            } catch (FileNotFoundException | SMSException | SSOException e) {
                throw new InvalidPersistentJwtException(e);
            }
            if (publicDeprecatedKey != null) {
                builder = jwtBuilderFactory.jwe(publicDeprecatedKey);
            } else {
                throw new InvalidPersistentJwtException("No encryption key found for realm: " + orgName);
            }
        }

        EncryptedThenSignedJwtBuilder signedEncryptedJwtBuilder = builder
                .headers()
                .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims(claimsSet)
                .signedWith(signingHandler, SIGNING_ALGORITHM);

        if (signingKeyId != null) {
            signedEncryptedJwtBuilder.headers().kid(signingKeyId);
        }

        return signedEncryptedJwtBuilder;
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
}
