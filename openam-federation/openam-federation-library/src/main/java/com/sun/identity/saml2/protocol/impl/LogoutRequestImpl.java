/**
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
 * $Id: LogoutRequestImpl.java,v 1.3 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2018-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.CONSENT;
import static com.sun.identity.saml2.common.SAML2Constants.DESTINATION;
import static com.sun.identity.saml2.common.SAML2Constants.ID;
import static com.sun.identity.saml2.common.SAML2Constants.ISSUE_INSTANT;
import static com.sun.identity.saml2.common.SAML2Constants.LOGOUT_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.NOTONORAFTER;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.REASON;
import static com.sun.identity.saml2.common.SAML2Constants.VERSION;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.SessionIndex;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class implements the <code>LogoutRequest</code> element in
 * SAML protocol schema.
 * It provides all the methods required by <code>LogoutRequest</code>
 */

public class LogoutRequestImpl extends RequestAbstractImpl
implements LogoutRequest {
    
    private static final Logger logger = LoggerFactory.getLogger(LogoutRequestImpl.class);
    private String reason;
    private Date notOnOrAfter;
    private EncryptedID encryptedId;
    private NameID nameId;
    private BaseID baseId;
    private List<String> sessionIndexList;
    
    /**
     * Constructs the <code>LogoutRequest</code> Object.
     *
     */
    public LogoutRequestImpl() {
        super(LOGOUT_REQUEST);
        isMutable = true;
    }
    
    /**
     * Constructs the <code>LogoutRequest</code> Object.
     *
     * @param element the Document Element of <code>LogoutRequest</code> object.
     * @throws SAML2Exception if <code>LogoutRequest</code> cannot be created.
     */
    
    public LogoutRequestImpl(Element element) throws SAML2Exception {
        super(LOGOUT_REQUEST);
        parseElement(element);
        if (isSigned) {
             signedXMLString = XMLUtils.print(element);
        }
    }
    
    /**
     * Constructs the <code>LogoutRequest</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>LogoutRequest</code> cannot be created.
     */
    public LogoutRequestImpl(String xmlString) throws SAML2Exception {
        super(LOGOUT_REQUEST);
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }
    
    /**
     * Returns the value of the notOnOrAfter property.
     *
     * @return <code>java.util.Date</code> value of the notOnOrAfter property
     * @see #setNotOnOrAfter(Date)
     */
    public java.util.Date getNotOnOrAfter() {
        return notOnOrAfter;
    }
    
    /**
     * Sets the value of the notOnOrAfter property.
     *
     * @param value <code>java.util.Date</code> value of the notOnOrAfter
     *        property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getNotOnOrAfter
     */
    public void setNotOnOrAfter(java.util.Date value) throws SAML2Exception {
        if (isMutable) {
            this.notOnOrAfter = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the reason property.
     *
     * @return <code>String</code> value of the reason property
     * @see #setReason(String)
     */
    public java.lang.String getReason() {
        return reason;
    }
    
    /**
     * Sets the value of the reason property.
     *
     * @param value <code>String</code> value of the reason property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getReason
     */
    public void setReason(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.reason = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the encryptedID property.
     *
     * @return the value of the encryptedID property
     * @see #setEncryptedID(EncryptedID)
     */
    public com.sun.identity.saml2.assertion.EncryptedID getEncryptedID() {
        return encryptedId;
    }
    
    /**
     * Sets the value of the encryptedID property.
     *
     * @param value the value of the encryptedID property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getEncryptedID
     */
    public void setEncryptedID(EncryptedID value)
    throws SAML2Exception {
        if (isMutable) {
            this.encryptedId = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the nameID property.
     *
     * @return the value of the nameID property
     * @see #setNameID(NameID)
     */
    public com.sun.identity.saml2.assertion.NameID getNameID() {
        return nameId;
    }
    
    /**
     * Sets the value of the nameID property.
     *
     * @param value the value of the nameID property to be set
     * @exception SAML2Exception if the object is immutable
     * @see #getNameID
     */
    public void setNameID(com.sun.identity.saml2.assertion.NameID value)
    throws SAML2Exception {
        if (isMutable) {
            this.nameId = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the baseID property.
     *
     * @return the value of the baseID property
     * @see #setBaseID(BaseID)
     */
    public com.sun.identity.saml2.assertion.BaseID getBaseID() {
        return baseId;
    }
    
    /**
     * Sets the value of the baseID property.
     *
     * @param value the value of the baseID property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getBaseID
     */
    public void setBaseID(com.sun.identity.saml2.assertion.BaseID value)
    throws SAML2Exception {
        if (isMutable) {
            this.baseId = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the SessionIndex property.
     *
     * @return A list containing objects of type <code>java.lang.String</code>
     * @see #setSessionIndex(List)
     */
    public java.util.List<String> getSessionIndex() {
        return sessionIndexList;
    }
    
    /**
     * Sets the value of the SessionIndex property.
     *
     * @param sessionIndexList
     *        A list containing objects of type <code>java.lang.String</code>
     * @throws SAML2Exception if the object is immutable
     * @see #getSessionIndex
     */
    public void setSessionIndex(java.util.List sessionIndexList)
    throws SAML2Exception {
        if (isMutable) {
            this.sessionIndexList = sessionIndexList;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();

        if (isSigned && signedXMLString != null) {
            Document parsedSig = XMLUtils.toDOMDocument(signedXMLString);
            if (parsedSig == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
            }
            fragment.appendChild(document.importNode(parsedSig.getDocumentElement(), true));
            return fragment;
        }

        Element logoutRequestElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                LOGOUT_REQUEST, includeNSPrefix, declareNS);
        fragment.appendChild(logoutRequestElement);

        logoutRequestElement.setAttribute(ID, requestId);
        logoutRequestElement.setAttribute(VERSION, version);
        logoutRequestElement.setAttribute(ISSUE_INSTANT, DateUtils.toUTCDateFormat(issueInstant));

        if (isNotBlank(destinationURI)) {
            logoutRequestElement.setAttribute(DESTINATION, destinationURI);
        }

        if (isNotBlank(consent)) {
            logoutRequestElement.setAttribute(CONSENT, consent);
        }
        if (notOnOrAfter != null) {
            logoutRequestElement.setAttribute(NOTONORAFTER, DateUtils.toUTCDateFormat(notOnOrAfter));
        }
        if (isNotBlank(reason)) {
            logoutRequestElement.setAttribute(REASON, reason);
        }

        // NB "nameID" here is actually the Issuer from the superclass, nameId (lowercase d) is the local NameID field
        if (nameID != null) {
            logoutRequestElement.appendChild(nameID.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (isNotBlank(signatureString)) {
            List<Node> sigNodes = SAML2Utils.parseSAMLFragment(signatureString);
            for (Node node : sigNodes) {
                logoutRequestElement.appendChild(document.importNode(node, true));
            }
        }
        if (extensions != null) {
            logoutRequestElement.appendChild(extensions.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (baseId != null) {
            logoutRequestElement.appendChild(baseId.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (nameId != null) {
            logoutRequestElement.appendChild(nameId.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (encryptedId != null) {
            logoutRequestElement.appendChild(encryptedId.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        if (isNotEmpty(sessionIndexList)) {
            for (String sessionString : sessionIndexList) {
                ProtocolFactory protoFactory = ProtocolFactory.getInstance();
                SessionIndex sIndex = protoFactory.createSessionIndex(sessionString);
                logoutRequestElement.appendChild(sIndex.toDocumentFragment(document, includeNSPrefix, declareNS));
            }
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable()	 {
        if (isMutable) {
            super.makeImmutable();
            if ((baseId != null) && (baseId.isMutable())) {
                baseId.makeImmutable();
            }
            if ((nameId != null) && (nameId.isMutable())) {
                nameId.makeImmutable();
            }
            isMutable = false;
        }
    }
    
    /**
     * Returns true if object is mutable.
     *
     * @return true if object is immutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /**
     * Parses the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    private void parseElement(Element element) throws SAML2Exception {
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        requestId = element.getAttribute(SAML2Constants.ID);
        validateID(requestId);
        
        version = element.getAttribute(SAML2Constants.VERSION);
        validateVersion(version);
        
        String issueInstantStr = element.getAttribute(
        SAML2Constants.ISSUE_INSTANT);
        validateIssueInstant(issueInstantStr);
        
        destinationURI = element.getAttribute(SAML2Constants.DESTINATION);
        consent = element.getAttribute(SAML2Constants.CONSENT);
        String notOnOrAfterStr = element.getAttribute(
        SAML2Constants.NOTONORAFTER);
        validateNotOnOrAfterStr(notOnOrAfterStr);
        reason = element.getAttribute(SAML2Constants.REASON);
        
        String sessionIndexStr = null;
        
        NodeList nList = element.getChildNodes();
        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
                    if (cName.equals(SAML2Constants.ISSUER)) {
                        nameID =
                        assertionFactory.createIssuer((Element)childNode);
                    } else if (cName.equals(SAML2Constants.SIGNATURE)) {
                        signatureString = XMLUtils.print((Element) childNode);
                        isSigned = true;
                    } else if (cName.equals(SAML2Constants.EXTENSIONS)) {
                        extensions =
                        protoFactory.createExtensions((Element)childNode);
                    } else if (cName.equals(SAML2Constants.BASEID)) {
                        baseId =
                        assertionFactory.createBaseID((Element)childNode);
                    } else if (cName.equals(SAML2Constants.NAMEID)) {
                        nameId =
                        assertionFactory.createNameID((Element)childNode);
                    } else if (cName.equals(SAML2Constants.ENCRYPTEDID)) {
                        encryptedId =
                        assertionFactory.createEncryptedID((Element)childNode);
                    } else if (cName.equals(SAML2Constants.SESSION_INDEX)) {
                        if ((sessionIndexList == null) ||
                        (sessionIndexList.isEmpty())) {
                            sessionIndexList = new ArrayList();
                        }
                        sessionIndexStr =
                        XMLUtils.getElementString((Element)childNode);
                        sessionIndexList.add(sessionIndexStr);
                    }
                }
            }
            validateBaseIDorNameIDorEncryptedID();
            if ((sessionIndexList != null) && (!sessionIndexList.isEmpty())) {
                sessionIndexList =
                Collections.unmodifiableList(sessionIndexList);
            }
        }
    }
    
   /* Validates the NotOnOrAfter attribute in the SAML Request. */
    private void validateNotOnOrAfterStr(String notOnOrAfterStr)
    throws SAML2Exception {
       if (notOnOrAfterStr != null && notOnOrAfterStr.length() != 0) {
            try {
                notOnOrAfter = DateUtils.stringToDate(notOnOrAfterStr);
            } catch (ParseException e) {
                logger.debug("Error parsing NotOnOrAfterStr", e);
                throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("incorrectNotOnOrAfterStr"));
            }
        }
    }
    
    /* validate the sequence and occurence of BaseID Element*/
    private void validateBaseIDorNameIDorEncryptedID() throws SAML2Exception {
        if (baseId == null && nameId == null && encryptedId == null) {
            logger.debug("BaseID ,NameID ,EncryptedID NULL ");
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingBaseIDorNameIDorEncryptedID"));
        }
    }
    
}
