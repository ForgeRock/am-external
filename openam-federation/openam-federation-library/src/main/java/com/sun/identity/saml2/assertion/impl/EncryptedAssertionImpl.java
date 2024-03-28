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
 * $Id: EncryptedAssertionImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 * Portions copyright 2014-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.assertion.impl;

import java.security.PrivateKey;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.xmlenc.EncManager;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>EncryptedAssertion</code> represents an assertion in
 * encrypted fashion, as defined by the XML Encryption Syntax and
 * Processing specification [XMLEnc]. The EncryptedAssertion contains
 * an <code>EncryptedData</code> and zero or more
 * <code>EncryptedKey</code>s.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class EncryptedAssertionImpl extends EncryptedElementImpl implements EncryptedAssertion {
    private static final Logger logger = LoggerFactory.getLogger(EncryptedAssertionImpl.class);
    public final String elementName = "EncryptedAssertion";

    // used by the constructors.
    private void parseElement(Element element)
            throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("EncryptedAssertionImpl."
                        + "parseElement: Input is null.");
            }
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("nullInput"));
        }

        // Make sure this is an EncryptedAssertion.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (logger.isDebugEnabled()) {
                logger.debug("EncryptedAssertionImpl."
                        + "parseElement: not EncryptedAssertion.");
            }
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("wrongInput"));
        }

    }

    /**
     * Default Class constructor
     */
    EncryptedAssertionImpl() {
    }

    /**
     * Class constructor with <code>EncryptedAssertion</code> in
     * <code>Element</code> format.
     */
    public EncryptedAssertionImpl(Element element)
            throws SAML2Exception {
        parseElement(element);
        xmlString = XMLUtils.print(element);
    }

    /**
     * Class constructor with <code>EncryptedAssertion</code> in xml string
     * format.
     */
    public EncryptedAssertionImpl(String xmlString)
            throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        this.xmlString = xmlString;
    }

    @Override
    public Assertion decrypt(Set<PrivateKey> privateKeys) throws SAML2Exception {
        Element el = EncManager.getEncInstance().decrypt(xmlString, privateKeys);

        SAML2SDKUtils.decodeXMLToDebugLog("EncryptedAssertionImpl.decrypt: ", el);

        return AssertionFactory.getInstance().createAssertion(el);
    }
}
