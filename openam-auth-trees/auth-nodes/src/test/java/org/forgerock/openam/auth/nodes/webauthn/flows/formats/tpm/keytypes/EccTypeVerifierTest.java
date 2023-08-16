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
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmEccCurve;
import org.testng.annotations.Test;

public class EccTypeVerifierTest {

    @Test
    public void testParseToType() throws IOException {
        //given
        byte[] paramBytes = new byte[] { 0, 16, 0, 16, 0, 1, 0, 16 };

        //when
        EccTypeVerifier result = EccTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.getCurveId()).isEqualTo(TpmEccCurve.TPM_ECC_NIST_P192);
        assertThat(result.getKdf()).isEqualTo(TpmAlg.TPM_ALG_NULL);
        assertThat(result.getScheme()).isEqualTo(TpmAlg.TPM_ALG_NULL);
        assertThat(result.getSymmetric()).isEqualTo(TpmAlg.TPM_ALG_NULL);
    }

}