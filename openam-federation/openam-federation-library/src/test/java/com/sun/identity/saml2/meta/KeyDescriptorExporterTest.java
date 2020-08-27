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
 * Copyright 2020 ForgeRock AS.
 */

package com.sun.identity.saml2.meta;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.KeyTypes;
import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import com.sun.identity.saml2.jaxb.xmlenc11.MGFType;
import com.sun.identity.saml2.jaxb.xmlsig.DigestMethodType;
import com.sun.identity.saml2.jaxb.xmlsig.X509DataType;

public class KeyDescriptorExporterTest {

    @Mock
    private Function<String, X509Certificate> keyAliasToCertificate;
    @Mock
    private X509Certificate certificate1;
    @Mock
    private X509Certificate certificate2;
    @Mock
    private PublicKey publicKey;
    @Mock
    private Saml2CredentialResolver saml2CredentialResolver;

    private final byte[] encodedCertificate = "some-certificate".getBytes();
    private KeyDescriptorExporter exporter;

    @BeforeMethod
    public void setup() throws CertificateEncodingException {
        MockitoAnnotations.initMocks(this);
        exporter = new KeyDescriptorExporter(saml2CredentialResolver);

        given(keyAliasToCertificate.apply("some-key-alias")).willReturn(certificate1);
        given(certificate1.getEncoded()).willReturn(encodedCertificate);
        given(certificate1.getPublicKey()).willReturn(publicKey);
        given(certificate2.getEncoded()).willReturn(encodedCertificate);
        given(certificate2.getPublicKey()).willReturn(publicKey);
        given(publicKey.getAlgorithm()).willReturn("RSA");

        XmlSecurity.init();
    }

