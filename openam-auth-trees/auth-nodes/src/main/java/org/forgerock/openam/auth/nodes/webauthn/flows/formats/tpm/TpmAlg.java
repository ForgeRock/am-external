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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;

/**
 * This enum maps TPM codes to supported algorithms, and those known-supported through Cose.
 */
// @Checkstyle:ignore JavadocVariable
public enum TpmAlg {

    TPM_ALG_ERROR(0x0000, null),
    TPM_ALG_RSA(0x0001, CoseAlgorithm.RS1),
    TPM_ALG_TDES(0x0003, null),
    TPM_ALG_SHA(0x0004, null),
    TPM_ALG_HMAC(0x0005, null),
    TPM_ALG_AES(0x0006, null),
    TPM_ALG_MGF1(0x0007, null),
    TPM_ALG_KEYEDHASH(0x0008, null),
    TPM_ALG_XOR(0x000A, null),
    TPM_ALG_SHA256(0x000B, CoseAlgorithm.RS256),
    TPM_ALG_SHA384(0x000C, CoseAlgorithm.RS384),
    TPM_ALG_SHA512(0x000D, CoseAlgorithm.RS512),
    TPM_ALG_NULL(0x0010, null),
    TPM_ALG_SM3_256(0x0012, null),
    TPM_ALG_SM4(0x0013, null),
    TPM_ALG_RSASSA(0x0014, null),
    TPM_ALG_RSAES(0x0015, null),
    TPM_ALG_RSAPSS(0x0016, null),
    TPM_ALG_OAEP(0x0017, null),
    TPM_ALG_ECDSA(0x0018, null),
    TPM_ALG_ECDH(0x0019, null),
    TPM_ALG_ECDAA(0x001A, null),
    TPM_ALG_SM2(0x001B, null),
    TPM_ALG_ECSCHNORR(0x001C, null),
    TPM_ALG_ECMQV(0x001D, null),
    TPM_ALG_KDF1_SP800_56A(0x0020, null),
    TPM_ALG_KDF2(0x0021, null),
    TPM_ALG_KDF1_SP800_108(0x0022, null),
    TPM_ALG_ECC(0x0023, null),
    TPM_ALG_SYMCIPHER(0x0025, null),
    TPM_ALG_CAMELLIA(0x0026, null),
    TPM_ALG_CTR(0x0040, null),
    TPM_ALG_OFB(0x0041, null),
    TPM_ALG_CBC(0x0042, null),
    TPM_ALG_CFB(0x0043, null),
    TPM_ALG_ECB(0x0044, null);

    private final int value;
    private final CoseAlgorithm coseAlg;

    TpmAlg(int value, CoseAlgorithm coseAlg) {
        this.value = value;
        this.coseAlg = coseAlg;
    }

    /**
     * Retrieve the Cose alg representation of this algorithm.
     *
     * @return The Cose alg representation supported, or null.
     */
    public CoseAlgorithm getCoseAlg() {
        return coseAlg;
    }

    /**
     * Retrieve the alg information from the int lookup.
     *
     * @param lookup The int of the alg information to look up.
     * @return The enum associated with that alg.
     */
    public static TpmAlg getTpmAlg(int lookup) {
        for (TpmAlg t : values()) {
            if (t.value == lookup) {
                return t;
            }
        }
        return null;
    }
}