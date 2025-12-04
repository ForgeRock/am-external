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
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.copyClassPathDataToTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.InputStream;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobReader;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

public class FidoMetadataV3ServiceTest {
    public static final String FIDO_BLOB_JWT = "FidoMetadataDownloader/fidoconformance/fido-mds3-blob.jwt";
    public static final String INTERMEDIATE_CHAIN_CERTIFICATE =
            "WebAuthnRegistrationNode/certificatechain/intermediate.crt";
    public static final String ROOT_CHAIN_CERTIFICATE = "WebAuthnRegistrationNode/certificatechain/root.crt";
    public static final Aaguid FIDO_CERTIFIED_AAGUID =
            new Aaguid(UUID.fromString("4d41190c-7beb-4a84-8018-adf265a6352d"));
    public static final Aaguid REVOKED_AAGUID =
            new Aaguid(UUID.fromString("ba86dc56-635f-4141-aef6-00227b1b9af6"));

    @Mock
    private TrustAnchorValidator.Factory factory;

    private CertificateFactory realCertFactory;
    private CertPathValidator realCertPathValidator;

    @BeforeEach
    void setup() throws Exception {
        openMocks(this);
        realCertFactory = CertificateFactory.getInstance("X.509");
        realCertPathValidator = CertPathValidator.getInstance("PKIX");
    }

    @Test
    void testCertificateIsRoot() throws Exception {
        File blob = copyClassPathDataToTempFile(FIDO_BLOB_JWT);

        List<MetadataEntry> metadataEntries = loadTestMetadataService(blob);
        FidoMetadataV3Service service = new FidoMetadataV3Service(realCertFactory, factory, metadataEntries);

        // Set the validator to analyse the root certificate, the devices will be passed the root certificate
        X509Certificate rootCert = getCert(ROOT_CHAIN_CERTIFICATE);
        List<X509Certificate> rootCerts = Arrays.asList(rootCert);

        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(new TrustAnchor(rootCert, null)), false);
        given(factory.create(any(), eq(false))).willReturn(trustAnchorvalidator);

        AuthenticatorDetails result1 = service.determineAuthenticatorStatus(FIDO_CERTIFIED_AAGUID, rootCerts);
        assertEquals(AuthenticatorStatus.FIDO_CERTIFIED_L1, result1.getMaxCertificationStatus());

        AuthenticatorDetails result2 = service.determineAuthenticatorStatus(REVOKED_AAGUID, rootCerts);
        assertEquals(AuthenticatorStatus.REVOKED, result2.getMaxCertificationStatus());
    }

    @Test
    void testCertificateIsIntermediate() throws Exception {
        File blob = copyClassPathDataToTempFile(FIDO_BLOB_JWT);
        List<MetadataEntry> metadataEntries = loadTestMetadataService(blob);
        FidoMetadataV3Service service = new FidoMetadataV3Service(realCertFactory, factory, metadataEntries);

        // Set the validator to analyse the root certificate, the devices will be passed an intermediate certificate
        X509Certificate rootCert = getCert(ROOT_CHAIN_CERTIFICATE);
        X509Certificate intermediateCert = getCert(INTERMEDIATE_CHAIN_CERTIFICATE);
        List<X509Certificate> intermediateCerts = Arrays.asList(intermediateCert);

        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(new TrustAnchor(rootCert, null)), false);
        given(factory.create(any(), eq(false))).willReturn(trustAnchorvalidator);

        AuthenticatorDetails result1 = service.determineAuthenticatorStatus(FIDO_CERTIFIED_AAGUID, intermediateCerts);
        assertEquals(AuthenticatorStatus.FIDO_CERTIFIED_L1, result1.getMaxCertificationStatus());

        AuthenticatorDetails result2 = service.determineAuthenticatorStatus(REVOKED_AAGUID, intermediateCerts);
        assertEquals(AuthenticatorStatus.REVOKED, result2.getMaxCertificationStatus());
    }

    public static Object[] failedTestDetails() {

        // No certificates
        List<MetadataEntry> metadataEntries1 = new ArrayList<>();
        metadataEntries1.add(new MetadataEntry(FIDO_CERTIFIED_AAGUID, Collections.emptyList(),
                new AuthenticatorDetails(AuthenticatorStatus.FIDO_CERTIFIED_L1, new HashSet<>(), new HashSet<>())));

        // No AAGUID
        List<MetadataEntry> metadataEntries2 = new ArrayList<>();

        // Multiple entries for the same AAGUID
        List<MetadataEntry> metadataEntries3 = new ArrayList<>();
        metadataEntries3.add(new MetadataEntry(FIDO_CERTIFIED_AAGUID, Collections.emptyList(),
                new AuthenticatorDetails(AuthenticatorStatus.FIDO_CERTIFIED_L1, new HashSet<>(), new HashSet<>())));
        metadataEntries3.add(new MetadataEntry(FIDO_CERTIFIED_AAGUID, Collections.emptyList(),
                new AuthenticatorDetails(AuthenticatorStatus.FIDO_CERTIFIED_L3, new HashSet<>(), new HashSet<>())));

        return new Object[][]{
                {"No attestation root certificates found associated with the AAGUID", metadataEntries1},
                {"No metadata entries found for the AAGUID", metadataEntries2},
                {"Multiple metadata entries found for the same AAGUID", metadataEntries3}
            };
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("failedTestDetails")
    public void testCertificateIsMissingForAAGUID(String error, List<MetadataEntry> metadataEntries) throws Exception {
        FidoMetadataV3Service service = new FidoMetadataV3Service(realCertFactory, factory, metadataEntries);

        // Set the validator to analyse the root certificate, the devices will be passed an intermediate certificate
        X509Certificate rootCert = getCert(ROOT_CHAIN_CERTIFICATE);
        X509Certificate intermediateCert = getCert(INTERMEDIATE_CHAIN_CERTIFICATE);
        List<X509Certificate> intermediateCerts = Arrays.asList(intermediateCert);

        //given
        TrustAnchorValidator trustAnchorvalidator = new TrustAnchorValidator(
                realCertFactory,
                realCertPathValidator,
                Collections.singleton(new TrustAnchor(rootCert, null)), false);
        given(factory.create(any(), eq(false))).willReturn(trustAnchorvalidator);

        assertThatThrownBy(() -> service.determineAuthenticatorStatus(FIDO_CERTIFIED_AAGUID, intermediateCerts))
                .isInstanceOf(MetadataException.class)
                .hasMessageContaining(error);
    }

    private X509Certificate getCert(String certPath) throws CertificateException {
        InputStream in = FileUtils.getFromClasspath(certPath);
        return (X509Certificate) realCertFactory.generateCertificate(in);
    }

    private List<MetadataEntry> loadTestMetadataService(File metadataFile) throws MetadataException {
        BlobVerifier blobVerifier = mock(BlobVerifier.class);
        doReturn(true).when(blobVerifier).verify(any(), any(), any());
        MetadataBlobPayloadDownloader metadataDownloader = new MetadataBlobPayloadDownloader(blobVerifier,
                new ResourceResolver(), new BlobReader(), new JwtReconstruction());
        FidoMetadataV3Processor fidoMetadataV3Processor = new FidoMetadataV3Processor(metadataDownloader,
                metadataFile.getAbsolutePath(), mock(TrustAnchorValidator.class));
        return fidoMetadataV3Processor.process();
    }
}
