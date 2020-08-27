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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.saml2.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.xml.bind.JAXBIntrospector;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.saml2.jaxb.xmlsig.X509DataType;

/**
 * Unit test for {@link KeyDescriptorUtil}.
 *
 * @since 7.0.0
 */
public final class KeyDescriptorUtilTest {

    @Mock
    private X509Certificate certificate;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void whenCertificatePassedShouldReturnValidKeyInfo() throws CertificateEncodingException {
        // Given
        byte[] encodedCertificate = "some-certificate".getBytes();
        given(certificate.getEncoded()).willReturn(encodedCertificate);

        // When
        KeyInfoType keyInfo = KeyDescriptorUtil.createKeyInfoFromCertificate(certificate);

        // Then
        assertThat(keyInfo).isNotNull();
        assertThat(keyInfo.getContent()).hasSize(1);

        X509DataType data = (X509DataType) JAXBIntrospector.getValue(keyInfo.getContent().get(0));
        assertThat(data.getX509IssuerSerialOrX509SKIOrX509SubjectName()).hasSize(1);

        byte[] actualEncoding = (byte[]) JAXBIntrospector.getValue(
                data.getX509IssuerSerialOrX509SKIOrX509SubjectName().get(0));

        assertThat(actualEncoding).isEqualTo(encodedCertificate);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void whenCertificateEncodingFailsShouldThrowException() throws CertificateEncodingException {
        // Given
        given(certificate.getEncoded()).willThrow(CertificateEncodingException.class);

        // When
        KeyDescriptorUtil.createKeyInfoFromCertificate(certificate);
    }

}