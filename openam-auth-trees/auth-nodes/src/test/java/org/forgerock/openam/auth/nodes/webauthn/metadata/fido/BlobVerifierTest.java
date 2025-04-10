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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils.copyClassPathDataToTempFile;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.openam.auth.nodes.webauthn.metadata.FileUtils;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver.BlobCertificateResolver;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver.CertificateResolutionException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link BlobVerifierTest} aims to test the verifier and its processing functions.
 */
public class BlobVerifierTest {

    private static final URL INVALID_URL = ResourceResolver.getUrl("http://test.url").orElse(null);

    private static String defaultBlobEndpoint;
    private static TrustAnchorValidator defaultValidator;

    private BlobCertificateResolver mockResolver;
    private BlobVerifier blobVerifier;

    @BeforeAll
    static void setupAll() {
        File blobFile = copyClassPathDataToTempFile("FidoMetadataDownloader/fidoconformance/fido-mds3-blob.jwt");
        defaultBlobEndpoint = blobFile.getAbsolutePath();

        defaultValidator = getTrustAnchorValidator(
                FileUtils.getFromClasspath("FidoMetadataDownloader/fidoconformance/fido-mds3-root.crt"));
    }

    @BeforeEach
    void setup() {
        mockResolver = mock(BlobCertificateResolver.class);
        blobVerifier = new BlobVerifier(mockResolver);
    }

