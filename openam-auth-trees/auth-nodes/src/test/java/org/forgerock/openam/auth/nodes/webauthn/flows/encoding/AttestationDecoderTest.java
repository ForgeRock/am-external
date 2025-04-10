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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidSafetyNetVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;

@ExtendWith(MockitoExtension.class)
public class AttestationDecoderTest {

    private CertificateFactory realCertFactory;
    private AttestationDecoder decoder;

    public static Stream<Arguments> formatVerifier() {
        return Stream.of(
                arguments("none", NoneVerifier.class),
                arguments("fido-u2f", FidoU2fVerifier.class),
                arguments("packed", PackedVerifier.class),
                arguments("tpm", TpmVerifier.class),
                arguments("android-safetynet", AndroidSafetyNetVerifier.class)
        );
    }

    public static Stream<Arguments> unsupportedFormats() {
        return Stream.of(
                arguments("android-key")
        );
    }

    @BeforeEach
    void theSetUp() {
        decoder = new AttestationDecoder(new AuthDataDecoder());
        try {
            realCertFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            // cannot happen. X.509 has to be supported
        }
    }

    @Test
    void testSelfSignedRootCaCertificate() throws Exception {
        //given
        List<X509Certificate> certificates = getCertificates(realCertFactory,
                "attestation.crt", "intermediate.crt", "root.crt");
        byte[] input = new AttestationBuilder()
                .fmt("tpm")
                .x5cCertificateChain(certificates)
                .build();

        //when
        assertThatThrownBy(() -> decoder.decode(input))
                //then
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("root certificate must not be self-signed");
    }

    @Test
    void testDoesNotIgnoreIntermediateCaCertificateIfMissingRoot() throws Exception {
        //given
        List<X509Certificate> certificates = getCertificates(realCertFactory,
                "attestation.crt", "intermediate.crt"); // no root certificate
        byte[] input = new AttestationBuilder()
                .fmt("tpm")
                .x5cCertificateChain(certificates)
                .build();

        //when
        AttestationObject result = decoder.decode(input);

        //then
        assertThat(result.attestationStatement.getAttestnCerts()).isEqualTo(certificates);
    }

    @Test
    void testCertificateChainIsEmptyIfInvalidCertificateProvided() throws Exception {
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
        AttestationObject result = decoder.decode(input);

        //then
        assertThat(result.attestationStatement.getAttestnCerts()).isEmpty();
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
