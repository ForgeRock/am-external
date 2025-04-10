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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The files referenced by this test are:
 * <p>
 * Set A:
 * rootCA.crt - a root CA certificate
 * forgerock.test.crt - a cert that chains off rootCa.crt. It has an expiry time of 1 million days.
 * invalidRootCA.crt - a (different) root certificate
 * <p>
 * Set B:
 * root.crt - a root CA certificate
 * intermediate.crt - an intermediate CA certificate that chains off root.crt
 * attestation.crt - a leaf attestation certificate that chains off intermediate.crt
 */
@ExtendWith(MockitoExtension.class)
public class TrustAnchorValidatorTest {

    @Mock
    CertificateFactory mockCertFactory;
    @Mock
    CertPathValidator mockCertPathValidator;
    @Mock
    X509Certificate mockCertificate;

    CertificateFactory realCertFactory;
    CertPathValidator realCertPathValidator;

    @BeforeEach
    void theSetup() {
        try {
            realCertFactory = CertificateFactory.getInstance("X.509");
            realCertPathValidator = CertPathValidator.getInstance("PKIX");
        } catch (CertificateException | NoSuchAlgorithmException e) {
            //cannot happen. X.509 / PKIX has to be supported
        }
    }

    @Test
    void attestationIsTrueWhenTrustedAnchorsFound() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getCATrustAnchor(realCertFactory)), false);
        List<X509Certificate> certs = Collections.singletonList(getCert(realCertFactory));

        //when
        boolean result = trustAnchorvalidator.isTrusted(realCertFactory.generateCertPath(certs));

        //then
        assertThat(result).isTrue();
    }


    @Test
    void attestationCAWhenTrustedAnchorsFound() throws Exception {
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
    void attestationCAWhenTrustedAnchorFoundAndIntermediateCAProvided() throws Exception {
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
    void attestationIsInvalidIfPathIsBackwards() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getIntermediateCA(realCertFactory), getAttestationCert(realCertFactory));

        //when
        try {
            trustAnchorvalidator.isTrusted(realCertFactory.generateCertPath(certs));
            fail();
        } catch (CertPathValidatorException e) {
            assertThat(e.getMessage()).contains("Path does not chain with any of the trust anchors");
        }
    }

    @Test
    void attestationIsInvalidIfStageIsDuplicated() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getAttestationCert(realCertFactory), getIntermediateCA(realCertFactory),
                getIntermediateCA(realCertFactory));

        //when
        try {
            trustAnchorvalidator.isTrusted(realCertFactory.generateCertPath(certs));
            fail();
        } catch (CertPathValidatorException e) {
            assertThat(e.getMessage()).contains("subject/issuer name chaining check failed");
        }
    }

    @Test
    void attestationIsInvalidIfRootIsInPath() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getAttestationCert(realCertFactory), getIntermediateCA(realCertFactory),
                getRootCAAsCert(realCertFactory));

        //when
        try {
            trustAnchorvalidator.isTrusted(realCertFactory.generateCertPath(certs));
            fail();
        } catch (CertPathValidatorException e) {
            assertThat(e.getMessage()).contains("validity check failed");
        }
    }

    @Test
    void attestationCAWhenTrustedAnchorFoundButChainMissingIntermediateCACert() throws Exception {
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
    void attestationBasicWhenNoTrustedAnchorsFound() throws Exception {
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
    void attestationBasicWhenNullTrustAnchors() {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(mockCertFactory, mockCertPathValidator,
                null, false);

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(Collections.singletonList(mockCertificate));

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    @Test
    void attestationBasicWhenEmptyTrustAnchors() {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(mockCertFactory, mockCertPathValidator,
                Collections.emptySet(), false);

        //when
        AttestationType result = trustAnchorvalidator.getAttestationType(Collections.singletonList(mockCertificate));

        //then
        assertThat(result).isEqualTo(AttestationType.BASIC);
    }

    @Test
    void attestationCertPathContainsTrustAnchor() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getAttestationCert(realCertFactory), getIntermediateCA(realCertFactory),
                getRootCAAsCert(realCertFactory));

        //when
        boolean result = trustAnchorvalidator.containsTrustAnchor(certs);

        //then
        assertThat(result).isTrue();
    }

    @Test
    void attestationCertPathDoesContainTrustAnchor() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getRootCAAsCert(realCertFactory));

        //when
        boolean result = trustAnchorvalidator.isRootCertificate(certs);

        //then
        assertThat(result).isTrue();
    }

    @Test
    void attestationCertPathDoesNotContainTrustAnchor() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getIntermediateCA(realCertFactory));

        //when
        boolean result = trustAnchorvalidator.isRootCertificate(certs);

        //then
        assertThat(result).isFalse();
    }

    @Test
    void attestationCertPathDoesContainTrustAnchorButIsAChain() throws Exception {
        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(getRootCA(realCertFactory)), false);
        List<X509Certificate> certs = List.of(getIntermediateCA(realCertFactory), getRootCAAsCert(realCertFactory));

        //when
        boolean result = trustAnchorvalidator.isRootCertificate(certs);

        //then
        assertThat(result).isFalse();
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

    private X509Certificate getRootCAAsCert(CertificateFactory cf) throws CertificateException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("WebAuthnRegistrationNode/certificatechain/root.crt");
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
