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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmAlg;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link RsaTypeVerifier} class.
 */
public class RsaTypeVerifierTest {

    @Test
    void testParseToType() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.getExponent()).isEqualTo(65537);
        assertThat(result.getKeyBits()).isEqualTo(new byte[]{8, 0});
        assertThat(result.getScheme()).isEqualTo(TpmAlg.TPM_ALG_NULL);
        assertThat(result.getSymmetric()).isEqualTo(TpmAlg.TPM_ALG_NULL);
    }

    @Test
    void testVerify() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};

        BigInteger modulus = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        byte[] unique = modulus.toByteArray();
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(unique);

        RsaJWK jwk = mockRsaJwk(modulus, KeyType.RSA, BigInteger.valueOf(65537));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.verify(jwk, rsaUniqueParameter)).isTrue();
    }

    @Test
    void testVerifyFailsWhenKeyTypeIsInvalid() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};

        BigInteger modulus = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        byte[] unique = modulus.toByteArray();
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(unique);

        RsaJWK jwk = mockRsaJwk(modulus, KeyType.EC, BigInteger.valueOf(65537));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.verify(jwk, rsaUniqueParameter)).isFalse();
    }

    @Test
    void testVerifyFailsWhenPublicExponentIsInvalid() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};

        BigInteger modulus = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        byte[] unique = modulus.toByteArray();
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(unique);

        RsaJWK jwk = mockRsaJwk(modulus, KeyType.RSA, BigInteger.valueOf(65538));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.verify(jwk, rsaUniqueParameter)).isFalse();
    }

    @Test
    void testVerifyFailsWhenUniqueParameterIsInvalid() throws IOException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};

        BigInteger modulus = new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B4F", 16);
        byte[] unique = modulus.toByteArray();
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(unique);

        RsaJWK jwk = mockRsaJwk(
                new BigInteger("3A444E97FFFA7D89B928A2D55573B097C2616AF88CD9F8B1501772FF7C6B5F", 16),
                KeyType.RSA, BigInteger.valueOf(65537));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);

        //then
        assertThat(result.verify(jwk, rsaUniqueParameter)).isFalse();
    }

    @Test
    void testGetUniqueParameter() throws IOException, InvalidTpmtPublicException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};
        byte[] pubAreaBytes = new byte[]{0, 3, 1, 2, 3};
        RsaUniqueParameter rsaUniqueParameter = new RsaUniqueParameter(new byte[]{1, 2, 3});
        DataInputStream pubArea = new DataInputStream(new ByteArrayInputStream(pubAreaBytes));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);
        TpmtUniqueParameter uniqueParameter = result.getUniqueParameter(pubArea);

        //then
        assertThat(uniqueParameter).isInstanceOf(RsaUniqueParameter.class);
        assertThat((RsaUniqueParameter) uniqueParameter).isEqualTo(rsaUniqueParameter);
    }

    @Test
    void testGetUniqueParameterThrowsException() throws IOException, InvalidTpmtPublicException {
        //given
        byte[] paramBytes = new byte[]{0, 16, 0, 16, 8, 0, 0, 0, 0, 0};
        byte[] pubAreaBytes = new byte[]{0, 3, 1, 2, 3, 0};
        DataInputStream pubArea = new DataInputStream(new ByteArrayInputStream(pubAreaBytes));

        //when
        RsaTypeVerifier result = RsaTypeVerifier.parseToType(paramBytes);
        assertThatThrownBy(() -> result.getUniqueParameter(pubArea))
                //then
                .isInstanceOf(InvalidTpmtPublicException.class)
                .hasMessageContaining("Bytes remaining in pubArea after parsing tpmtPublic!");
    }

    private RsaJWK mockRsaJwk(BigInteger modulus, KeyType keyType, BigInteger publicExponent) {
        RSAPublicKey publicKey = mock(RSAPublicKey.class);
        when(publicKey.getPublicExponent()).thenReturn(publicExponent);
        when(publicKey.getModulus()).thenReturn(modulus);

        RsaJWK jwk = mock(RsaJWK.class);
        when(jwk.getKeyType()).thenReturn(keyType);
        when(jwk.toRSAPublicKey()).thenReturn(publicKey);

        return jwk;
    }
}
