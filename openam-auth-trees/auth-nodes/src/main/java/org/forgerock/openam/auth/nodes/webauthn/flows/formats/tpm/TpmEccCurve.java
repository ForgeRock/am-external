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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import org.forgerock.json.jose.jws.SupportedEllipticCurve;

/**
 * This enum maps TPM codes to ECC curve types, and those supported by AM.
 */
// @Checkstyle:ignore JavadocVariable
public enum TpmEccCurve {

    TPM_ECC_NONE(0x0000, null),
    TPM_ECC_NIST_P192(0x0001, null),
    TPM_ECC_NIST_P224(0x0002, null),
    TPM_ECC_NIST_P256(0x0003, SupportedEllipticCurve.P256),
    TPM_ECC_NIST_P384(0x0004, SupportedEllipticCurve.P384),
    TPM_ECC_NIST_P521(0x0005, SupportedEllipticCurve.P521),
    TPM_ECC_BN_P256(0x0010, null),
    TPM_ECC_BN_P638(0x0011, null),
    TPM_ECC_SM2_P256(0x0020, null);

    private int value;
    private SupportedEllipticCurve curve;

    TpmEccCurve(int value, SupportedEllipticCurve curve) {
        this.value = value;
        this.curve = curve;
    }

    /**
     * Retrieve the curve information from the int lookup.
     *
     * @param lookup The int of the curve information to look up.
     * @return The enum associated with that type.
     */
    public static TpmEccCurve getTpmEccCurve(int lookup) {
        for (TpmEccCurve c : values()) {
            if (c.value == lookup) {
                return c;
            }
        }
        return null;
    }

    /**
     * Retrieve the mapped supported curve.
     *
     * @return The mapped supported curve, or null.
     */
    public SupportedEllipticCurve getSupportedEllipticCurve() {
        return curve;
    }
}
