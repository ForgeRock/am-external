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
 * Copyright 2020-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.x509;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;

/**
 * Utility methods for the X509 Certificate Nodes.
 */
public final class CertificateUtils {

    private static final String X_509 = "X.509";

    private CertificateUtils() {
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
                .collect(Collectors.toList());
        return CertificateFactory.getInstance(X_509).generateCertPath(certificates);
    }

    private static Function<String, Certificate> parseCertificate() {
        return value -> {
            try {
                return (X509Certificate) CertificateFactory.getInstance(X_509)
                        .generateCertificate(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
        };
    }

}
