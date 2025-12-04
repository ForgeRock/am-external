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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import org.forgerock.util.Reject;

/**
 * Class to abstract away the validation logic for comparing the pubArea unique value
 * with an RSA public key.
 */
public final class RsaUniqueParameter implements TpmtUniqueParameter {
    private final byte[] unique;

    RsaUniqueParameter(final byte[] unique) {
        this.unique = unique;
    }

    /**
     * Verifies modulus in public key is equal to that specified in the pubArea.
     *
     * @param publicKey The RSA public key for comparison
     * @return True if the RSA key modulus matches the pubArea unique value
     */
    @Override
    public boolean verifyUniqueParameter(final PublicKey publicKey) {
        Reject.ifNull(publicKey, "Public key object cannot be null");
        Reject.ifTrue(!(publicKey instanceof RSAPublicKey), "Public key must be an RSAPublicKey object");

        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        return rsaPublicKey.getModulus().equals(new BigInteger(1, unique));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RsaUniqueParameter that = (RsaUniqueParameter) o;
        return Arrays.equals(unique, that.unique);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(unique);
    }
}
