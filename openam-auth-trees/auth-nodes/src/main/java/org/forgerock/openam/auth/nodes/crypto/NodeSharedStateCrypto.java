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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.crypto;

import static org.forgerock.openam.secrets.SecretsUtils.convertRawEncryptionKey;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.Secret;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Encapsulates crypto operations for node shared state payloads.
 *
 * @supported.api
 */
@Singleton
public class NodeSharedStateCrypto {

    /**
     * The node shared state encryption purpose.
     */
    public static final Purpose<DataEncryptionKey> NODE_SHARED_STATE_ENCRYPTION_PURPOSE =
            purpose(Labels.NODE_SHARED_STATE_ENCRYPTION, DataEncryptionKey.class);

    /**
     * The node shared state decryption purpose.
     */
    public static final Purpose<DataDecryptionKey> NODE_SHARED_STATE_DECRYPTION_PURPOSE =
            purpose(Labels.NODE_SHARED_STATE_ENCRYPTION, DataDecryptionKey.class);

    private static final JweAlgorithm JWE_ALGORITHM = JweAlgorithm.DIRECT;

    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;

    private final JwtBuilderFactory jwtBuilderFactory;

    private final SecretsProvider secretsProvider;

    private final SecretReference<DataEncryptionKey> encryptionKeySecretReference;

    /**
     * Constructor.
     *
     * @param jwtBuilderFactory the JWT builder factory
     * @param secrets           the secrets service
     */
    @Inject
    public NodeSharedStateCrypto(JwtBuilderFactory jwtBuilderFactory, Secrets secrets) {
        this.jwtBuilderFactory = jwtBuilderFactory;
        this.secretsProvider = secrets.getGlobalSecrets();
        this.encryptionKeySecretReference = new SecretReference<>(this.secretsProvider,
                NODE_SHARED_STATE_ENCRYPTION_PURPOSE, Time.getClock());
    }

    /**
     * Encrypt a shared state {@link JsonValue} payload.
     *
     * @param payload the {@link JsonValue} to be encrypted
     * @return the encrypted string
     */
    public String encrypt(JsonValue payload) {
        try {
            DataEncryptionKey encryptionKey = encryptionKeySecretReference.get();
            return jwtBuilderFactory
                    .jwe(convertRawEncryptionKey(encryptionKey, JWE_ALGORITHM, ENCRYPTION_METHOD))
                    .headers()
                    .kid(encryptionKey.getStableId())
                    .alg(JWE_ALGORITHM)
                    .enc(ENCRYPTION_METHOD)
                    .done()
                    .claims(new JwtClaimsSet(payload.asMap()))
                    .asJwt()
                    .build();
        } catch (NoSuchSecretException e) {
            throw new IllegalStateException("No secret found for the node shared state encryption", e);
        }
    }

    /**
     * Decrypt an encrypted string.
     *
     * @param payload the string to be decrypted
     * @return the decrypted {@link JsonValue}
     */
    public JsonValue decrypt(String payload) {
        EncryptedJwt encryptedJwt = jwtBuilderFactory.reconstruct(payload, EncryptedJwt.class);
        Promise<Stream<DataDecryptionKey>, NeverThrowsException> promise = secretsProvider.getNamedOrValidSecrets(
                NODE_SHARED_STATE_DECRYPTION_PURPOSE, encryptedJwt.getHeader().getKeyId());
        try {
            DataDecryptionKey decryptionKey = promise.getOrThrowUninterruptibly()
                    // loop through all the candidate keys, in case the kid has been renamed in the backend,
                    .filter(key -> isDecryptionKeyCandidate(encryptedJwt, key))
                    .findAny()
                    .orElseThrow(() -> new NoSuchSecretException(NODE_SHARED_STATE_DECRYPTION_PURPOSE.getLabel()));
            encryptedJwt.decrypt(convertRawEncryptionKey(decryptionKey, JWE_ALGORITHM, ENCRYPTION_METHOD));
        } catch (NoSuchSecretException e) {
            throw new IllegalStateException("No secret found for the node shared state decryption", e);
        }
        return encryptedJwt.getClaimsSet().toJsonValue();
    }

    private static <S extends Secret> Purpose<S> purpose(String label, Class<S> type) {
        return label == null ? null : Purpose.purpose(label, type);
    }

    private boolean isDecryptionKeyCandidate(EncryptedJwt encryptedJwt, DataDecryptionKey key) {
        try {
            encryptedJwt.decrypt(convertRawEncryptionKey(key, JWE_ALGORITHM, ENCRYPTION_METHOD));
            return true;
        } catch (JweDecryptionException | NoSuchSecretException e) {
            return false;
        }
    }

}
