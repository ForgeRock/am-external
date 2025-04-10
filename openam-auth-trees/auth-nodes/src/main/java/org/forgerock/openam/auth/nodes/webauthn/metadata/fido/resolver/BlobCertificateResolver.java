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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver;

import static java.text.MessageFormat.format;

import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Responsible for locating the certificates that we are to use for processing the BLOB.
 */
public class BlobCertificateResolver {
    private final ResourceResolver resourceResolver;

    /**
     * Constructor.
     */
    public BlobCertificateResolver() {
        this(new ResourceResolver());
    }

    @VisibleForTesting
    BlobCertificateResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Resolve certificates from a URL.
     *
     * @param url the URL that locates the Certificates
     * @return a {@code List} of Certificates
     * @throws CertificateResolutionException on error
     */
    public List<Certificate> resolveCertificate(URL url) throws CertificateResolutionException {
        try {
            Collection<? extends Certificate> certificates =
                    CertificateUtils.getX509Factory().generateCertificates(resourceResolver.resolve(url));
            return new ArrayList<>(certificates);
        } catch (CertificateException | IOException e) {
            String error = format("Failed to resolve certificate from URL {0}", url.toString());
            throw new CertificateResolutionException(error, e);
        }
    }
}
