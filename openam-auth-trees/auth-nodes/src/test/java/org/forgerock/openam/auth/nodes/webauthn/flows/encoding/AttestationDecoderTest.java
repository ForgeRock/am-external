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
 * Copyright 2021-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidSafetyNetVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.forgerock.openam.oauth2.OAuth2ClientOriginSearcher;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;

public class AttestationDecoderTest {

    @Mock
    OAuth2ClientOriginSearcher mockClientOriginHelper;
    @Mock
    TrustAnchorValidator.Factory factory;
    @Mock
    TrustAnchorUtilities trustUtilities;

    private CertificateFactory realCertFactory;
    private AttestationDecoder decoder;

    @BeforeTest
    public void theSetUp() {
        initMocks(this);
        decoder = new AttestationDecoder(trustUtilities, factory, new AuthDataDecoder(),
                new FlowUtilities(mockClientOriginHelper), new NoneVerifier());
        try {
            realCertFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            // cannot happen. X.509 has to be supported
        }
    }

    @DataProvider(name = "formatVerifier")
    public Object[][] formatVerifier() {
        return new Object[][] {
                { "none", NoneVerifier.class },
                { "fido-u2f", FidoU2fVerifier.class },
                { "packed", PackedVerifier.class },
                { "tpm", TpmVerifier.class },
                { "android-safetynet", AndroidSafetyNetVerifier.class}
        };
    }

    @DataProvider(name = "unsupportedFormats")
    public Object[][] unsupportedFormats() {
        return new Object[][] {
                { "android-key" },
        };
    }

    @Test(dataProvider = "unsupportedFormats", expectedExceptions = DecodingException.class)
    public void testExceptionThrownForUnsupportedFormats(String name) throws Exception {
        //given
        byte[] input = new AttestationBuilder().fmt(name).build();

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        //expecting an exception
    }

    @Test(dataProvider = "formatVerifier")
    public void testCorrectVerifierReturnedForAttestationFormat(String name,
                                                                Class<AttestationVerifier> verifier) throws Exception {
        //given
        byte[] input = new AttestationBuilder().fmt(name).build();

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        assertThat(result.attestationVerifier).isOfAnyClassIn(verifier);
    }

    @Test
    public void testIgnoresRootCaCertificate() throws Exception {
        //given
        List<X509Certificate> certificates = getCertificates(realCertFactory,
                "attestation.crt", "intermediate.crt", "root.crt");
        byte[] input = new AttestationBuilder()
                .fmt("tpm")
                .x5cCertificateChain(certificates)
                .build();

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        assertThat(result.attestationStatement.getAttestnCerts())
                .containsExactly(certificates.get(0), certificates.get(1));
    }

    @Test
    public void testDoesNotIgnoreIntermediateCaCertificateIfMissingRoot() throws Exception {
        //given
        List<X509Certificate> certificates = getCertificates(realCertFactory,
                "attestation.crt", "intermediate.crt"); // no root certificate
        byte[] input = new AttestationBuilder()
                .fmt("tpm")
                .x5cCertificateChain(certificates)
                .build();

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        assertThat(result.attestationStatement.getAttestnCerts()).isEqualTo(certificates);
    }

    @Test
    public void testCertificateIsNullIfInvalidCertificateProvided() throws Exception {
        //given
        CborBuilder cborBuilder = new CborBuilder();
        cborBuilder.addMap()
                .put("fmt", "tpm")
                .putMap("attStmt")
                .putArray("x5c").add("not a certificate".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder cborEncoder = new CborEncoder(baos);
        cborEncoder.encode(cborBuilder.build());
        byte[] input = baos.toByteArray();

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        assertThat(result.attestationStatement.getAttestnCerts()).isNull();
    }

    private List<X509Certificate> getCertificates(CertificateFactory cf, String... certificateNames)
            throws CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        for (String certificateName : certificateNames) {
            InputStream in = this.getClass().getClassLoader()
                    .getResourceAsStream("WebAuthnRegistrationNode/certificatechain/" + certificateName);
            certificates.add((X509Certificate) cf.generateCertificate(in));
        }
        return certificates;
    }

    private class AttestationBuilder {

        private String fmt;
        private List<X509Certificate> certificateChain;

        public AttestationBuilder fmt(String fmt) {
            this.fmt = fmt;
            return this;
        }

        public AttestationBuilder x5cCertificateChain(List<X509Certificate> certificateChain) {
            this.certificateChain = certificateChain;
            return this;
        }

        public byte[] build() throws Exception {
            MapBuilder<CborBuilder> rootMap = new CborBuilder().addMap();
            rootMap.put("fmt", fmt);

            if (certificateChain != null) {
                ArrayBuilder x5cArray = rootMap
                        .putMap("attStmt")
                        .putArray("x5c");

                for (X509Certificate certificate : certificateChain) {
                    x5cArray.add(certificate.getEncoded());
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder cborEncoder = new CborEncoder(baos);
            cborEncoder.encode(rootMap.end().build());

            return baos.toByteArray();
        }
    }

}
