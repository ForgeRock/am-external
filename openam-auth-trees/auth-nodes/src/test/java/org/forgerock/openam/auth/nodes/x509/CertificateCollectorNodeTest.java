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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.COLLECTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.NOT_COLLECTED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.security.X509Decoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@ExtendWith(MockitoExtension.class)
public class CertificateCollectorNodeTest {

    private static final String HEADER_NAME = "testHeaderName";

    @Mock
    private CertificateCollectorNode.Config config;

    @Mock
    private X509Decoder certificateDecoder;

    @InjectMocks
    private CertificateCollectorNode certificateCollectorNode;

    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        request = mock(HttpServletRequest.class);
    }

    @Test
    void shouldNotCollectIfCollectionMethodEitherAndHeaderAndRequestNotProvided()
            throws NodeProcessException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    void shouldCollectIfCollectionMethodEitherAndRequestProvided() throws NodeProcessException {
        X509Certificate certificate = generateCASignedCertificate("uid=user");
        X509Certificate[] x509Certificates = new X509Certificate[]{certificate};

        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(request.getAttribute("jakarta.servlet.request.X509Certificate")).willReturn(x509Certificates);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    void shouldNotCollectIfCollectionMethodHeaderAndHeaderNotProvided()
            throws NodeProcessException {
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    void shouldCollectIfCollectionMethodHeaderAndBase64HeaderProvided()
            throws NodeProcessException, CertificateException {
        // Given
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));

        X509Certificate certificate = generateCASignedCertificate("uid=user");
        String encodedCertificate = encodeBase64String(certificate.getEncoded());
        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                HEADER_NAME, encodedCertificate
        );
        TreeContext treeContext = getContext(json(object()), headers);
        given(certificateDecoder.decodeCertificate(encodedCertificate)).willReturn(certificate);

        // When
        Action action = certificateCollectorNode.process(treeContext);

        // Then
        assertThat(action.outcome).isEqualTo(COLLECTED.name());
        JsonValue collectedCertificateJson = treeContext.getStateFor(certificateCollectorNode).get("X509Certificate");
        assertThat(collectedCertificateJson).isNotNull();
        assertThat(collectedCertificateJson.get(0).getObject()).isEqualTo(certificate);
    }

    @Test
    void shouldCollectIfCollectionMethodHeaderAndPemHeaderProvided()
            throws NodeProcessException, CertificateException {
        // Given
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));

        X509Certificate certificate = generateCASignedCertificate("uid=user");
        String pemEncodedCertificate = "-----BEGIN CERTIFICATE-----\n%s\n-----END CERTIFICATE-----"
                .formatted(encodeBase64String(certificate.getEncoded()));
        ListMultimap<String, String> headers = ImmutableListMultimap.of(HEADER_NAME, pemEncodedCertificate);
        TreeContext treeContext = getContext(json(object()), headers);
        given(certificateDecoder.decodeCertificate(pemEncodedCertificate)).willReturn(certificate);

        // When
        Action action = certificateCollectorNode.process(treeContext);

        // Then
        assertThat(action.outcome).isEqualTo(COLLECTED.name());
        JsonValue collectedCertificateJson = treeContext.getStateFor(certificateCollectorNode).get("X509Certificate");
        assertThat(collectedCertificateJson).isNotNull();
        assertThat(collectedCertificateJson.get(0).getObject()).isEqualTo(certificate);
    }

    @Test
    void shouldCollectIfCollectionMethodHeaderAndDerHeaderProvided()
            throws NodeProcessException, CertificateException {
        // Given
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));

        X509Certificate certificate = generateCASignedCertificate("uid=user");
        String derEncodedCertificate = ":%s:".formatted(encodeBase64String(certificate.getEncoded()));
        ListMultimap<String, String> headers = ImmutableListMultimap.of(HEADER_NAME, derEncodedCertificate);
        TreeContext treeContext = getContext(json(object()), headers);
        given(certificateDecoder.decodeCertificate(derEncodedCertificate)).willReturn(certificate);

        // When
        Action action = certificateCollectorNode.process(treeContext);

        // Then
        assertThat(action.outcome).isEqualTo(COLLECTED.name());
        JsonValue collectedCertificateJson = treeContext.getStateFor(certificateCollectorNode).get("X509Certificate");
        assertThat(collectedCertificateJson).isNotNull();
        assertThat(collectedCertificateJson.get(0).getObject()).isEqualTo(certificate);
    }

    @Test
    void shouldCollectIfCollectionMethodHeaderAndDerHeaderChainProvided()
            throws NodeProcessException, CertificateException {
        // Given
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));

        X509Certificate userCertificate = generateCASignedCertificate("uid=user");
        X509Certificate rootCertificate = generateCASignedCertificate("uid=root");
        String derEncodedCertificateChain = ":%s:, :%s:".formatted(
                encodeBase64String(userCertificate.getEncoded()),
                encodeBase64String(rootCertificate.getEncoded()));
        ListMultimap<String, String> headers = ImmutableListMultimap.of(HEADER_NAME, derEncodedCertificateChain);
        TreeContext treeContext = getContext(json(object()), headers);
        given(certificateDecoder.decodeCertificate(derEncodedCertificateChain)).willReturn(userCertificate);


        // When
        Action action = certificateCollectorNode.process(treeContext);

        // Then
        assertThat(action.outcome).isEqualTo(COLLECTED.name());
        JsonValue collectedCertificateJson = treeContext.getStateFor(certificateCollectorNode).get("X509Certificate");
        assertThat(collectedCertificateJson).isNotNull();
        assertThat(collectedCertificateJson.size()).isEqualTo(1);
        assertThat(collectedCertificateJson.get(0).getObject()).isEqualTo(userCertificate);
    }

    @Test
    void shouldCollectIfCollectionMethodEitherAndHeaderProvided()
            throws NodeProcessException, CertificateException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of(HEADER_NAME));

        X509Certificate certificate = generateCASignedCertificate("uid=user");

        String encodedCertificate = new String(Base64.encodeBase64(certificate.getEncoded()));
        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                HEADER_NAME, encodedCertificate);
        TreeContext treeContext = getContext(json(object()), headers);
        given(certificateDecoder.decodeCertificate(encodedCertificate))
                .willReturn(certificate);

        // When
        Action action = certificateCollectorNode.process(treeContext);

        // Then
        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    void shouldCollectIfCollectionMethodRequestAndRequestProvided() throws NodeProcessException {
        X509Certificate certificate = generateCASignedCertificate("uid=user");
        X509Certificate[] x509Certificates = new X509Certificate[]{certificate};

        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.REQUEST);
        given(request.getAttribute("jakarta.servlet.request.X509Certificate")).willReturn(x509Certificates);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    void shouldNotCollectIfCollectionMethodRequestAndRequestNotProvided()
            throws NodeProcessException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.REQUEST);
        given(request.getAttribute("jakarta.servlet.request.X509Certificate")).willReturn(null);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    void shouldThrowExceptionIfNoClientCertificateHttpHeaderNameInConfigAndHeaderTypeSet() {
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.empty());

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        assertThatThrownBy(() -> certificateCollectorNode.process(treeContext))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionIfNoClientCertificateHttpHeaderNameInConfigAndEitherTypeSetWithNoCertInRequest() {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.empty());

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        assertThatThrownBy(() -> certificateCollectorNode.process(treeContext))
                .isInstanceOf(IllegalStateException.class);
    }

    private TreeContext getContext(JsonValue transientState, ListMultimap<String, String> headers) {
        return new TreeContext(json(object()), transientState, new ExternalRequestContext.Builder()
                .clientIp("trustedHost")
                .servletRequest(request)
                .headers(headers)
                .build(),
                emptyList(), Optional.empty());
    }

    private static X509Certificate generateCASignedCertificate(String principalName) {
        KeyPair keyPair = generateKeyPair();
        Date expiryDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
        try {
            X500Principal subjectAndIssuer = new X500Principal(principalName);
            BigInteger serial = new BigInteger(159, new SecureRandom());
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subjectAndIssuer, serial, new Date(), expiryDate,
                    subjectAndIssuer, keyPair.getPublic());
            X509CertificateHolder certHolder =
                    certBuilder.build(new JcaContentSignerBuilder("SHA256WithECDSA")
                            .build(keyPair.getPrivate()));

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create CA-signed certificate", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("unable to generate test key-pair", e);
        }
    }
}
