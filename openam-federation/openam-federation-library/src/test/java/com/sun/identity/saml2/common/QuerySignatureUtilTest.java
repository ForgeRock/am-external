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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import static com.sun.identity.saml2.common.QuerySignatureUtil.isValidSignature;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.forgerock.json.jose.utils.DerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QuerySignatureUtilTest {

    @Mock
    private X509Certificate certificate;

    @BeforeEach
    void setup() throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        given(certificate.getPublicKey()).willReturn(keyPair.getPublic());
        given(certificate.getSubjectDN()).willReturn(new X500Principal("cn=test"));
        given(certificate.getSerialNumber()).willReturn(BigInteger.ONE);
    }

    @Test
    void shouldRejectInvalidEcdsaSignatureValue() throws Exception {
        Signature signature = Signature.getInstance("SHA256WithECDSA");
        byte[] invalidSig = DerUtils.encodeEcdsaSignature(new byte[64]);

        assertThatThrownBy(() -> isValidSignature(signature, Set.of(certificate), new byte[0], invalidSig))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Invalid signature value");
    }
}
