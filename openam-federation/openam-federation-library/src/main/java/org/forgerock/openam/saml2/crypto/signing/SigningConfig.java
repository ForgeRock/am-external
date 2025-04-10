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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.crypto.signing;

import java.security.Key;
import java.security.cert.X509Certificate;

import org.forgerock.util.annotations.VisibleForTesting;

/**
 * This POJO contains the signing configuration to be used for signing SAML2 XML documents.
 *
 * @since AM 7.0.0
 */
public final class SigningConfig {

    private final Key signingKey;
    private final X509Certificate certificate;
    private final String signingAlgorithm;
    private final String digestMethod;

    /**
     * Creates a new {@link SigningConfig} instance. Please use {@link SigningConfigFactory} rather than calling this
     * constructor directly.
     *
     * @param signingKey The signing key.
     * @param certificate The certificate to include in the XML signature. May be null if the certificate should not be
     * included.
     * @param signingAlgorithm The signing algorithm.
     * @param digestMethod The digest method.
     */
    @VisibleForTesting
    public SigningConfig(Key signingKey, X509Certificate certificate, String signingAlgorithm,
            String digestMethod) {
        this.signingKey = signingKey;
        this.certificate = certificate;
        this.signingAlgorithm = signingAlgorithm;
        this.digestMethod = digestMethod;
    }

    /**
     * Retrieve the signing key to be used for signing the SAML2 document.
     *
     * @return The key to be used when creating the signature.
     */
    public Key getSigningKey() {
        return signingKey;
    }

    /**
     * The public certificate that should be included along with the XML signature under &lt;ds:KeyInfo&gt;.
     *
     * @return The certificate to include along with the signature. May be null, if the certificate should not be
     * included.
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * The URI of the signing algorithm to be used when creating the XML signature. For example:
     * {@literal http://www.w3.org/2001/04/xmldsig-more#rsa-sha512}
     *
     * @return The URI of the signing algorithm.
     */
    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    /**
     * The URI of the digest method to be used when creating the XML signature. For example:
     * {@literal http://www.w3.org/2001/04/xmlenc#sha512}
     *
     * @return The URI of the digest method.
     */
    public String getDigestMethod() {
        return digestMethod;
    }
}
