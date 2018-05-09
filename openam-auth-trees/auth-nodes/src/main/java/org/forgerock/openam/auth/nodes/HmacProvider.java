/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import java.security.SecureRandom;

import com.sun.identity.shared.encode.Base64;

/**
 * Provides HMAC signing keys.
 */
public final class HmacProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private HmacProvider() { }

    /**
     * Generates and returns a random a signing key, suitable for HMAC.
     *
     * @param keySize the key size.
     * @return the key.
     */
    public static String generateSigningKey(KeySize keySize) {
        byte[] bytes = new byte[keySize.sizeInBits() / 8];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.encode(bytes);
    }

    /**
     * Holds the valid key size values.
     */
    public enum KeySize {
        /** Key of size 256 bits. */
        BITS_256(256),
        /** Key of size 128 bits. */
        BITS_128(128);

        private int sizeInBits;

        /**
         * Constructs the KeySize.
         *
         * @param sizeInBits the size in bits as an int.
         */
        KeySize(int sizeInBits) {
            this.sizeInBits = sizeInBits;
        }

        /**
         * Return the size as an int.
         *
         * @return the size.
         */
        public int sizeInBits() {
            return sizeInBits;
        }
    }
}
