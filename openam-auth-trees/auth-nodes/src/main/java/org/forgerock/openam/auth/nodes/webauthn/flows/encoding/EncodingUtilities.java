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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class or basic common encoding operations.
 */
public final class EncodingUtilities {

    private static final Logger logger = LoggerFactory.getLogger(EncodingUtilities.class);

    private EncodingUtilities() { }

    /**
     * Decode a Base64URL encoded string.
     *
     * @param encodedValue the base64URL encoded value.
     * @return the decoded value.
     */
    public static byte[] base64UrlDecode(String encodedValue) {
        return Base64url.decode(encodedValue);
    }

    /**
     * Return the SHA-256 hash of the string value.
     *
     * @param value a string value.
     * @return the hash of the string value.
     */
    public static byte[] getHash(String value) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            logger.error("failed to perform a SHA-256 hash", e);
        }
        return hash;
    }

    /**
     * Returns a base64 URL encoded hash of the String input.
     * @param input the input string.
     * @return the base64 URL hashed string value.
     */
    public static String base64UrlEncode(String input) {
        return Base64url.encode(input.getBytes());
    }
}
