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
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.forgerock.util.Reject;

/**
 * Class to abstract away the validation logic for comparing the pubArea unique value
 * with an EC public key.
 */
public final class EccUniqueParameter implements TpmtUniqueParameter {

    private final byte[] x;
    private final byte[] y;

    EccUniqueParameter(final byte[] x, final byte[] y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Verifies unique coordinates in public key are equal to those specified in the pubArea.
     *
     * @param publicKey The EC public key for comparison
     * @return True if EC key's co-ordinate values match the pubArea unique values
     */
    @Override
    public boolean verifyUniqueParameter(final PublicKey publicKey) {
        Reject.ifNull(publicKey, "Public key object cannot be null");
        Reject.ifTrue(!(publicKey instanceof ECPublicKey), "Public key must be an ECPublicKey object");

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        BigInteger uniqueX = new BigInteger(1, x);
        BigInteger uniqueY = new BigInteger(1, y);
        return (ecPublicKey.getW().getAffineX().equals(uniqueX) && ecPublicKey.getW().getAffineY().equals(uniqueY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EccUniqueParameter that = (EccUniqueParameter) o;
        return Arrays.equals(x, that.x) && Arrays.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(x);
        return 31 * result + Arrays.hashCode(y);
    }
}
