/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SigProvider.java,v 1.2 2008/06/25 05:48:04 qcheng Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.xmlsig;

import java.security.cert.X509Certificate;
import java.util.Set;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * <code>SigProvider</code> is an interface for signing and verifying XML documents.
 */
public interface SigProvider {

    /**
     * Sign the XML document node whose identifying attribute value is as supplied, using enveloped signatures and use
     * exclusive XML canonicalization. The resulting signature is inserted after the first child node (normally Issuer
     * element for SAML2) of the node to be signed.
     *
     * @param xmlString String representing the XML document to be signed.
     * @param idValue ID attribute value of the root node to be signed.
     * @param signingConfig The signing configuration.
     * @return Element representing the signature element.
     * @throws SAML2Exception If the document could not be signed.
     */
    Element sign(String xmlString, String idValue, SigningConfig signingConfig) throws SAML2Exception;

    /**
     * Verify the enveloped signature in the provided XML document.
     *
     * @param xmlString String representing an signed XML document.
     * @param idValue ID attribute value of the node whose signature is to be verified.
     * @param verificationCerts Certificates containing the public keys which may be used for signature verification;
     * These certificates may also may be used to check against the certificate included in the signature.
     * @return True if the xml signature was successfully verified, false otherwise.
     * @throws SAML2Exception If problem occurs during verification.
     */
    boolean verify(String xmlString, String idValue, Set<X509Certificate> verificationCerts) throws SAML2Exception;

    /**
     * Verify the enveloped signature in the provided XML document.
     *
     * @param xmlString String representing an signed XML document.
     * @param idAttribute The name of the ID attribute on the root element. May not be null.
     * @param idValue ID attribute value of the node whose signature is to be verified.
     * @param verificationCerts Certificates containing the public keys which may be used for signature verification;
     * These certificates may also may be used to check against the certificate included in the signature.
     * @return True if the xml signature was successfully verified, false otherwise.
     * @throws SAML2Exception If problem occurs during verification.
     */
    boolean verify(String xmlString, String idAttribute, String idValue, Set<X509Certificate> verificationCerts)
            throws SAML2Exception;
}
