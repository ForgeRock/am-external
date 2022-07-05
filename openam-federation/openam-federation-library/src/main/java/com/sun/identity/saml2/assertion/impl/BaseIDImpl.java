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
 * $Id: BaseIDImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *  The <code>BaseID</code> is an extension point that allows 
 *  applications to add new kinds of identifiers.
 */
public class BaseIDImpl extends BaseIDAbstractImpl implements BaseID {
    private static final Logger logger = LoggerFactory.getLogger(BaseIDImpl.class);
    public static final String BASE_ID_ELEMENT = "BaseID";
    public static final String NAME_QUALIFIER_ATTR = "NameQualifier";
    public static final String SP_NAME_QUALIFIER_ATTR = "SPNameQualifier";

   /** 
    * Default constructor
    */
    public BaseIDImpl() {
    }

    /**
     * This constructor is used to build <code>BaseID</code> object from
     * a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>BaseID</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public BaseIDImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "BaseIDImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>BaseID</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>BaseID</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public BaseIDImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            logger.error(
                "BaseIDImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_assertion_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            logger.error(
                "BaseIDImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(BASE_ID_ELEMENT)) {
            logger.error("BaseIDImpl.processElement(): "
                + "invalid local name " + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(NAME_QUALIFIER_ATTR);
        if (attrValue != null) {
            setNameQualifier(attrValue);
        }

        attrValue = element.getAttribute(SP_NAME_QUALIFIER_ATTR);
        if (attrValue != null) {
            setSPNameQualifier(attrValue);
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS) {
        DocumentFragment fragment = document.createDocumentFragment();
        Element baseIdElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                BASE_ID_ELEMENT, includeNSPrefix, declareNS);
        fragment.appendChild(baseIdElement);

        String nameQualifier = getNameQualifier();
        if (nameQualifier != null) {
            baseIdElement.setAttribute(NAME_QUALIFIER_ATTR, nameQualifier);
        }
        String spNameQualifier = getSPNameQualifier();
        if (spNameQualifier != null) {
            baseIdElement.setAttribute(SP_NAME_QUALIFIER_ATTR, spNameQualifier);
        }
        return fragment;
    }
}
