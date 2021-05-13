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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.x509;

import java.security.cert.X509Certificate;
import java.util.List;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;

/**
 * Utility methods for the X509 Certificate Nodes.
 */
final class CertificateUtils {

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
}
