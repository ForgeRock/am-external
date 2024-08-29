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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.COLLECTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.NOT_COLLECTED;
import static org.mockito.BDDMockito.given;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@RunWith(MockitoJUnitRunner.class)
public class CertificateCollectorNodeTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private CertificateCollectorNode.Config config;
    private CertificateCollectorNode certificateCollectorNode;

    @Before
    public void setup() {
        certificateCollectorNode = new CertificateCollectorNode(config);
    }

    @Test
    public void shouldNotCollectIfCollectionMethodEitherAndHeaderAndRequestNotProvided()
            throws NodeProcessException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of("testHeaderName"));

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    public void shouldCollectIfCollectionMethodEitherAndRequestProvided() throws NodeProcessException {
        X509Certificate certificate = generateCASignedCertificate("uid=user");
        X509Certificate[] x509Certificates = new X509Certificate[]{certificate};

        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(x509Certificates);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }
    @Test
    public void shouldNotCollectIfCollectionMethodHeaderAndHeaderNotProvided()
            throws NodeProcessException {
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of("testHeaderName"));

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    public void shouldCollectIfCollectionMethodHeaderAndHeaderProvided()
            throws NodeProcessException, CertificateEncodingException {
        given(config.trustedRemoteHosts()).willReturn(Set.of("any"));
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.HEADER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of("testHeaderName"));

        X509Certificate certificate = generateCASignedCertificate("uid=user");

        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                "testHeaderName", new String(Base64.encodeBase64(certificate.getEncoded())
        ));
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    public void shouldCollectIfCollectionMethodEitherAndHeaderProvided()
            throws NodeProcessException, CertificateEncodingException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.EITHER);
        given(config.clientCertificateHttpHeaderName()).willReturn(Optional.of("testHeaderName"));

        X509Certificate certificate = generateCASignedCertificate("uid=user");

        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                "testHeaderName", new String(Base64.encodeBase64(certificate.getEncoded())
                ));
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    public void shouldCollectIfCollectionMethodRequestAndRequestProvided() throws NodeProcessException {
        X509Certificate certificate = generateCASignedCertificate("uid=user");
        X509Certificate[] x509Certificates = new X509Certificate[]{certificate};

        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.REQUEST);
        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(x509Certificates);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(COLLECTED.name());
    }

    @Test
    public void shouldNotCollectIfCollectionMethodRequestAndRequestNotProvided()
            throws NodeProcessException {
        given(config.certificateCollectionMethod())
                .willReturn(CertificateCollectorNode.CertificateCollectionMethod.REQUEST);
        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(null);

        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        TreeContext treeContext = getContext(json(object()), headers);

        Action action = certificateCollectorNode.process(treeContext);

        assertThat(action.outcome).isEqualTo(NOT_COLLECTED.name());
    }

    @Test
    public void shouldThrowExceptionIfNoClientCertificateHttpHeaderNameInConfigAndHeaderTypeSet() {
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
    public void shouldThrowExceptionIfNoClientCertificateHttpHeaderNameInConfigAndEitherTypeSetWithNoCertInRequest() {
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