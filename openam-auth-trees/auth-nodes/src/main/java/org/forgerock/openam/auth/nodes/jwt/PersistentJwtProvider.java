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

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import java.io.FileNotFoundException;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JweDecryptionCheckedException;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
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
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.KeyUsage;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Provides persistent cookie jwts.
 */
public class PersistentJwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(PersistentJwtProvider.class);
    private static final String TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY = "tokenIdleTimeSeconds";
    private static final Purpose<DataDecryptionKey> PCOOKIE_NODES_DECRYPTION_PURPOSE =
            Purpose.purpose(Labels.PCOOKIE_NODES_ENCRYPTION, DataDecryptionKey.class);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());

    private final AMKeyProvider amKeyProvider;
    private final AuthKeyFactory authKeyFactory;
    private final JwtReconstruction jwtReconstruction;
    private final RealmLookup realmLookup;
    private final SecretReferenceCache secretReferenceCache;
    private final SigningManager signingManager;

    /**
     * Constructs a PersistentJwtProvider.
     *
     * @param authKeyFactory the auth key factory.
     * @param jwtReconstruction the jwt builder.
     * @param amKeyProvider the AM key provider.
     * @param realmLookup the realm lookup.
     * @param secretReferenceCache cache of secret reference.
     * @param signingManager the signing manager.
     */
    @Inject
    public PersistentJwtProvider(AuthKeyFactory authKeyFactory, JwtReconstruction jwtReconstruction,
            AMKeyProvider amKeyProvider, RealmLookup realmLookup, SecretReferenceCache secretReferenceCache,
            @Named("EmptySigningManager") SigningManager signingManager) {
        this.authKeyFactory = authKeyFactory;
        this.jwtReconstruction = jwtReconstruction;
        this.amKeyProvider = amKeyProvider;
        this.realmLookup = realmLookup;
        this.secretReferenceCache = secretReferenceCache;
        this.signingManager = signingManager;
    }

    /**
     * Returns a valid, decrypted Jwt. A jwt is 'valid' if its signature has been verified and the current time is
     * within the jwt's idle and expiry time.
     *
     * @param jwtString The jwt as a String.
     * @param orgName the org name.
     * @param verificationKeysReference the secrets reference.
     * @return a valid, decrypted Jwt.
     * @throws InvalidPersistentJwtException thrown if jwt parsing or validation fails.
     */
    public Jwt getValidDecryptedJwt(String jwtString, String orgName,
            ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference)
            throws InvalidPersistentJwtException {
        if (jwtString == null) {
            throw new InvalidPersistentJwtException("jwtString is null");
        }
        Reject.ifNull(verificationKeysReference, "verificationKeys is null");

        Realm realm;
        try {
            realm = realmLookup.lookup(orgName);
        } catch (RealmLookupException e) {
            throw new InvalidPersistentJwtException("Unable to find realm for org: " + orgName, e);
        }

        EncryptedThenSignedJwt jwt;
        try {
            jwt = jwtReconstruction.reconstructJwt(jwtString, EncryptedThenSignedJwt.class);
        } catch (InvalidJwtException e) {
            throw new InvalidPersistentJwtException("jwt reconstruction error");
        }
        if (jwt == null) {
            throw new InvalidPersistentJwtException("jwt reconstruction error");
        }

        validateSignature(jwt, verificationKeysReference);


        Jwt decrypted = decryptJwt(orgName, realm, jwt);

        if (!isWithinIdleAndExpiryTime(decrypted)) {
            throw new InvalidPersistentJwtException("jwt is not within expiry or idle time");
        }
        return decrypted;
    }

    private EncryptedJwt decryptJwt(String orgName, Realm realm, EncryptedThenSignedJwt jwt)
            throws InvalidPersistentJwtException {
        ValidSecretsReference<DataDecryptionKey, NeverThrowsException> decryptionKeyReference =
                secretReferenceCache.realm(realm).namedOrValid(PCOOKIE_NODES_DECRYPTION_PURPOSE,
                        jwt.getJweHeader().getKeyId());
        try {
            return decryptionKeyReference.getAsync().then(secrets -> {
                if (secrets.isEmpty()) {
                    logger.warn("No secrets found, falling back to deprecated decryption");
                    return deprecatedDecrypt(orgName, jwt);
                } else {
                    return jwt.decrypt(decryptionKeyReference);
                }
            }).getOrThrowIfInterrupted().getOrThrowIfInterrupted();
        } catch (JweDecryptionCheckedException e) {
            throw new InvalidPersistentJwtException(e);
        }
    }

    private Promise<? extends EncryptedJwt, JweDecryptionCheckedException> deprecatedDecrypt(
            String orgName, EncryptedThenSignedJwt jwt) {
        DataDecryptionKey secret;
        try {
            PrivateKey privateKey = authKeyFactory.getPrivateAuthKey(amKeyProvider, orgName);
            if (privateKey == null) {
                logger.error("No secrets available for JWT decryption");
                return newExceptionPromise(new JweDecryptionCheckedException());
            }
            secret = new SecretBuilder()
                             .secretKey(privateKey)
                             .expiresAt(Instant.MAX)
                             .stableId("deprecated-decryption-key")
                             .keyUsages(Set.of(KeyUsage.DECRYPT))
                             .build(PCOOKIE_NODES_DECRYPTION_PURPOSE);
        } catch (SSOException | SMSException | FileNotFoundException | IllegalStateException
                 | NoSuchSecretException e1) {
            logger.error("error getting keys from store", e1);
            return newExceptionPromise(new JweDecryptionCheckedException());
        }
        SecretsProvider secretsProvider = new SecretsProvider(FIXED_CLOCK);
        secretsProvider.useSpecificSecretForPurpose(PCOOKIE_NODES_DECRYPTION_PURPOSE, secret);
        return jwt.decrypt(secretsProvider, PCOOKIE_NODES_DECRYPTION_PURPOSE);
    }

    private void validateSignature(SignedJwt jwt,
            ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference)
            throws InvalidPersistentJwtException {
        if (!jwt.verify(signingManager.newVerificationHandler(verificationKeysReference).getOrThrowIfInterrupted())) {
            throw new InvalidPersistentJwtException("failed to verify jwt signature");
        }
    }

    private boolean isWithinIdleAndExpiryTime(Jwt jwt) {
        JwtClaimsSet jwtClaimsSet = jwt.getClaimsSet();
        long expiryTime = jwtClaimsSet.getExpirationTime().getTime();
        long tokenIdleTimeMillis = TimeUnit.SECONDS.toMillis(
                jwtClaimsSet.getClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Number.class).longValue());
        long tokenIdleTime = new Date(tokenIdleTimeMillis).getTime();
        long nowTime = new Date(Time.currentTimeMillis()).getTime();
        return expiryTime > nowTime && tokenIdleTime > nowTime;
    }
}
