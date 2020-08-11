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

import static java.util.Arrays.copyOfRange;

import java.math.BigInteger;

/**
 * This class represents a method of, and the result of parsing the certInfo's contents of the signed attestation data.
 */
final class TpmAttested {

    private static final int TPM_ALG_LNG = 2;

    TpmAlg nameAlg;
    byte[] name;
    byte[] qualifiedName;

    private TpmAttested(TpmAlg nameAlg, byte[] name, byte[] qualifiedName) {
        this.nameAlg = nameAlg;
        this.name = name;
        this.qualifiedName = qualifiedName;
    }

    static TpmAttested toTpmAttested(byte[] attestedName, byte[] attestedQualifiedName) {
        int algLookup = new BigInteger(1, copyOfRange(attestedName, 0, TPM_ALG_LNG)).intValue();
        TpmAlg nameAlg = TpmAlg.getTpmAlg(algLookup);
        attestedName = copyOfRange(attestedName, TPM_ALG_LNG, attestedName.length);

        return new TpmAttested(nameAlg, attestedName, attestedQualifiedName);
    }

}