    @Test
    public void shouldReturnSigningKeyDescriptorWhenSigningSecretIsPresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidSigningCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(1);
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));

        assertThat(keyDescriptor.getUse()).isEqualTo(KeyTypes.SIGNING);

        assertThat(keyDescriptor.getKeyInfo().getContent()).hasSize(1);
        X509DataType data = (X509DataType) JAXBIntrospector.getValue(keyDescriptor.getKeyInfo().getContent().get(0));

        assertThat(data.getX509IssuerSerialOrX509SKIOrX509SubjectName()).hasSize(1);
        byte[] actualEncoding = (byte[]) JAXBIntrospector.getValue(
                data.getX509IssuerSerialOrX509SKIOrX509SubjectName().get(0));

        assertThat(actualEncoding).isEqualTo(encodedCertificate);
    }

    @Test
    public void shouldReturnSigningKeyDescriptorsWhenMultipleSigningSecretsArePresent() throws SAML2Exception {
        given(saml2CredentialResolver.resolveValidSigningCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Set.of(certificate1, certificate2));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(2);

        List<KeyTypes> keyTypes = keyDescriptors.stream()
                .map(JAXBElement::getValue)
                .map(KeyDescriptorType::getUse)
                .distinct()
                .collect(toList());

        assertThat(keyTypes).hasSize(1);
        assertThat(keyTypes).containsOnly(KeyTypes.SIGNING);
    }

    @Test
    public void shouldReturnKeyDescriptorsWhenPassedValidEncryptionAlgorithms() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        Map<String, List<String>> encryptionAlgorithms = ImmutableMap.of("encryptionAlgorithms",
                ImmutableList.<String>builder()
                // Data encryption algorithm
                .add("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                // Key transport algorithm
                .add("http://www.w3.org/2009/xmlenc11#rsa-oaep")
                .build());
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, encryptionAlgorithms);

        // Then
        assertThat(keyDescriptors).hasSize(1);
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        List<EncryptionMethodType> encryptionMethods = keyDescriptor.getEncryptionMethod();

        assertThat(encryptionMethods).hasSize(2);
        assertThat(encryptionMethods).extracting("algorithm")
                .containsExactly("http://www.w3.org/2009/xmlenc11#rsa-oaep",
                        "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
    }

    @Test
    public void shouldReturnEqualKeyDescriptorsWhenMultipleSecretsArePresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Set.of(certificate1, certificate2));

        // When
        Map<String, List<String>> encryptionAlgorithms = ImmutableMap.of("encryptionAlgorithms",
                ImmutableList.<String>builder()
                        // Data encryption algorithm
                        .add("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                        // Key transport algorithm
                        .add("http://www.w3.org/2009/xmlenc11#rsa-oaep")
                        .build());

        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, encryptionAlgorithms);

        // Then
        assertThat(keyDescriptors).hasSize(2);
    }

    @Test
    public void shouldContainKeySizeWhenDataEncryptionAlgorithmIsPresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        // Data encryption algorithm
        Map<String, List<String>> encryptionAlgorithms = ImmutableMap.of("encryptionAlgorithms",
                Collections.singletonList("http://www.w3.org/2001/04/xmlenc#aes128-cbc"));

        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, encryptionAlgorithms);

        // Then
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        EncryptionMethodType encryptionMethod = keyDescriptor.getEncryptionMethod().stream()
                .filter(method -> "http://www.w3.org/2001/04/xmlenc#aes128-cbc".equals(method.getAlgorithm()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find encryption method"));

        assertThat(encryptionMethod.getContent()).hasSize(1);
        BigInteger actualKeySize = (BigInteger) JAXBIntrospector.getValue(encryptionMethod.getContent().get(0));
        assertThat(actualKeySize.intValue()).isEqualTo(128);
    }

    @Test
    public void shouldContainDigestMethodWhenTransportEncryptionAlgorithmIsPresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        // Key transport algorithm
        Map<String, List<String>> encryptionAlgorithms = ImmutableMap.of("encryptionAlgorithms",
                Collections.singletonList("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"));
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, encryptionAlgorithms);

        // Then
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        EncryptionMethodType encryptionMethod = keyDescriptor.getEncryptionMethod().stream()
                .filter(method -> "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p".equals(method.getAlgorithm()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find encryption method"));

        assertThat(encryptionMethod.getContent()).hasSize(1);
        DigestMethodType digestMethod = (DigestMethodType) JAXBIntrospector
                .getValue(encryptionMethod.getContent().get(0));
        assertThat(digestMethod.getAlgorithm()).isEqualTo("http://www.w3.org/2001/04/xmlenc#sha256");
    }

    @Test
    public void shouldIncludeMgfIfNotPresentWhenRsaTransportEncryptionAlgorithmIsPresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        // Key transport algorithm without MGF function
        Map<String, List<String>> encryptionAlgorithms = ImmutableMap.of("encryptionAlgorithms",
                Collections.singletonList("http://www.w3.org/2009/xmlenc11#rsa-oaep"));
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, encryptionAlgorithms);

        // Then
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        EncryptionMethodType encryptionMethod = keyDescriptor.getEncryptionMethod().stream()
                .filter(method -> "http://www.w3.org/2009/xmlenc11#rsa-oaep".equals(method.getAlgorithm()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find encryption method"));

        assertThat(encryptionMethod.getContent()).hasSize(2);

        MGFType mgf = encryptionMethod.getContent().stream()
                .map(JAXBIntrospector::getValue)
                .filter(object -> object instanceof MGFType)
                .map(MGFType.class::cast)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find the MGF entry"));

        assertThat(mgf.getAlgorithm()).isEqualTo("http://www.w3.org/2009/xmlenc11#mgf1sha256");
    }

    @Test
    public void shouldReturnDefaultAlgorithmsWhenNoEncryptionOrTransportAlgorithmsArePresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(1);
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        List<EncryptionMethodType> encryptionMethods = keyDescriptor.getEncryptionMethod();

        assertThat(encryptionMethods).hasSize(2);
                assertThat(encryptionMethods).extracting("algorithm")
                .containsExactly("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p",
                        "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
    }

    @Test
    public void shouldReturnOnlySigningKeyDescriptorsWhenNoEncryptionSecretsArePresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidSigningCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(1);
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        assertThat(keyDescriptor.getUse()).isEqualTo(KeyTypes.SIGNING);
    }

    @Test
    public void shouldReturnOnlyEncryptionKeyDescriptorsWhenNoSigningSecretsArePresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(1);
        KeyDescriptorType keyDescriptor = (KeyDescriptorType) JAXBIntrospector.getValue(keyDescriptors.get(0));
        assertThat(keyDescriptor.getUse()).isEqualTo(KeyTypes.ENCRYPTION);
    }

    @Test
    public void shouldReturnBothSigningAndEncryptionKeyDescriptorsWhenBothSecretsArePresent() throws SAML2Exception {
        // Given
        given(saml2CredentialResolver.resolveValidSigningCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate1));
        given(saml2CredentialResolver.resolveValidEncryptionCredentials("test-realm","test-entity",
                Saml2EntityRole.SP)).willReturn(Collections.singleton(certificate2));

        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors("test-realm","test-entity",
                Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(2);
        List<KeyTypes> keyTypes = keyDescriptors.stream()
                .map(JAXBElement::getValue)
                .map(KeyDescriptorType::getUse)
                .collect(toList());

        assertThat(keyTypes).containsExactly(KeyTypes.SIGNING, KeyTypes.ENCRYPTION);
    }

    @Test
    public void shouldReturnNoSigningAndEncryptionKeyDescriptorsWhenBothSecretsAreAbsent() throws SAML2Exception {
        // When
        List<KeyDescriptorElement> keyDescriptors = exporter.createKeyDescriptors(
                "test-realm","test-entity", Saml2EntityRole.SP, Collections.emptyMap());

        // Then
        assertThat(keyDescriptors).hasSize(0);
    }
}