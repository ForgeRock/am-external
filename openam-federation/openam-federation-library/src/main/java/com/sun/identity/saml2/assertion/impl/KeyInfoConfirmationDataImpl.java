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
 * $Id: KeyInfoConfirmationDataImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2019-2023 ForgeRock AS.
 */


package com.sun.identity.saml2.assertion.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.KeyInfoConfirmationData;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>KeyInfoConfirmationData</code> constrains a 
 * <code>SubjectConfirmationData</code> element to contain one or more
 * <ds:KeyInfo> elements that identify cryptographic keys that are used
 * in some way to authenticate an attesting entity. The particular 
 * confirmation method MUST define the exact mechanism by which the
 * confirmation data can be used. The optional attributes defined by
 * <code>SubjectConfirmationData</code> MAY also appear.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class KeyInfoConfirmationDataImpl
extends SubjectConfirmationDataImpl implements  KeyInfoConfirmationData 
{
    private static final Logger logger = LoggerFactory.getLogger(KeyInfoConfirmationDataImpl.class);
    public final String elementName = "KeyInfoConfirmationData";
    private List keyInfo = null;
        
    public KeyInfoConfirmationDataImpl() {
        mutable = true;
    }

    public KeyInfoConfirmationDataImpl(Element element) throws SAML2Exception {
        parseElement(element);
        makeImmutable();
    }

    public KeyInfoConfirmationDataImpl(String xmlString) throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(
                       xmlString);
        if (doc == null) {
            throw new SAML2Exception("");
        }
        parseElement(doc.getDocumentElement());
        makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "KeyInfoConfirmationDataImpl.parseElement: " 
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }

        // Make sure this is an EncryptedAssertion.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "KeyInfoConfirmationDataImpl.parseElement: "
                    + "not EncryptedIDImpl.");
            }
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <KeyInfoConfirmationData> element
        NamedNodeMap attrs = ((Node)element).getAttributes();
        parseAttributes(attrs);
        parseContent(element);
        parseKeyInfo(element);
    }
        
    private void parseKeyInfo(Element element) throws SAML2Exception {
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            Node child = nl.item(i);
            String childName = child.getLocalName();
            if (childName == null) {
                continue;
            }
            if (!childName.equals("KeyInfo")) {
                continue;
            }
            getKeyInfo().add(XMLUtils.print((Element)child));
        }
    }

    /**
     * Returns the list of <code>KeyInfo</code> object.
     *
     * @return a List of <code>KeyInfo</code> objects.
     * @see #setKeyInfo(List)
     */
    public List getKeyInfo()
    {
            if (keyInfo == null) {
                keyInfo = new ArrayList();
            }
            
            return keyInfo;
    }

    /**
     * Sets the List of <code>KeyInfo</code> object.
     *
     * @param info List of <code>KeyInfo</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getKeyInfo()
     */
    public void setKeyInfo(List info)  throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        keyInfo = info;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(elementName, document, includeNSPrefix, declareNS);
        Node rootElement = fragment.getFirstChild();

        if (!getKeyInfo().isEmpty()) {
            for (Object obj : keyInfo) {
                Document parsedXml = XMLUtils.toDOMDocument((String) obj);
                if (parsedXml != null) {
                    rootElement.appendChild(document.adoptNode(parsedXml.getDocumentElement()));
                } else {
                    logger.error("KeyInfoConfirmationDataImpl.toDocumentFragment(): KeyData is not well-formed " +
                            "XML: {}", obj);
                }
            }
        }

        return fragment;
    }

    /**
    * Makes the object immutable
    */
    public void makeImmutable()
    {
        if (!mutable) {
            return;
        }

        super.makeImmutable();
  
        if ((keyInfo != null) && (!keyInfo.isEmpty())) {
            keyInfo = Collections.unmodifiableList(keyInfo);
        }
    }
}
