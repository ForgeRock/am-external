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
