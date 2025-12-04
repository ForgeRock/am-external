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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;

/**
 * Utility methods for the X509 Certificate Nodes.
 */
public final class CertificateUtils {

    private static final String X_509 = "X.509";
    private static final String PKIX = "PKIX";

    private CertificateUtils() {
    }

    /**
     * Create an instance of the JDK standard certificate factory.
     *
     * @return a {@link CertificateFactory}
     */
    public static CertificateFactory getX509Factory() {
        try {
            return CertificateFactory.getInstance(X_509);
        } catch (CertificateException e) {
            //Not expected to be thrown, X.509 always supported on Java
            throw new IllegalStateException("Unexpected failure", e);
        }
    }

    /**
     * Generate a JDK default {@link CertPathValidator} using the {@code PKIX} algorithm.
     *
     * @return An initialised instance of the {@link CertPathValidator}
     */
    public static CertPathValidator getCertPathValidator() {
        try {
            return CertPathValidator.getInstance(PKIX);
        } catch (NoSuchAlgorithmException e) {
            //Not expected to be thrown, PKIX always supported on Java
            throw new IllegalStateException("Unexpected error", e);
        }
    }

    /**
     * Gets the first X509 Certificate from a list of certificates.
     *
     * @param certs The {@code List} of X509 Certificates.
     * @param logger Logger instance.
     * @return The first X509 Certificate from the provided list.
     * @throws NodeProcessException If the list is empty.
     */
    static X509Certificate getX509Certificate(List<X509Certificate> certs, Logger logger) throws NodeProcessException {
        X509Certificate theCert = !certs.isEmpty() ? certs.get(0) : null;

        if (theCert == null) {
            logger.debug("Certificate: no cert passed in.");
            throw new NodeProcessException("No certificate passed from Shared State. Check configuration of the "
                    + "certificate collector node");
        }
        return theCert;
    }

    /**
     * Read a certificate from the provided path.
     *
     * @param path stream to the certificate file
     * @return An {@link X509Certificate} read from the file system
     * @throws CertificateException if there was an error reading the {@link Certificate}
     */
    public static X509Certificate readCertificate(InputStream path) throws CertificateException {
        return (X509Certificate) getX509Factory().generateCertificate(path);
    }

    /**
     * Extract the certificates contained within the {@code x5c} field of a {@link JWK}.
     * <p>
     * Certificates are a binary format and as such the JWK specification encodes them as Base64 encoded
     * strings. These need to be extracted from the JWK and processed.
     * </p>
     * <p>
     * <b>Note:</b>For the specific JWK we are using for this test code, we know it is a Base64 encoded file
     * which requires the header and footer to be included.
     * </p>
     *
     * @param jwk Non null {@link JWK}.
     * @return Non null {@link CertPath}.
     * @throws CertificateException Throws when failed to extract the x5c field.
     */
    public static CertPath getCertPathFromJwkX5c(JWK jwk) throws CertificateException {
        List<Certificate> certificates = jwk.getX509Chain().stream()
                .map(rawCertificate -> "-----BEGIN CERTIFICATE-----\n" + rawCertificate + "\n-----END CERTIFICATE-----")
                .map(parseCertificate())
                .toList();
        return getX509Factory().generateCertPath(certificates);
    }

    /**
     * Extract the certificates contained within the {@code x5c} field of a {@link SignedJwt}.
     * <p>
     * Certificates are a binary format and as such the JWT specification encodes them as Base64 encoded
     * strings. These need to be extracted from the JWT and processed.
     * </p>
     *
     * @param jwt Non null {@link SignedJwt}.
     * @return Non null {@link CertPath}.
     * @throws CertificateException Throws when failed to extract the x5c field.
     */
    public static CertPath getCertPathFromJwtX5c(SignedJwt jwt) throws CertificateException {
        List<Certificate> certificates = jwt.getHeader().getX509CertificateChain().stream()
                .map(base64Encoded -> new String(Base64.getDecoder().decode(base64Encoded)))
                .map(rawCertificate -> "-----BEGIN CERTIFICATE-----\n" + rawCertificate + "\n-----END CERTIFICATE-----")
                .map(parseCertificate())
                .toList();
        return getX509Factory().generateCertPath(certificates);
    }

    private static Function<String, Certificate> parseCertificate() throws CertificateException {
        return rethrowFunction(value -> getX509Factory()
                .generateCertificate(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))));
    }
}
