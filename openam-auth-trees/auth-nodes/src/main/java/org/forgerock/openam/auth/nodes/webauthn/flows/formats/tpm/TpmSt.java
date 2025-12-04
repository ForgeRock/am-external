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
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

/**
 * This enum represents the various types of information that could potentially be included in a parsed tpm
 * structure during attestation.
 */
// @Checkstyle:ignore JavadocVariable
public enum TpmSt {

    TPM_ST_RSP_COMMAND(0x00C4),
    TPM_ST_NULL(0x8000),
    TPM_ST_NO_SESSIONS(0x8001),
    TPM_ST_SESSIONS(0x8002),
    TPM_ST_ATTEST_NV(0x8014),
    TPM_ST_ATTEST_COMMAND_AUDIT(0x8015),
    TPM_ST_ATTEST_SESSION_AUDIT(0x8016),
    TPM_ST_ATTEST_CERTIFY(0x8017), // this is the important one for webAuthn
    TPM_ST_ATTEST_QUOTE(0x8018),
    TPM_ST_ATTEST_TIME(0x8019),
    TPM_ST_ATTEST_CREATION(0x801A),
    TPM_ST_CREATION(0x8021),
    TPM_ST_VERIFIED(0x8022),
    TPM_ST_AUTH_SECRET(0x8023),
    TPM_ST_HASHCHECK(0x8024),
    TPM_ST_AUTH_SIGNED(0x8025),
    TPM_ST_FU_MANIFEST(0x8029);

    private final int value;

    TpmSt(int value) {
        this.value = value;
    }

    /**
     * Retrieve the type information from the int lookup.
     *
     * @param lookup The int of the type information to look up.
     * @return The enum associated with that type.
     */
    public static TpmSt getType(int lookup) {
        for (TpmSt t : values()) {
            if (t.value == lookup) {
                return t;
            }
        }
        return null;
    }
}
