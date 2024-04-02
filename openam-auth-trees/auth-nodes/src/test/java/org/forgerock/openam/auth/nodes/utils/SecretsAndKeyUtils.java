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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.utils;

import java.time.Instant;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.KeyUsage;

/**
 * Utility class for helping with generating secrets and keys
 */
public class SecretsAndKeyUtils {

    private SecretsAndKeyUtils() {
    }

    /**
     * Creates and returns a new secret builder.
     *
     * @param keyId     The key id
     * @param keyBytes  The key bytes
     * @param usage     The key usage
     * @param algorithm The secrets algorithm
     * @return The secret builder
     */
    public static SecretBuilder getSecretBuilder(String keyId, byte[] keyBytes, KeyUsage usage, String algorithm) {
        return new SecretBuilder()
                .stableId(keyId)
                .expiresAt(Instant.MAX)
                .secretKey(new SecretKeySpec(keyBytes, algorithm))
                .keyUsages(Set.of(usage));
    }
}
