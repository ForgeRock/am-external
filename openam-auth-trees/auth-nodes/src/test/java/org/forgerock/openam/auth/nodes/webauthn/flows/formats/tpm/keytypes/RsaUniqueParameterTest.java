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
import java.security.interfaces.RSAPublicKey;

import org.testng.annotations.Test;

/**
 * Test for the {@link RsaUniqueParameter} class.
 */
public class RsaUniqueParameterTest {
    @Test
    public void testValidateUniqueParameter() {
        BigInteger m = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        runTest(m, m, true);
    }

    @Test
    public void testValidateUniqueParameterWrongValue() {
        BigInteger m = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger m2 = new BigInteger("1DD8CF685E90E8FF8B9BCABC6EA56C61A039AD8CC469677FA05BCA53A778D87", 16);
        runTest(m, m2, false);
    }

    private void runTest(final BigInteger uniqueModulus, final BigInteger rsaKeyModulus,
            final boolean expectedOutcome) {
        //given
        byte[] unique = uniqueModulus.toByteArray();
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(unique);

        RSAPublicKey publicKey = mock(RSAPublicKey.class);
        when(publicKey.getModulus()).thenReturn(rsaKeyModulus);

        //then
        assertThat(rsaUniqueParameter.verifyUniqueParameter(publicKey)).isEqualTo(expectedOutcome);
    }
}
