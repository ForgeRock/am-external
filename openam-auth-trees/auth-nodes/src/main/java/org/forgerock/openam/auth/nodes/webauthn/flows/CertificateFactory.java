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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for creating certificates from cert data.
 */
public final class CertificateFactory {

    private static final Logger logger = LoggerFactory.getLogger("amAuth");

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
            logger.debug("failed to convert certificate data into a certificate object");
            return null;
        }
    }
}
