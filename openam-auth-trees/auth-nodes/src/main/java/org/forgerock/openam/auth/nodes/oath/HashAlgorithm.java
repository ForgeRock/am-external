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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oath;

/**
 * Supported hash algorithms for TOTP.
 *
 * As defined on <a href="https://tools.ietf.org/html/rfc6238">[RFC6238]</a>, TOTP implementations MAY use HMAC-SHA-256
 * or HMAC-SHA-512 functions, based on SHA-256 or SHA-512 hash functions, instead of the HMAC-SHA-1 function that has
 * been specified for the HOTP computation in <a href="https://tools.ietf.org/html/rfc4226">[RFC4226]</a>.
 */
public enum HashAlgorithm {
    /**
     * HMAC-SHA1 algorithm.
     */
    HMAC_SHA1 {
        @Override
        public String toString() {
            return "SHA1";
        }
    },
    /**
     * HMAC-SHA256 algorithm.
     */
    HMAC_SHA256 {
        @Override
        public String toString() {
            return "SHA256";
        }
    },
    /**
     * HMAC-SHA512 algorithm.
     */
    HMAC_SHA512 {
        @Override
        public String toString() {
            return "SHA512";
        }
    }
}