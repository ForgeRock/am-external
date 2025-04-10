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

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmsAttestException;
import org.junit.jupiter.api.Test;

public class TpmsAttestTest {

    @Test
    void testToTpmsAttest() throws InvalidTpmsAttestException {
        //given
        byte[] paramBytes = new byte[]{-1, 84, 67, 71, -128, 23, 0, 34, 0, 11, 85, -120, 104, -14, 71, 96, -116, 2,
                -35, 81, 59, -38, -93, -113, 8, -50, 62, -94, -116, 126, 43, -13, 53, -88, 34, -88, 110, -123, -66,
                105, -118, -100, 0, 20, 36, -57, -20, 86, 57, 40, -80, -64, 119, -63, 121, -104, 119, -110, -25, -34,
                -26, 103, -20, 117, 0, 0, 0, 0, 11, -96, -49, 29, -18, -3, 97, -5, 32, -86, -90, -66, 1, 127, 123, -66,
                -15, 100, -88, 95, -38, 0, 34, 0, 11, -75, 32, 96, -95, -114, 31, -32, -65, -78, 50, -78, -27, -44, 99,
                -2, -88, 92, 56, -66, -67, -68, 14, -35, 117, -124, 46, -22, -55, 92, -17, -118, -4, 0, 34, 0, 11, -97,
                -69, -99, 28, -40, -31, 94, -99, 72, -20, 66, 111, -79, 88, -84, 98, -117, -9, 12, 113, 114, -27, -21,
                -31, -44, -86, -61, -50, -118, 21, 123, -27};

        //when
        TpmsAttest result = TpmsAttest.toTpmsAttest(paramBytes);

        //then
        assertThat(result.magic).isEqualTo(0xff544347);
        assertThat(result.type).isEqualTo(TpmSt.TPM_ST_ATTEST_CERTIFY);
        assertThat(result.clockInfo).isEqualTo(new byte[]{0, 0, 0, 0, 11, -96, -49, 29, -18, -3, 97, -5, 32, -86,
                -90, -66, 1});
        assertThat(result.extraData).isEqualTo(new byte[]{36, -57, -20, 86, 57, 40, -80, -64, 119, -63, 121, -104,
                119, -110, -25, -34, -26, 103, -20, 117});
        assertThat(result.firmwareVersion).isEqualTo(new byte[]{127, 123, -66, -15, 100, -88, 95, -38});
        assertThat(result.qualifiedSigner).isEqualTo(new byte[]{0, 11, 85, -120, 104, -14, 71, 96, -116, 2, -35, 81,
                59, -38, -93, -113, 8, -50, 62, -94, -116, 126, 43, -13, 53, -88, 34, -88, 110, -123, -66, 105, -118,
                -100});

        assertThat(result.attested.nameAlg).isEqualTo(TpmAlg.TPM_ALG_SHA256);
        assertThat(result.attested.name).isEqualTo(new byte[]{-75, 32, 96, -95, -114, 31, -32, -65, -78, 50, -78,
                -27, -44, 99, -2, -88, 92, 56, -66, -67, -68, 14, -35, 117, -124, 46, -22, -55, 92, -17, -118, -4});
        assertThat(result.attested.qualifiedName).isEqualTo(new byte[]{0, 11, -97, -69, -99, 28, -40, -31, 94, -99,
                72, -20, 66, 111, -79, 88, -84, 98, -117, -9, 12, 113, 114, -27, -21, -31, -44, -86, -61, -50, -118,
                21, 123, -27});
    }

}
