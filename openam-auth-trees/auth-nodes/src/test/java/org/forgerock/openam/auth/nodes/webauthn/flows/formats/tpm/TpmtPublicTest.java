
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.RsaTypeVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes.TpmtUniqueParameter;
import org.junit.jupiter.api.Test;

public class TpmtPublicTest {

    @Test
    void testToTpmtPublic() throws InvalidTpmtPublicException {
        //given
        byte[] paramBytes = new byte[]{0, 1, 0, 11, 0, 6, 4, 114, 0, 32, -99, -1, -53, -13, 108, 56, 58, -26, -103,
                -5, -104, 104, -36, 109, -53, -119, -41, 21, 56, -124, -66, 40, 3, -110, 44, 18, 65, 88, -65, -83, 34,
                -82, 0, 16, 0, 16, 8, 0, 0, 0, 0, 0, 1, 0, -81, -73, 115, -31, 63, -72, -3, 123, -54, -107, 127, -60,
                -40, -50, 0, 20, 43, -111, -113, 17, 5, -20, 49, 34, -75, 48, -103, -85, -24, 86, -119, 124, -104, 5,
                103, 102, -3, 112, -80, 1, 101, -3, -124, -49, -95, -51, -81, 68, 98, -65, -122, -77, 109, -89, -125,
                4, 114, -104, 71, 49, 117, 96, -71, -18, 49, 60, 12, 127, -40, -40, -79, -32, 52, 107, -79, -82, -108,
                36, -85, -104, 32, -92, -109, -60, -101, 101, 50, -33, -7, -44, -84, -19, 93, -4, 84, 127, -59, -67,
                20, 30, -82, 36, 20, 98, -119, 106, -106, -120, 93, -35, 53, 95, -10, -18, 63, 2, -4, -50, 40, -80,
                -13, 48, -117, 64, 106, -117, -128, 62, 31, 57, 0, -54, 38, -14, -79, -107, 71, 56, -62, 102, -66,
                -125, 73, -36, 59, 113, -100, 59, -58, -65, -95, -90, -105, -92, 72, 103, -86, -105, 61, -40, -48, 0,
                -77, 95, -81, -111, 67, 124, 36, -110, -1, -97, 97, -7, -19, 42, 39, -22, 119, -11, 52, -81, -3, 17,
                11, -126, 66, 20, -17, 115, -4, -110, -43, -85, 83, -110, -85, 105, 55, 63, 23, -103, 55, 28, -97,
                -48, -61, -8, 40, 73, -66, -56, 127, 47, -85, -95, 9, -107, -70, 8, -71, -126, 80, 32, -24, 16, 46,
                -35, 17, 117, -12, -18, 40, -108, -117, -18, 33, -19, 68, 72, -7, -59, 59, 76, 103, 51, 15, -48, 83,
                18, 32, -43, 23, -105, -94, -41};

        BigInteger modulus = new BigInteger(1, new byte[]{-81, -73, 115, -31, 63, -72, -3, 123, -54, -107, 127, -60,
                -40, -50, 0, 20, 43, -111, -113, 17, 5, -20, 49, 34, -75, 48, -103, -85, -24, 86, -119, 124, -104, 5,
                103, 102, -3, 112, -80, 1, 101, -3, -124, -49, -95, -51, -81, 68, 98, -65, -122, -77, 109, -89, -125,
                4, 114, -104, 71, 49, 117, 96, -71, -18, 49, 60, 12, 127, -40, -40, -79, -32, 52, 107, -79, -82, -108,
                36, -85, -104, 32, -92, -109, -60, -101, 101, 50, -33, -7, -44, -84, -19, 93, -4, 84, 127, -59, -67,
                20, 30, -82, 36, 20, 98, -119, 106, -106, -120, 93, -35, 53, 95, -10, -18, 63, 2, -4, -50, 40, -80,
                -13, 48, -117, 64, 106, -117, -128, 62, 31, 57, 0, -54, 38, -14, -79, -107, 71, 56, -62, 102, -66,
                -125, 73, -36, 59, 113, -100, 59, -58, -65, -95, -90, -105, -92, 72, 103, -86, -105, 61, -40, -48, 0,
                -77, 95, -81, -111, 67, 124, 36, -110, -1, -97, 97, -7, -19, 42, 39, -22, 119, -11, 52, -81, -3, 17,
                11, -126, 66, 20, -17, 115, -4, -110, -43, -85, 83, -110, -85, 105, 55, 63, 23, -103, 55, 28, -97,
                -48, -61, -8, 40, 73, -66, -56, 127, 47, -85, -95, 9, -107, -70, 8, -71, -126, 80, 32, -24, 16, 46,
                -35, 17, 117, -12, -18, 40, -108, -117, -18, 33, -19, 68, 72, -7, -59, 59, 76, 103, 51, 15, -48, 83,
                18, 32, -43, 23, -105, -94, -41});
        RSAPublicKey publicKey = mock(RSAPublicKey.class);
        when(publicKey.getModulus()).thenReturn(modulus);

        //when
        TpmtPublic result = TpmtPublic.toTpmtPublic(paramBytes);
        TpmtUniqueParameter uniqueParameter = result.unique;

        //then
        assertThat(result.nameAlg).isEqualTo(TpmAlg.TPM_ALG_SHA256);
        assertThat(result.type).isEqualTo(TpmAlg.TPM_ALG_RSA);
        assertThat(result.typeVerifier).isOfAnyClassIn(RsaTypeVerifier.class);
        assertThat(result.objectAttrs).isEqualTo(394354);
        assertThat(result.authPolicy).isEqualTo(new byte[]{-99, -1, -53, -13, 108, 56, 58, -26, -103, -5, -104, 104,
                -36, 109, -53, -119, -41, 21, 56, -124, -66, 40, 3, -110, 44, 18, 65, 88, -65, -83, 34, -82});
        assertThat(uniqueParameter.verifyUniqueParameter(publicKey)).isTrue();
    }

}
