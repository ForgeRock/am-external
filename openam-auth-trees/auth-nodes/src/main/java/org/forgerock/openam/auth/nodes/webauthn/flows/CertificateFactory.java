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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for creating certificates from cert data.
 */
public final class CertificateFactory {

    private static final Logger logger = LoggerFactory.getLogger(CertificateFactory.class);

    private CertificateFactory() { }

    /**
     * Creates a certificate from the provided cert data. If there's an error, the method returns null.
     *
     * @param certData the certificate data as bytes.
     * @return A certificate or null.
     */
    public static X509Certificate createCert(byte[] certData) {
        try {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certData));
        } catch (Exception e) {
            logger.warn("failed to convert certificate data into a certificate object", e);
            return null;
        }
    }

    /**
     * Creates certificates from the provided cert data. If there's an error, the method returns null.
     *
     * @param certData the certificate data as bytes where each certificate has been concatenated together into a single
     *                 byte array.
     * @return A certificate or null.
     */
    @SuppressWarnings("unchecked")
    public static List<X509Certificate> createCerts(byte[] certData) {
        try {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X509");
            return (List<X509Certificate>) cf.generateCertificates(new ByteArrayInputStream(certData));
        } catch (Exception e) {
            logger.warn("failed to convert certificate data into a certificate objects", e);
            return null;
        }
    }
}
