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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.KeyUsage;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * Extensible interface for configuration classes which contain hmac signing and verification keys.
 * Provides default helper methods for obtaining the deprecated keys and/or secret references.
 */
public interface HmacSigningKeyConfig {

    /**
     * The HMAC algorithm.
     */
    String HMAC_ALGORITHM = "HMAC";

    /**
     * The deprecated stable id.
     */
    String DEPRECATED_STABLE_ID = UUID.randomUUID().toString();
    /**
     * The default signing purpose which persistent cookie nodes/treehooks should fall back to in the case of a
     * missing signingKeyPurpose and hmacSigningKey.
     */
    Purpose<SigningKey> DEFAULT_SIGNING_PURPOSE =
            Purpose.purpose(Labels.DEFAULT_PCOOKIE_NODES_SIGNING, SigningKey.class);
    /**
     * The default verification purpose which persistent cookie nodes/treehooks should fall back to in the case of a
     * missing verificationKeyPurpose and hmacVerificationKey.
     */
    Purpose<VerificationKey> DEFAULT_VERIFICATION_PURPOSE =
            Purpose.purpose(Labels.DEFAULT_PCOOKIE_NODES_SIGNING, VerificationKey.class);

    /**
     * The (optional) signing key purpose.
     *
     * @return The signing key purpose.
     */
    Optional<Purpose<SigningKey>> signingKeyPurpose();

    /**
     * The (optional) signing key.
     *
     * @return The signing key.
     * @deprecated use {@link #signingKeyPurpose()} instead.
     */
    @Deprecated
    Optional<char[]> hmacSigningKey();

    /**
     * The {@link #hmacSigningKey()} wrapped in a {@link SigningKey}.
     *
     * @return an optional signing key.
     * @deprecated use {@link #signingKeyReference(SecretCache)} instead.
     */
    @Deprecated
    default Optional<SigningKey> wrappedHmacSigningKey() {
        return hmacSigningKey()
                .map(key -> Base64.getDecoder().decode(new String(key)))
                .map(key -> {
                    try {
                        return new SigningKey(new SecretBuilder()
                                .expiresAt(Instant.MAX)
                                .secretKey(new SecretKeySpec(key, HMAC_ALGORITHM))
                                .keyUsages(Set.of(KeyUsage.SIGN))
                                .stableId(DEPRECATED_STABLE_ID)
                        );
                    } catch (NoSuchSecretException e) {
                        return null;
                    }
                });
    }

    /**
     * The (optional) verification key purpose.
     *
     * @return The verification key purpose.
     */
    default Optional<Purpose<VerificationKey>> verificationKeyPurpose() {
        return signingKeyPurpose().map(purpose -> Purpose.purpose(purpose.getLabel(), VerificationKey.class));
    }

    /**
     * The {@link #hmacSigningKey()} wrapped in a {@link VerificationKey}.
     *
     * @return an optional verification key.
     * @deprecated use {@link #verificationKeyPurpose()} instead.
     */
    @Deprecated
    default Optional<VerificationKey> wrappedHmacVerificationKey() {
        return hmacSigningKey()
                .map(key -> Base64.getDecoder().decode(new String(key)))
                .map(key -> {
                    try {
                        return new VerificationKey(new SecretBuilder()
                                .expiresAt(Instant.MAX)
                                .secretKey(new SecretKeySpec(key, HMAC_ALGORITHM))
                                .keyUsages(Set.of(KeyUsage.VERIFY))
                                .stableId(DEPRECATED_STABLE_ID)
                        );
                    } catch (NoSuchSecretException e) {
                        return null;
                    }
                });
    }

    /**
     * A secret reference for the verification key.
     *
     * @param kid         The key ID. If not provided or cannot be found, all valid secrets will be queried.
     * @param secretCache The secret cache.
     * @return A secret reference to one or more valid verification keys.
     */
    default ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference(String kid,
            SecretCache secretCache) {
        return verificationKeyPurpose()
                .map(purpose -> secretCache.namedOrValid(purpose, kid))
                .or(() -> wrappedHmacVerificationKey()
                        .map(verificationKey -> {
                            SecretsProvider secretsProvider = new SecretsProvider(Clock.systemUTC());
                            secretsProvider.useSpecificSecretForPurpose(DEFAULT_VERIFICATION_PURPOSE,
                                    verificationKey);
                            return secretsProvider.createValidReference(DEFAULT_VERIFICATION_PURPOSE);
                        }))
                .orElseGet(() -> secretCache.namedOrValid(DEFAULT_VERIFICATION_PURPOSE, kid));
    }

    /**
     * The secret reference for the signing key.
     *
     * @param secretCache The secret cache.
     * @return A secret reference to the signing key.
     */
    default SecretReference<SigningKey> signingKeyReference(SecretCache secretCache) {
        return signingKeyPurpose()
                .map(secretCache::active)
                .or(() -> wrappedHmacSigningKey()
                        .map(verificationKey -> {
                            SecretsProvider secretsProvider = new SecretsProvider(Clock.systemUTC());
                            secretsProvider.useSpecificSecretForPurpose(DEFAULT_SIGNING_PURPOSE,
                                    verificationKey);
                            return secretsProvider.createActiveReference(DEFAULT_SIGNING_PURPOSE);
                        }))
                .orElse(secretCache.active(DEFAULT_SIGNING_PURPOSE));
    }

}
