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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.xml.bind.JAXBIntrospector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.saml2.jaxb.xmlsig.X509DataType;

/**
 * Unit test for {@link KeyDescriptorUtil}.
 *
 * @since 7.0.0
 */
@ExtendWith(MockitoExtension.class)
public final class KeyDescriptorUtilTest {

    @Mock
    private X509Certificate certificate;


    @Test
    void whenCertificatePassedShouldReturnValidKeyInfo() throws CertificateEncodingException {
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

    @Test
    void whenCertificateEncodingFailsShouldThrowException() {
        assertThatThrownBy(() -> {
            // Given
            given(certificate.getEncoded()).willThrow(CertificateEncodingException.class);

            // When
            KeyDescriptorUtil.createKeyInfoFromCertificate(certificate);
        }).isInstanceOf(IllegalStateException.class);
    }

}
