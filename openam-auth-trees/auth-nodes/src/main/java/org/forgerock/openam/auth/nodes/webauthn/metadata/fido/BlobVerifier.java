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

import static java.text.MessageFormat.format;

import java.io.File;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.jose.jws.JwsHeaderKey;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver.BlobCertificateResolver;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver.CertificateResolutionException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.keys.VerificationKey;

/**
 * {@link BlobVerifier} provides a set of processing functions that will allow the caller
 * to examine the provided metadata file and verify the JWS signature.
 * <p>
 * This class breaks up this processing into three distinct stages:
 * <ol>
 * <li>Extract the {@link CertPath} from the {@link SignedJwt}</li>
 * <li>Verify the {@link CertPath} chain to ensure no {@link Certificate} in the chain
 * has been revoked.</li>
 * <li>Verify the signature of the {@link SignedJwt} using the {@link CertPath}</li>
 * </ol>
 * </p>
 */
public class BlobVerifier {

    private final BlobCertificateResolver blobResolver;

    /**
     * Create a new instance of the {@link BlobVerifier}.
     *
     * @param blobResolver Required for resolution of the {@link BlobCertificateResolver}.
     */
    @Inject
    public BlobVerifier(BlobCertificateResolver blobResolver) {
        this.blobResolver = blobResolver;
    }

    /**
     * Verify the validity of the provided jwt.
     *
     * @param jwt The blob jwt to verify.
     * @param mdsBlobEndpoint non-null, non-empty URL that the jwt was retrieved from.
     * @param trustAnchorValidator non-null, non-empty {@link TrustAnchorValidator} to use.
     * @return {@code true} if the jwt was successfully verified, otherwise {@code false}
     * @throws MetadataException If there was any error whilst attempting to verify the jwt.
     */
    public boolean verify(SignedJwt jwt, String mdsBlobEndpoint, TrustAnchorValidator trustAnchorValidator)
            throws MetadataException {
        validateContextParameter(mdsBlobEndpoint, "MDS Blob Endpoint");
        if (trustAnchorValidator.getCertificates().isEmpty()) {
            throw new IllegalStateException(format("Invalid Context state; trust anchors cannot be empty"));
        }
        //@Checkstyle:off LineLength
        // Identify the Certificates used to sign the JWT - Sections 3.2.4, 3.2.5, 3.5.6 Metadata BLOB processing rules
        // https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#metadata-blob-object-processing-rules
        //@Checkstyle:on LineLength
        Set<CertPath> certPaths = getCertPath(jwt, mdsBlobEndpoint, trustAnchorValidator);
        for (CertPath certPath : certPaths) {
            try {
                if (trustAnchorValidator.isTrusted(certPath) && verifySignature(jwt, certPath)) {
                    return true;
                }
            } catch (CertPathValidatorException | InvalidAlgorithmParameterException e) {
                // ignore
            }
        }

        return false;
    }

    /**
     * Inspect the provided {@link SignedJwt} and extract any {@link Certificate} that are present in
     * the form of a {@link CertPath}.
     * <p>
     * The FIDO Alliance meta data server (MDS) uses a standard JWT as a table of contents. This function
     * will examine the JWT and as per the specification, extract the Certificates that have been used to
     * sign the JWT.
     * </p>
     * <p>
     * <i>The FIDO Server must verify that the URL specified by the x5u attribute has the same web-origin as the URL
     * used to download the metadata BLOB from. The FIDO Server should ignore the file if the web-origin differs
     * (in order to prevent loading objects from arbitrary sites).</i>
     * </p>
     * <p>
     * The generated {@link CertPath} will only contain the certificates that are referred to in the JWT.
     * The trust anchor (Root Certificate) will not be included in the chain.
     * </p>
     *
     * @param jwt non-null {@link SignedJwt} which represents the MDS BLOB.
     *
     * @return A non-null {@link CertPath} which represents all certificates that were acquired from
     * the {@link JwsHeaderKey#X5U} or {@link JwsHeaderKey#X5C} header field of the JWT.
     *
     * @throws MetadataException If there was any error identifying the certificates in the
     * JWT including resolving certificates referred to by URL.
     */
    Set<CertPath> getCertPath(SignedJwt jwt, String mdsBlobEndpoint, TrustAnchorValidator trustAnchorValidator)
            throws MetadataException {

        // (3.2.4 Metadata BLOB object processing rules)
        if (isX5uMode(jwt)) {
            URL x509Url = jwt.getHeader().getX509Url();

            /*
            Perform a same-origin check, only if the JWT was downloaded from a URL
            and contains the x5u header.
             */
            if (isBlobEndpointURL(mdsBlobEndpoint)) {
                URL blobUrl = ResourceResolver.getUrl(mdsBlobEndpoint).orElseThrow(() ->
                        new MetadataException("Failed to resolve URL"));
                // (3.2.4.1 Metadata BLOB object processing rules)
                if (!sameOrigin(x509Url, blobUrl)) {
                    throw new MetadataException("JWT endpoint and JWT x5u URL did not share same origin");
                }
            }

            try {
                List<Certificate> certificates = blobResolver.resolveCertificate(x509Url);
                return Set.of(CertificateUtils.getX509Factory().generateCertPath(certificates));
            } catch (CertificateResolutionException | CertificateException e) {
                throw new MetadataException("Failed to resolve x5u", e);
            }
        }

        // (3.2.5 Metadata BLOB object processing rules)
        if (isX5cMode(jwt)) {
            try {
                CertPath certPath = CertificateUtils.getCertPathFromJwtX5c(jwt);
                if (certPath.getCertificates().isEmpty()) {
                    throw new MetadataException("No certificates were found in the JWT");
                }
                return Set.of(certPath);
            } catch (CertificateException e) {
                throw new MetadataException("Failed to extract certificate", e);
            }
        }

        /*
        If the x5u attribute is missing, the chain should be retrieved from the x5c attribute.
        If that attribute is missing as well, Metadata BLOB signing trust anchor is considered
        the BLOB signing certificate chain.
         */
        // (3.2.5 Metadata BLOB object processing rules)
        Set<X509Certificate> certificates = trustAnchorValidator.getCertificates();
        Set<CertPath> certPaths = new HashSet<>();
        for (X509Certificate certificate : certificates) {
            try {
                certPaths.add(
                        CertificateUtils.getX509Factory().generateCertPath(Collections.singletonList(certificate)));
            } catch (CertificateException e) {
                throw new MetadataException("Failed to generate CertPath", e);
            }
        }
        return certPaths;
    }

