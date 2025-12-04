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
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver;

/**
 * Responsible for modelling the failure to resolve a certificate.
 */
public class CertificateResolutionException extends Exception {
    private static final long serialVersionUID = -4566060171278770043L;

    /**
     * Creates an instance of this exception.
     *
     * @param reason the message associated with the exception
     * @param cause the root exception
     */
    public CertificateResolutionException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Default Constructor.
     */
    public CertificateResolutionException() {
        super();
    }
}
