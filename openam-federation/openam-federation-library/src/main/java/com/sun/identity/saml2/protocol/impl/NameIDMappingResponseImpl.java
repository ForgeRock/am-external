/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NameIDMappingResponseImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.NAME_ID_MAPPING_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.STATUS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.NameIDMappingResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;

public class NameIDMappingResponseImpl extends StatusResponseImpl
   implements NameIDMappingResponse {

    private static final Logger logger = LoggerFactory.getLogger(NameIDMappingResponseImpl.class);
    public final String elementName = "NameIDMappingResponse";
    NameID nameID = null;
    EncryptedID encryptedID = null;

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     */
    public NameIDMappingResponseImpl() {
        super(NAME_ID_MAPPING_RESPONSE);
        isMutable = true;
    }

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     *
     * @param element Document Element of <code>ManageNameIDRequest<code>
     *     object.
     * @throws SAML2Exception if <code>ManageNameIDRequest<code> cannot be
     *     created.
     */
    public NameIDMappingResponseImpl(Element element) throws SAML2Exception {
        super(NAME_ID_MAPPING_RESPONSE);
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
        makeImmutable();
    }

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     *
     * @param xmlString XML representation of the
     *     <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception if <code>ManageNameIDRequest<code> cannot be
     *     created.
     */
    public NameIDMappingResponseImpl(String xmlString) throws SAML2Exception {
        super(NAME_ID_MAPPING_RESPONSE);
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception("errorObtainingElement");
        }
        parseElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
        makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
        AssertionFactory af = AssertionFactory.getInstance();
        ProtocolFactory pf = ProtocolFactory.getInstance();
        
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "NameIDMappingResponseImpl.parseElement: Input is null.");
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
                    "NameIDMappingResponseImpl.parseElement: " +
                    "not ManageNameIDResponse.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        responseId = element.getAttribute("ID");
        validateID(responseId);
            
        version = element.getAttribute("Version");
        validateVersion(version);

        String issueInstantStr = element.getAttribute("IssueInstant");
        validateIssueInstant(issueInstantStr);
            
        destination = element.getAttribute("Destination");
        consent = element.getAttribute("Consent");
        inResponseTo = element.getAttribute("InResponseTo");

        NodeList nList = element.getChildNodes();

        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
                    if (cName.equals("Issuer")) {
                        issuer = af.createIssuer((Element)childNode);
                    } else if (cName.equals("Signature")) {
                        signatureString =
                            XMLUtils.getElementString((Element)childNode);
                        isSigned = true; 
                    } else if (cName.equals("Extensions")) {
                        extensions = pf.createExtensions((Element)childNode);
                    } else if (cName.equals("NameID")) {
                        nameID = af.createNameID((Element)childNode);
                    } else if (cName.equals("EncryptedID")) {
                        encryptedID = af.createEncryptedID((Element)childNode);
                    } else if (cName.equals("Status")) {
                        status = pf.createStatus((Element)childNode);
                    } 
                }
            }
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);
        if (isSigned && signedXMLString != null) {
            return fragment;
        }

        Element rootElement = (Element) fragment.getFirstChild();
        // The old code inserts the NameID or EncryptedNameID elements before the Status, so preserve that behaviour
        String prefix = includeNSPrefix ? PROTOCOL_PREFIX : "";
        Node statusNode = XMLUtils.getChildNode(rootElement, prefix + STATUS);

        if (nameID != null) {
            rootElement.insertBefore(nameID.toDocumentFragment(document, includeNSPrefix, declareNS), statusNode);
        }
        if (encryptedID != null) {
            rootElement.insertBefore(encryptedID.toDocumentFragment(document, includeNSPrefix, declareNS), statusNode);
        }

        return fragment;
    }

    /**
     * Returns the value of the <code>encryptedID</code> property.
     *
     * @return the value of the <code>encryptedID</code> property.
     * @see #setEncryptedID(EncryptedID)
     */
    public EncryptedID getEncryptedID()
    {
        return encryptedID;
    }

    /**
     * Sets the value of the <code>encryptedID</code> property.
     *
     * @param value the value of the <code>encryptedID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getEncryptedID
     */
    public void setEncryptedID(EncryptedID value) throws SAML2Exception
    {
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        encryptedID = value;
    }

    /**
     * Returns the value of the <code>nameID</code> property.
     *
     * @return the value of the <code>nameID</code> property.
     * @see #setNameID(NameID)
     */
    public NameID getNameID()
    {
        return nameID;
    }


    /**
     * Sets the value of the <code>nameID</code> property.
     *
     * @param value the value of the <code>nameID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getNameID
     */
    public void setNameID(NameID value) throws SAML2Exception
    {
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        nameID = value;
    }

    protected void validateData() throws SAML2Exception {
        if (((nameID != null) && (encryptedID != null)) ||
            ((nameID == null) && (encryptedID == null))){
            throw new SAML2Exception("nameIDMRespWrongID");
        }

    }
}
