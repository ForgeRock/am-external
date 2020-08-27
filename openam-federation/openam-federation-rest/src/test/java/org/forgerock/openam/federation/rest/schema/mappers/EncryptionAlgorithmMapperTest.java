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
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;
import static org.mockito.BDDMockito.given;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Function;

import org.forgerock.openam.federation.rest.schema.shared.EncryptionAlgorithm;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.service.CurrentSourceContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;

/**
 * Unit test for {@link EncryptionAlgorithmMapper}.
 *
 * @since 7.0.0
 */
public final class EncryptionAlgorithmMapperTest {

    @Mock
    private Function<String, X509Certificate> keyAliasToCertificate;
    @Mock
    private X509Certificate certificate;

    private EnricherContext parentContext;

    private EncryptionAlgorithmMapper mapper;

    @BeforeMethod
    public void setup() throws CertificateEncodingException {
        MockitoAnnotations.initMocks(this);
        mapper = new EncryptionAlgorithmMapper();

        given(keyAliasToCertificate.apply("some-key-alias")).willReturn(certificate);
        parentContext = new CurrentSourceContext(ROOT, emptyList(), new TypeToken<List<KeyDescriptorElement>>() { });

        XmlSecurity.init();
    }

    @Test
    public void shouldReturnCorrespondingEncryptionAlgorithmsWhenPassedStringAlgorithms() {
        // When
        List<EncryptionAlgorithm> algorithms = mapper.map(
                List.of("http://www.w3.org/2009/xmlenc11#rsa-oaep"), parentContext);

        // Then
        assertThat(algorithms).hasSize(1);
        assertThat(algorithms).containsExactly(EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_RSA_OAEP);
    }

    @Test
    public void shouldReturnDistinctEncryptionAlgorithmsWhenPassedDuplicateStringAlgorithms() {
        // When
        List<EncryptionAlgorithm> algorithms = mapper.map(
                List.of("http://www.w3.org/2009/xmlenc11#rsa-oaep",
                        "http://www.w3.org/2009/xmlenc11#rsa-oaep",
                        "http://www.w3.org/2009/xmlenc11#aes128-gcm"), parentContext);

        // Then
        assertThat(algorithms).hasSize(2);
        assertThat(algorithms).containsExactly(EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_RSA_OAEP,
                EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_AES_128_GCM);
    }

    @Test
    public void shouldReturnCorrespondingStringAlgorithmsWhenPassedEncryptionAlgorithms() {
        // When
        List<String> algorithms = mapper.inverse(
                List.of(EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_RSA_OAEP), parentContext);

        // Then
        assertThat(algorithms).hasSize(1);
        assertThat(algorithms).containsExactly("http://www.w3.org/2009/xmlenc11#rsa-oaep");
    }

    @Test
    public void shouldReturnDistinctStringAlgorithmsWhenPassedDuplicateEncryptionAlgorithms() {
        // When
        List<String> algorithms = mapper.inverse(
                List.of(EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_RSA_OAEP,
                        EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_AES_128_GCM,
                        EncryptionAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLENC_11_RSA_OAEP), parentContext);

        // Then
        assertThat(algorithms).hasSize(2);
        assertThat(algorithms).containsExactly("http://www.w3.org/2009/xmlenc11#rsa-oaep",
                "http://www.w3.org/2009/xmlenc11#aes128-gcm");
    }
}
