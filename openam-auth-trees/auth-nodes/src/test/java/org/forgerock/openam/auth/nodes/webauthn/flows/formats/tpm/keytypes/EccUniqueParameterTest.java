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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;

import org.testng.annotations.Test;

/**
 * Test for the {@link EccUniqueParameter} class.
 */
public class EccUniqueParameterTest {
    @Test
    public void testValidateUniqueParameter() {
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        runTest(x, y, x, y, true);
    }

    @Test
    public void testValidateUniqueParameterWrongValues() {
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        BigInteger x2 = new BigInteger("1DD8CF685E90E8FF8B9BCABC6EA56C61A039AD8CC469677FA05BCA53A778D87", 16);
        BigInteger y2 = new BigInteger("3F454152FE83A283FD4CEC62EEA8D7160CCFC12402C465605161D0E318D130", 16);
        runTest(x, y, x2, y2, false);
    }

    private void runTest(final BigInteger uniqueX, final BigInteger uniqueY, final BigInteger ecKeyX, final BigInteger
            ecKeyY, final boolean expectedOutcome) {
        //given
        byte[][] unique = new byte[][]{
                uniqueX.toByteArray(),
                uniqueY.toByteArray()
        };
        EccUniqueParameter eccUniqueParameter = new EccUniqueParameter(unique[0], unique[1]);

        ECPoint ecPoint = mock(ECPoint.class);
        ECPublicKey publicKey = mock(ECPublicKey.class);
        when(ecPoint.getAffineX()).thenReturn(ecKeyX);
        when(ecPoint.getAffineY()).thenReturn(ecKeyY);
        when(publicKey.getW()).thenReturn(ecPoint);

        //then
        assertThat(eccUniqueParameter.verifyUniqueParameter(publicKey)).isEqualTo(expectedOutcome);
    }
}