    @Test
    void testBlobVerifierEmptyEndpoint() {
        assertThatThrownBy(() -> blobVerifier.verify(mock(SignedJwt.class), "", defaultValidator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid Context state; MDS Blob Endpoint cannot be empty");

    }

    @Test
    void testBlobVerifierEmptyTrustAnchor() {
        assertThatThrownBy(() -> blobVerifier.verify(mock(SignedJwt.class), defaultBlobEndpoint,
                new TrustAnchorValidator(mock(CertificateFactory.class), CertificateUtils.getCertPathValidator(),
                        Collections.emptySet(), false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid Context state; trust anchors cannot be empty");

    }

    private SignedJwt readNonSignedJwtx5c() {
        String string = FileUtils.readStream(FileUtils.getFromClasspath("not-signed.jwt"));
        return new JwtReconstruction().reconstructJwt(string, SignedJwt.class);
    }

    @Test
    void testBlobVerifierNonSignedJwtValidx5c() throws MetadataException {
        SignedJwt jwt = readNonSignedJwtx5c();
        Set<CertPath> verify = blobVerifier.getCertPath(jwt, defaultBlobEndpoint, defaultValidator);
        assertThat(verify).hasSize(1);
        assertThat(verify.iterator().next().getCertificates()).hasSize(1);
    }

    @Test
    void testBlobVerifierNonSignedJwtNox5C() {
        SigningHandler signingHandler = new NOPSigningHandler();
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(signingHandler);
        SignedJwt jwt = jwtBuilder.headers().x5c(Collections.emptyList()).done().asJwt();

        assertThatThrownBy(() -> blobVerifier.getCertPath(jwt, defaultBlobEndpoint, defaultValidator))
                .isInstanceOf(MetadataException.class)
                .hasMessage("No certificates were found in the JWT");
    }

    @Test
    void testBlobVerifierNonSignedJwtx5CInvalid() {
        // Create a JWT which is not signed
        SigningHandler signingHandler = new NOPSigningHandler();
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(signingHandler);

        // Then insert an unrelated certificate in the x5c field.
        String certificateString = FileUtils.readStream(
                FileUtils.getFromClasspath("not-a-certificate"));
        // Assemble a Jwt with the certificate in the x5c field.
        SignedJwt jwt = jwtBuilder.headers().x5c(Collections.singletonList(certificateString)).done().asJwt();
        assertThatThrownBy(() -> {
            blobVerifier.getCertPath(jwt, defaultBlobEndpoint, defaultValidator);
        }).isInstanceOf(MetadataException.class);
    }

    /*
        The x5u tests are different from the x5c tests in that they need to look for a URL in the JWT
        and then download any and all certificates found at that location and present these are the
        certificates used to sign the JWT.

        There is an additional requirement from the specification which indicates that the URL that
        is present in the x5u must also have the same origin as the URL used to download the JWT.
        Therefore we need to include this concept in these tests.
     */
    private SignedJwt readNonSignedJwtx5u(String certificateEndpoint) throws Exception {
        URL certUrl = new URL(certificateEndpoint);

        // Mock Certificate Resolver
        Certificate certificate = CertificateUtils.readCertificate(
                FileUtils.getFromClasspath("FidoMetadataDownloader/forgerock/forgerock-signing.pem"));
        given(mockResolver.resolveCertificate(certUrl)).willReturn(Collections.singletonList(certificate));

        /*
            The official FMS MDS JWT doesn't use x5u so for this set of test cases we will
            mock a SignedJwt which will return the required URL for test purposes. No signature
            is required as the method under test does not exercise that function.
         */
        return mockSignedJwtWithX5u(certUrl);
    }

    @Test
    void testBlobVerifierNonSignedJwtValidx5u() throws Exception {
        String blobEndpoint = "https://mds.fidoalliance.org";
        String certEndpoint = "https://mds.fidoalliance.org";

        SignedJwt jwt = readNonSignedJwtx5u(certEndpoint);
        Set<CertPath> verify = blobVerifier.getCertPath(jwt, blobEndpoint, defaultValidator);
        assertThat(verify).hasSize(1);
        assertThat(verify.iterator().next().getCertificates()).hasSize(1);
    }

    @Test
    void testBlobVerifierNonSignedJwtValidx5uDifferentOrigin() throws Exception {
        // Swapping 'mds2' for 'mds'
        String fmsBlobEndpoint = "https://mds.fidoalliance.org";
        String certEndpoint = "https://mds.notfidoalliance.org";
        SignedJwt jwt =  readNonSignedJwtx5u(certEndpoint);

        assertThatThrownBy(() -> blobVerifier.getCertPath(jwt, fmsBlobEndpoint, defaultValidator))
                .isInstanceOf(MetadataException.class)
                .hasMessage("JWT endpoint and JWT x5u URL did not share same origin");
    }

    @Test
    void testBlobVerifierNonSignedJwtValidx5uUsingUrL() throws Exception {
        String endpointUrl = "https://mds3.fidoalliance.org";
        SignedJwt jwt = readNonSignedJwtx5u(endpointUrl);
        jwt.getHeader().setX509Url(new URL(endpointUrl));
        blobVerifier.getCertPath(jwt, endpointUrl, defaultValidator);
    }

    @Test
    void testBlobVerifierNonSignedJwtValidx5uSignedUsingInvalidUrL() throws Exception {
        SignedJwt jwt = mockSignedJwtWithX5u(INVALID_URL);

        given(mockResolver.resolveCertificate(INVALID_URL))
                .willThrow(new CertificateResolutionException());
        assertThatThrownBy(() -> blobVerifier.getCertPath(jwt, defaultBlobEndpoint, defaultValidator))
                .isInstanceOf(MetadataException.class)
                .hasMessage("Failed to resolve x5u");
    }

    @Test
    void testBlobVerifierNonSignedJwtTrustAnchorUsed() throws Exception {
        SignedJwt jwt = mockSignedJwt();
        Set<CertPath> result = blobVerifier.getCertPath(jwt, defaultBlobEndpoint, defaultValidator);

        assertThat(result).hasSize(1);
        CertPath certPath = result.iterator().next();
        assertThat(certPath.getCertificates()).hasSize(1);

        /*
        Verify that the certificate in the Trust Anchor is the same as the
        certificate in the returned CertPath
         */
        assertThat(certPath.getCertificates().get(0)).isEqualTo(defaultValidator.getCertificates().iterator().next());
    }

    private SignedJwt readFidoMdsJwt() {
        String string = FileUtils.readStream(
                FileUtils.getFromClasspath("forgerock-signed.jwt"));
        return new JwtReconstruction().reconstructJwt(string, SignedJwt.class);
    }

    @Test
    void testBlobVerifierFidoMdsJwt() throws Exception {
        SignedJwt jwt = readFidoMdsJwt();
        CertPath certPath = CertificateUtils.getCertPathFromJwtX5c(jwt);
        assertThat(blobVerifier.verifySignature(jwt, certPath)).isTrue();
    }

    @Test
    void testBlobVerifierUnrelatedCerts() throws Exception {
        SignedJwt jwt = readFidoMdsJwt();
        Certificate certificate = CertificateUtils.readCertificate(
                FileUtils.getFromClasspath("FidoMetadataDownloader/rockforge/rockforge-signing.pem"));
        CertPath certPath = CertificateUtils.getX509Factory().generateCertPath(Collections.singletonList(certificate));
        assertThat(blobVerifier.verifySignature(jwt, certPath)).isFalse();
    }

    @Test
    void testBlobVerifierNoCerts() throws Exception {
        SignedJwt jwt = readFidoMdsJwt();
        CertPath certPath = CertificateUtils.getX509Factory().generateCertPath(Collections.emptyList());
        assertThatThrownBy(() -> {
            blobVerifier.verifySignature(jwt, certPath);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("CertPath cannot be empty for this function");
    }

    private static SignedJwt mockSignedJwt() {
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(new NOPSigningHandler());
        return jwtBuilder.asJwt();
    }

    private static SignedJwt mockSignedJwtWithX5u(URL url) {
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(new NOPSigningHandler());
        return jwtBuilder.headers().x5u(url).done().asJwt();
    }

    private static TrustAnchorValidator getTrustAnchorValidator(InputStream stream) {
        try {
            X509Certificate rootCert = (X509Certificate) CertificateUtils.getX509Factory().generateCertificate(stream);
            return new TrustAnchorValidator(mock(CertificateFactory.class), CertificateUtils.getCertPathValidator(),
                    Set.of(new TrustAnchor(rootCert, null)), false);
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }
}
