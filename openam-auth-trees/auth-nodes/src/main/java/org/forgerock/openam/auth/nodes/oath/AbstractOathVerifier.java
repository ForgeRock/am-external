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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;

/**
 * Provides a method to verify the input OTP.
 */
abstract class AbstractOathVerifier {

    static final int[] DOUBLE_DIGITS = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
    static final int[] DIGITS_POWER = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    final OathTokenVerifierNode.Config config;
    final OathDeviceSettings settings;

    /**
     * The constructor.
     *
     * @param config the node configuration.
     * @param settings the oath device settings.
     */
    AbstractOathVerifier(OathTokenVerifierNode.Config config, OathDeviceSettings settings) {
        this.config = config;
        this.settings = settings;
    }

    /**
     * Validates the provided OTP code.
     *
     * @param otp the opt token code.
     * @throws OathVerificationException if the token code could not be validated.
     */
    abstract void verify(String otp) throws OathVerificationException;

    /**
     * Retrieves the shared secret stored on the device settings.
     *
     * @return The shared secret.
     * @throws OathVerificationException if there is an issue with the shared secret key.
     */
    String getSharedSecret() throws OathVerificationException {
        String sharedSecret = settings.getSharedSecret();

        if (sharedSecret == null || sharedSecret.isEmpty()) {
            throw new OathVerificationException("Secret key is not a valid value");
        }

        // Get rid of white space in string (messes with the data converter).
        sharedSecret = sharedSecret.replaceAll("\\s+", "");
        // Convert sharedSecret to lowercase.
        sharedSecret = sharedSecret.toLowerCase();
        // Make sure sharedSecret is even length.
        if ((sharedSecret.length() % 2) != 0) {
            sharedSecret = "0" + sharedSecret;
        }

        return sharedSecret;
    }

    /**
     * Check if the strings have the same length and all bytes at corresponding positions are equal.
     *
     * @param str1 one of the digests to compare.
     * @param str2 the other digest to compare.
     * @return true if the digests are equal, false otherwise.
     */
    boolean isEqual(String str1, String str2) {
        return MessageDigest.isEqual(str1.getBytes(), str2.getBytes());
    }

    /**
     * Computes a Hashed Message Authentication Code with the crypto hash algorithm as a parameter.
     *
     * @param crypto the crypto algorithm.
     * @param keyBytes the bytes to use for the HMAC key.
     * @param textBytes the message or text to be authenticated.
     * @return the HMAC result.
     */
    static byte[] hmacSha(String crypto, byte[] keyBytes, byte[] textBytes) {
        try {
            Mac hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(textBytes);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    /**
     * Put the bytes into a result int. If add an offset is enabled, calculate it using the truncationOffset.
     *
     * @param truncationOffset the truncation Offset value.
     * @param hashBytes the hash bytes.
     * @param addOffset if should add an offset to the generation of the OTP.
     * @return the binary value.
     */
    static int getBinary(int truncationOffset, byte[] hashBytes, boolean addOffset) {
        int offset = hashBytes[hashBytes.length - 1] & 0xf;
        if (addOffset) {
            if ((0 <= truncationOffset) && (truncationOffset < (hashBytes.length - 4))) {
                offset = truncationOffset;
            }
        }

        return ((hashBytes[offset] & 0x7f) << 24) | ((hashBytes[offset + 1] & 0xff) << 16)
                | ((hashBytes[offset + 2] & 0xff) << 8) | (hashBytes[offset + 3] & 0xff);
    }
}
