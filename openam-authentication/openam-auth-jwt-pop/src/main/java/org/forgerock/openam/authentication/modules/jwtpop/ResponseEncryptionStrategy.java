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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import javax.inject.Provider;

import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.jwt.Algorithm;

/**
 * Determines how Proof of Possession challenge responses should be encrypted.
 */
public enum ResponseEncryptionStrategy {
    /**
     * Elliptic Curve Diffie-Hellman (ECDH) key agreement with ephemeral keys. In this case each party contributes a
     * fresh random ECDH key-pair, which are used to derive a shared secret key. The ephemeral public keys are sent
     * to the other party and are signed using long-term keys to ensure authenticity. As fresh encryption keys are
     * derived each time and long-term keys are only used to authenticate, this provides <em>forward secrecy</em>:
     * any compromise of the long-term private keys does not allow an attacker to (passively) decrypt any previous or
     * future sessions.
     * <p>
     * The authentication module will generate an ephemeral key pair and send the public key in the challenge. The
     * private key will be kept in the authentication session. The other party will then generate their own key pair,
     * use ECDH to derive a shared secret and encrypt the result. They will send their ephemeral key pair (signed
     * with their long-term private key) alongside the response to AM, which will perform the same ECDH key agreement
     * to decrypt the response. All public keys involved in the exchange (i.e., both sides' ephemeral public keys and
     * both sides' long-term identity public keys) are hashed into the key derivation process, along with the ECDH
     * shared secret, and random values contributed by both parties. This strongly binds the resulting encryption key
     * to the full context of the authentication process.
     */
    ECDHE {
        @Override
        public JWK getEncryptionKeyPair(Provider<JWKSet> subjectJwkSetProvider) {
            return generateEphemeralKeyPair();
        }
    },
    /**
     * Uses a pre-shared key stored in the JWK Set in the subject's profile to directly encrypt the response. This is
     * a simple and cheap method that can be used to provide additional security strength. However, as a single
     * shared key is used to encrypt all exchanges, it is more vulnerable to key compromise than the ECDHE strategy.
     * It is recommended that keys are regularly rotated if using this mode.
     */
    PSK {
        @Override
        public JWK getEncryptionKeyPair(Provider<JWKSet> subjectJwkSetProvider) {
            return findEncryptionKey(subjectJwkSetProvider.get());
        }
    };

    public abstract JWK getEncryptionKeyPair(Provider<JWKSet> subjectJwkSetProvider);

    EcJWK generateEphemeralKeyPair() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(SupportedEllipticCurve.P256.getParameters());
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();

            return EcJWK.builder((ECPublicKey) keyPair.getPublic())
                    .privateKey((ECPrivateKey) keyPair.getPrivate())
                    .keyUse(KeyUse.ENC)
                    .algorithm(JweAlgorithm.ECDH_ES)
                    .build();

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to generate ephemeral ECDH keypair");
        }
    }

    JWK findEncryptionKey(final JWKSet jwkSet) {
        JWK result = null;
        for (JWK candidate : jwkSet.getJWKsAsList()) {
            if (candidate.getUse() == KeyUse.ENC || isEncryptionAlgorithm(candidate.getJwaAlgorithm())) {
                if (result != null) {
                    throw new IllegalStateException("more than one encryption key registered for user");
                }
                result = candidate;
            }
        }
        if (result == null) {
            throw new IllegalStateException("no encryption key in profile");
        }
        return result;
    }

    private boolean isEncryptionAlgorithm(final Algorithm algorithm) {
        return algorithm instanceof JweAlgorithm;
    }
}
