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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Various utility methods for working with JWKs.
 */
final class JwkUtils {
    private JwkUtils() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Creates a JWK Thumbprint of one or more JWKs. If only a single JWK is specified, then the thumbprint is as
     * in <a href="https://tools.ietf.org/html/rfc7638">RFC 7638: JSON Web Key (JWK) Thumbprint</a>. If more than one
     * JWK is specified, then the binary thumbprints of each are concatenated and returned.
     *
     * @param messageDigest the message digest (hash) algorithm to use for the thumbprint.
     * @param jwks the Json Web Keys to thumprint.
     * @return the combined thumbprint of the given JWKs
     */
    static byte[] thumbprint(String messageDigest, JWK... jwks) {
        try {
            final MessageDigest md = MessageDigest.getInstance(messageDigest);
            final int hashLen = md.getDigestLength();
            if (hashLen <= 0) {
                throw new IllegalArgumentException("unable to determine output size of hash algorithm");
            }
            final byte[] buffer = new byte[hashLen * jwks.length];
            int offset = 0;
            for (JWK jwk : jwks) {
                final JsonValue essentialKeys = new JsonValue(essentialKeys(jwk));
                final byte[] macInput = essentialKeys.toString().replaceAll("\\s+", "").getBytes(UTF_8);
                md.update(macInput);
                offset += md.digest(buffer, offset, hashLen);
            }

            return buffer;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (DigestException e) {
            throw new IllegalStateException("Unable to compute JWK thumbprint", e);
        }
    }

    /**
     * Returns a lexicographically ordered map of the essential keys for a JWK. The essential keys are those keys
     * that are required to reproduce the public key (or the whole key for OctJWK).
     *
     * @param jwk the JWK
     * @return the essential keys and values of the JWK, in lexicographical order.
     */
    @VisibleForTesting
    static SortedMap<String, String> essentialKeys(JWK jwk) {
        final SortedMap<String, String> essentialKeys = new TreeMap<>();
        essentialKeys.put("kty", keyType(jwk).toString());
        switch (keyType(jwk)) {
            case OCT:
                essentialKeys.put("k", ((OctJWK) jwk).getKey());
                break;
            case EC:
                essentialKeys.put("crv", ((EcJWK) jwk).getCurve());
                essentialKeys.put("x", ((EcJWK) jwk).getX());
                essentialKeys.put("y", ((EcJWK) jwk).getY());
                break;
            case RSA:
                essentialKeys.put("n", ((RsaJWK) jwk).getModulus());
                essentialKeys.put("e", ((RsaJWK) jwk).getPublicExponent());
                break;
            default:
                throw new IllegalArgumentException("unrecognised key type: " + keyType(jwk));
        }
        return essentialKeys;
    }

    /**
     * Determines the key type of the given JWK.
     *
     * @param jwk the JWK.
     * @return the {@link KeyType} of the JWK.
     */
    @VisibleForTesting
    static KeyType keyType(JWK jwk) {
        if (jwk.getKeyType() != null) {
            return jwk.getKeyType();
        }
        if (jwk instanceof OctJWK) {
            return KeyType.OCT;
        }
        if (jwk instanceof RsaJWK) {
            return KeyType.RSA;
        }
        if (jwk instanceof EcJWK) {
            return KeyType.EC;
        }
        throw new IllegalArgumentException("unrecognised key type: " + jwk);
    }

    /**
     * Determines the JWS/JWE algorithm that this key is to be used for.
     *
     * @param jwk the key to determine the JWA algorithm for.
     * @param defaultAlgorithm the default if no algorithm has been specified for the key.
     * @return the algorithm to use with this key.
     */
    static Algorithm algorithm(JWK jwk, Algorithm defaultAlgorithm) {
        if (jwk.getJwaAlgorithm() != null) {
            return jwk.getJwaAlgorithm();
        }
        return defaultAlgorithm;
    }
}
