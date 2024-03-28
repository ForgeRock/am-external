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
 * $Id: NameIDImpl.java,v 1.3 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2015-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.xmlenc.EncManager;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *  The <code>NameID</code> is used in various SAML assertion constructs
 *  such as <code>Subject</code> and <code>SubjectConfirmation</code>
 *  elements, and in various protocol messages.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class NameIDImpl extends NameIDTypeImpl implements NameID {

    private static final Logger logger = LoggerFactory.getLogger(NameIDImpl.class);
    public static final String NAME_ID_ELEMENT = "NameID";

    /** 
     * Default constructor
     */
    public NameIDImpl() {
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>NameID</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public NameIDImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "NameIDImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>NameID</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public NameIDImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            logger.error(
                "NameIDImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            logger.error(
                "NameIDImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(NAME_ID_ELEMENT)) {
            logger.error(
                "NameIDImpl.processElement(): invalid local name " 
                + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }
        getValueAndAttributes(element);
    }

    @Override
    public EncryptedID encrypt(EncryptionConfig encryptionConfig, String recipientEntityID) throws SAML2Exception {
        Element el = EncManager.getEncInstance().encrypt(toXMLString(true, true),
                encryptionConfig, recipientEntityID, "EncryptedID");
        return AssertionFactory.getInstance().createEncryptedID(el);
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {

        DocumentFragment fragment = document.createDocumentFragment();
        Element nameIdElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                NAME_ID_ELEMENT, includeNSPrefix, declareNS);
        fragment.appendChild(nameIdElement);

        String nameQualifier = getNameQualifier();
        if (isNotBlank(nameQualifier)) {
            nameIdElement.setAttribute(NAME_QUALIFIER_ATTR, nameQualifier);
        }
        String spNameQualifier = getSPNameQualifier();
        if (isNotBlank(spNameQualifier)) {
            nameIdElement.setAttribute(SP_NAME_QUALIFIER_ATTR, spNameQualifier);
        }
        String format = getFormat();
        if (isNotBlank(format)) {
            nameIdElement.setAttribute(FORMAT_ATTR, format);
        }
        String spProvidedID = getSPProvidedID();
        if (isNotBlank(spProvidedID)) {
            nameIdElement.setAttribute(SP_PROVIDED_ID_ATTR, spProvidedID);
        }
        String value = getValue();
        if (isNotBlank(value)) {
            nameIdElement.setTextContent(value);
        } else {
            logger.error("NameIDImpl.toDocumentFragment(): name identifier is missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missing_name_identifier"));
        }

        return fragment;
    }
}
