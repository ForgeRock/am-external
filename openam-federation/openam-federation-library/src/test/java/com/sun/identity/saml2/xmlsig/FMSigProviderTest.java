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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.xmlsig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.forgerock.json.jose.utils.BigIntegerUtils;
import org.forgerock.util.encode.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.primitives.Bytes;
import com.sun.identity.saml2.common.SAML2Exception;

public class FMSigProviderTest {

    private static KeyPair ecdsaKeyPair;

    private static X509Certificate certificate;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();

        certificate = mock(X509Certificate.class);
        given(certificate.getPublicKey()).willReturn(ecdsaKeyPair.getPublic());
        given(certificate.getSubjectDN()).willReturn(new X500Principal("cn=test"));
        given(certificate.getSerialNumber()).willReturn(BigInteger.ONE);
    }

    public static Object[][] invalidEcdsaSignatureValues() {
        BigInteger order = ((ECPublicKey) ecdsaKeyPair.getPublic()).getParams().getOrder();
        byte[] tooBig = BigIntegerUtils.toBytesUnsigned(order);
        byte[] tooSmall = new byte[32];
        byte[] justRight = BigIntegerUtils.toBytesUnsigned(order.subtract(BigInteger.ONE));
        return new Object[][] {
                { Bytes.concat(tooSmall, tooSmall) },
                { Bytes.concat(tooSmall, justRight) },
                { Bytes.concat(justRight, tooSmall) },
                { Bytes.concat(tooBig, tooBig) },
                { Bytes.concat(tooBig, justRight) },
                { Bytes.concat(justRight, tooBig) },
        };
    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatureValues")
    public void shouldRejectInvalidEcdsaSignatureValues(byte[] invalidSignature) {
        // Given
        FMSigProvider sigProvider = new FMSigProvider();
        String sigdata = "<test ID='foo' xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" >test<ds:Signature " +
                "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod " +
                "Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
                "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256\"/>" +
                "<ds:Reference URI=\"#foo\"><ds:Transforms>" +
                "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>" +
                "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms>" +
                "<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
                "<ds:DigestValue>EQvOjCzmPLg1AzyJssUQ8OH1WEUbPXpQA/Xj8mIXMec=</ds:DigestValue></ds:Reference>" +
                "</ds:SignedInfo>" +
                "<ds:SignatureValue>" + Base64.encode(invalidSignature) + "</ds:SignatureValue>" +
                "</ds:Signature></test>";

        // When/then
        assertThatThrownBy(() -> sigProvider.verify(sigdata, "foo", Set.of(certificate)))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Invalid signature value");
    }
}