    /**
     * Tests for x5c mode.
     *
     * @param jwt non-null
     * @return {@code true} if the {@link SignedJwt} has the x5c header
     */
    private boolean isX5cMode(SignedJwt jwt) {
        return jwt.getHeader().getX509CertificateChain() != null;
    }

    /**
     * Tests for x5u mode.
     *
     * @param jwt non-null
     * @return {@code true} if the {@link SignedJwt} has the x5u header
     */
    private boolean isX5uMode(SignedJwt jwt) {
        return jwt.getHeader().getX509Url() != null;
    }

    private boolean isBlobEndpointURL(String endpointLocation) {
        return !new File(endpointLocation).exists();
    }

    private boolean sameOrigin(URL first, URL second) {
        return first.getProtocol().equals(second.getProtocol())
                && first.getHost().equals(second.getHost())
                && first.getPort() == second.getPort();
    }

    /**
     * Perform a signature verification of the {@link SignedJwt} with the provided {@link CertPath}.
     * <p>
     * This processing function will locate the first certificate in the {@link CertPath} and use the
     * public key of that {@link Certificate} to perform a verification of the JWT signature.
     * </p>
     * <a href="https://tools.ietf.org/html/rfc7515#section-4.1.6">RFC7517 JSON Web Signature (JWS)</a>
     * <p>
     * <i>The certificate containing the public key corresponding to the key used to digitally sign the
     * JWS MUST be the first certificate.  This MAY be followed by additional certificates, with each
     * subsequent certificate being the one used to certify the previous one.  The recipient MUST validate
     * the certificate chain according to RFC 5280 [RFC5280] and consider the certificate or certificate
     * chain to be invalid if any validation failure occurs.</i>
     * </p>
     *
     * @param jwt non-null {@link SignedJwt}.
     * @param certPath non-null, non-empty {@link CertPath} to use.
     *
     * @return {@code true} if the {@link Jwt} was successfully verified, otherwise {@code false}
     * indicates that the signature was not valid.
     *
     * @throws MetadataException If there was any unexpected error whilst attempting to
     * verify the signature. This exception indicates that the JWT or the {@link CertPath} was
     * in some way invalid.
     */
    public boolean verifySignature(SignedJwt jwt, CertPath certPath)
            throws MetadataException {
        // (3.2.6 Metadata BLOB object processing rules)
        if (certPath.getCertificates().isEmpty()) {
            throw new IllegalStateException("CertPath cannot be empty for this function");
        }

        // The clock determines the point in time for validity checking
        Clock clock = Clock.systemDefaultZone();

        Certificate certificate = certPath.getCertificates().get(0);
        VerificationKey handler;
        try {
            handler = new SecretBuilder()
                    .certificate(certificate)
                    .clock(clock)
                    .build(Purpose.VERIFY);

            SigningManager signingManager = new SigningManager(new SecretsProvider(clock));
            SigningHandler signingHandler = signingManager.newVerificationHandler(handler);

            return jwt.verify(signingHandler);
        } catch (Throwable e) {
            throw new MetadataException("Failed to verify signature", e);
        }
    }

    private static void validateContextParameter(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(format("Invalid Context state; {0} cannot be empty", name));
        }
    }
}
