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
 * Copyright 2020-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * The files referenced by this test are:
 *
 * Set A:
 * rootCA.crt - a root CA certificate
 * forgerock.test.crt - a cert that chains off rootCa.crt. It has an expiry time of 1 million days.
 * invalidRootCA.crt - a (different) root certificate
 *
 * Set B:
 * root.crt - a root CA certificate
 * intermediate.crt - an intermediate CA certificate that chains off root.crt
 * attestation.crt - a leaf attestation certificate that chains off intermediate.crt
 */
public class TrustAnchorValidatorTest {

    @Mock
    CertificateFactory mockCertFactory;
    @Mock
    CertPathValidator mockCertPathValidator;
    @Mock
    X509Certificate mockCertificate;

    CertificateFactory realCertFactory;
    CertPathValidator realCertPathValidator;

    @BeforeTest
    public void theSetup() {
        initMocks(this);

        try {
            realCertFactory = CertificateFactory.getInstance("X.509");
            realCertPathValidator = CertPathValidator.getInstance("PKIX");
        } catch (CertificateException | NoSuchAlgorithmException e) {
            //cannot happen. X.509 / PKIX has to be supported
        }
    }


    @Test
    public void attestationCAWhenTrustedAnchorsFound() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getCATrustAnchor(realCertFactory)), false);
        List<X509Certificate> certs = Collections.singletonList(getCert(realCertFactory));

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(certs);

        //then
        assertThat(result).isEqualTo(AttestationType.CA);
    }

    @Test
    public void attestationCAWhenTrustedAnchorFoundAndIntermediateCAProvided() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getAttestationCert(realCertFactory), getIntermediateCA(realCertFactory));

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(certs);

        //then
        assertThat(result).isEqualTo(AttestationType.CA);
    }

    @Test
    public void attestationCAWhenTrustedAnchorFoundButChainMissingIntermediateCACert() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getAttestationCert(realCertFactory));

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(certs);

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    @Test
    public void attestationBasicWhenNoTrustedAnchorsFound() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getIncorrectCATrustAnchor(realCertFactory)), false);
        List<X509Certificate> certs = Collections.singletonList(getCert(realCertFactory));

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(certs);

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    @Test
    public void attestationBasicWhenNullTrustAnchors() {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(mockCertFactory, mockCertPathValidator,
                null, false);

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(Collections.singletonList(mockCertificate));

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    @Test
    public void attestationBasicWhenEmptyTrustAnchors() {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(mockCertFactory, mockCertPathValidator,
                Collections.emptySet(), false);

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(Collections.singletonList(mockCertificate));

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    private X509Certificate getCert(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/forgerock.test.crt");
        return (X509Certificate) cf.generateCertificate(in);
    }

    private X509Certificate getAttestationCert(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/certificatechain/attestation.crt");
        return (X509Certificate) cf.generateCertificate(in);
    }

    private X509Certificate getIntermediateCA(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/certificatechain/intermediate.crt");
        return (X509Certificate) cf.generateCertificate(in);
    }

    private TrustAnchor getRootCA(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/certificatechain/root.crt");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        return new TrustAnchor(cert, null);
    }

    private TrustAnchor getCATrustAnchor(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("WebAuthnRegistrationNode/rootCA.crt");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        return new TrustAnchor(cert, null);
    }

    private TrustAnchor getIncorrectCATrustAnchor(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/invalidRootCA.crt");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        return new TrustAnchor(cert, null);
    }

}
