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
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmEccCurve;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Test for the {@link EccTypeVerifier} class.
 */
public class EccTypeVerifierTest {

    @Test
    void testParseToType() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 0, 1, 0, 16};

        //when
        EccTypeVerifier result = EccTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.getCurveId()).isEqualTo(TpmEccCurve.TPM_ECC_NIST_P192);
        assertThat(result.getKdf()).isEqualTo(TpmAlg.TPM_ALG_NULL);
        assertThat(result.getScheme()).isEqualTo(TpmAlg.TPM_ALG_NULL);
        assertThat(result.getSymmetric()).isEqualTo(TpmAlg.TPM_ALG_NULL);
    }

    @Test
    void testVerify() throws IOException {
        //given
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        EccUniqueParameter eccUniqueParameter = getEccUniqueParameter(x, y);
        JsonValue jsonValue = json(object());
        EcJWK jwk = mockEcJWK(jsonValue, x, y, KeyType.EC);

        // when
        EccTypeVerifier result = EccTypeVerifier.parseToType(new byte[]{0, 16, 0, 16, 0, 3, 0, 16});
        when(jwk.getEllipticCurve()).thenReturn(result.getCurveId().getSupportedEllipticCurve());

        try (MockedStatic<EcJWK> mockEcJWK = Mockito.mockStatic(EcJWK.class)) {
            mockEcJWK.when(() -> EcJWK.parse(jsonValue)).thenReturn(jwk);

            //then
            assertThat(result.verify(jwk, eccUniqueParameter)).isTrue();
        }
    }

    @Test
    void testVerifyFailsWhenSupportedEllipticCurveIsInvalid() throws IOException {
        //given
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        EccUniqueParameter eccUniqueParameter = getEccUniqueParameter(x, y);
        JsonValue jsonValue = json(object());
        EcJWK jwk = mockEcJWK(jsonValue, x, y, KeyType.EC);

        // when
        EccTypeVerifier result = EccTypeVerifier.parseToType(new byte[]{0, 16, 0, 16, 0, 3, 0, 16});
        when(jwk.getEllipticCurve()).thenReturn(null);

        try (MockedStatic<EcJWK> mockEcJWK = Mockito.mockStatic(EcJWK.class)) {
            mockEcJWK.when(() -> EcJWK.parse(jsonValue)).thenReturn(jwk);

            //then
            assertThat(result.verify(jwk, eccUniqueParameter)).isFalse();
        }
    }

    @Test
    void testVerifyFailsWhenKeyTypeIsInvalid() throws IOException {
        //given
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        EccUniqueParameter eccUniqueParameter = getEccUniqueParameter(x, y);
        JsonValue jsonValue = json(object());
        EcJWK jwk = mockEcJWK(jsonValue, x, y, KeyType.RSA);

        // when
        EccTypeVerifier result = EccTypeVerifier.parseToType(new byte[]{0, 16, 0, 16, 0, 3, 0, 16});
        when(jwk.getEllipticCurve()).thenReturn(result.getCurveId().getSupportedEllipticCurve());

        try (MockedStatic<EcJWK> mockEcJWK = Mockito.mockStatic(EcJWK.class)) {
            mockEcJWK.when(() -> EcJWK.parse(jsonValue)).thenReturn(jwk);

            //then
            assertThat(result.verify(jwk, eccUniqueParameter)).isFalse();
        }
    }

    @Test
    void testVerifyFailsWhenJWKEllipticCurveIsInvalid() throws IOException {
        //given
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        EccUniqueParameter eccUniqueParameter = getEccUniqueParameter(x, y);
        JsonValue jsonValue = json(object());
        EcJWK jwk = mockEcJWK(jsonValue, x, y, KeyType.EC);

        // when
        EccTypeVerifier result = EccTypeVerifier.parseToType(new byte[]{0, 16, 0, 16, 0, 3, 0, 16});
        when(jwk.getEllipticCurve()).thenReturn(null);

        try (MockedStatic<EcJWK> mockEcJWK = Mockito.mockStatic(EcJWK.class)) {
            mockEcJWK.when(() -> EcJWK.parse(jsonValue)).thenReturn(jwk);

            //then
            assertThat(result.verify(jwk, eccUniqueParameter)).isFalse();
        }
    }

    @Test
    void testVerifyFailsWhenUniqueParameterIsInvalid() throws IOException {
        //given
        BigInteger x = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        BigInteger y = new BigInteger("1D255EDB5170A9201DB93DB45DEC75CF94E41113A130999790F4918586ACD89", 16);
        EccUniqueParameter eccUniqueParameter = getEccUniqueParameter(
                new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B5F", 16), y);
        JsonValue jsonValue = json(object());
        EcJWK jwk = mockEcJWK(jsonValue, x, y, KeyType.EC);

        // when
        EccTypeVerifier result = EccTypeVerifier.parseToType(new byte[]{0, 16, 0, 16, 0, 3, 0, 16, 0});
        when(jwk.getEllipticCurve()).thenReturn(result.getCurveId().getSupportedEllipticCurve());

        try (MockedStatic<EcJWK> mockEcJWK = Mockito.mockStatic(EcJWK.class)) {
            mockEcJWK.when(() -> EcJWK.parse(jsonValue)).thenReturn(jwk);

            //then
            assertThat(result.verify(jwk, eccUniqueParameter)).isFalse();
        }
    }

    @Test
    void testGetUniqueParameter() throws IOException, InvalidTpmtPublicException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 0, 1, 0, 16};
        byte[] pubAreaBytes = new byte[]{0, 3, 1, 2, 3, 0, 3, 1, 2, 3};
        EccUniqueParameter eccUniqueParameter = new EccUniqueParameter(new byte[]{1, 2, 3}, new byte[]{1, 2, 3});

        DataInputStream pubArea = new DataInputStream(new ByteArrayInputStream(pubAreaBytes));

        //when
        EccTypeVerifier result = EccTypeVerifier.parseToType(paramBytes);
        TpmtUniqueParameter uniqueParameter = result.getUniqueParameter(pubArea);

        //then
        assertThat(uniqueParameter).isInstanceOf(EccUniqueParameter.class);
        assertThat((EccUniqueParameter) uniqueParameter).isEqualTo(eccUniqueParameter);
    }

    @Test
    void testGetUniqueParameterThrowsException() throws IOException, InvalidTpmtPublicException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 0, 1, 0, 16};
        byte[] pubAreaBytes = new byte[]{0, 3, 1, 2, 3, 0, 3, 1, 2, 3, 0};
        DataInputStream pubArea = new DataInputStream(new ByteArrayInputStream(pubAreaBytes));

        //when
        EccTypeVerifier result = EccTypeVerifier.parseToType(paramBytes);
        assertThatThrownBy(() -> result.getUniqueParameter(pubArea))
                //then
                .isInstanceOf(InvalidTpmtPublicException.class)
                .hasMessageContaining("Bytes remaining in pubArea after parsing tpmtPublic!");
    }

    private EccUniqueParameter getEccUniqueParameter(BigInteger x, BigInteger y) {
        byte[][] unique = new byte[][]{
                x.toByteArray(),
                y.toByteArray()
        };
        return new EccUniqueParameter(unique[0], unique[1]);
    }

    private EcJWK mockEcJWK(JsonValue jsonValue, BigInteger x, BigInteger y, KeyType keyType) {
        ECPoint ecPoint = mock(ECPoint.class);
        ECPublicKey publicKey = mock(ECPublicKey.class);
        when(ecPoint.getAffineX()).thenReturn(x);
        when(ecPoint.getAffineY()).thenReturn(y);
        when(publicKey.getW()).thenReturn(ecPoint);

        EcJWK jwk = mock(EcJWK.class);
        when(jwk.toJsonValue()).thenReturn(jsonValue);
        when(jwk.getKeyType()).thenReturn(keyType);
        when(jwk.toECPublicKey()).thenReturn(publicKey);

        return jwk;
    }
}
