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
 * $Id: RequestAbstractImpl.java,v 1.5 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.protocol.impl;


import static com.sun.identity.saml2.common.SAML2Constants.CONSENT;
import static com.sun.identity.saml2.common.SAML2Constants.DESTINATION;
import static com.sun.identity.saml2.common.SAML2Constants.ID;
import static com.sun.identity.saml2.common.SAML2Constants.ISSUE_INSTANT;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.VERSION;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This abstract class defines methods for setting and retrieving attributes and
 * elements associated with a SAML request message used in SAML protocols. This
 * class is the base class for all SAML Requests.
 */


public abstract class RequestAbstractImpl implements RequestAbstract {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestAbstractImpl.class);
    protected Issuer nameID = null;
    protected Extensions extensions = null;
    protected String requestId = null; 
    protected String version = null;
    protected Date issueInstant = null;
    protected String destinationURI = null;
    protected String consent = null;
    protected boolean isSigned = false;
    protected Boolean isSignatureValid = null;
    protected PublicKey publicKey = null;
    protected boolean isMutable = false;
    protected String  signatureString = null;
    protected String  signedXMLString = null; 
    protected String elementName;

    /**
     * Sets the container XML element name.
     *
     * @param elementName the container XML element name such as {@code AuthnRequest}. Don't include any namespace
     *                    prefix.
     */
    protected RequestAbstractImpl(String elementName) {
        this.elementName = elementName;
    }
 
    /**
     * Sets the <code>Issuer</code> object.
     *
     * @param nameID the new <code>Issuer</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssuer
     */
    public void setIssuer(Issuer nameID) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.nameID = nameID ;
    }
    
    /**
     * Returns the <code>Issuer</code> Object.
     *
     * @return the <code>Issuer</code> object.
     * @see #setIssuer(Issuer)
     */
    public Issuer getIssuer() {
        return nameID;
    }
    
    /**
     * Returns the <code>Signature</code> Object as a string.
     *
     * @return the <code>Signature</code> object as a string.
     */
    public String getSignature() {
        return signatureString;
    }

    @Override
    public void sign(SigningConfig signingConfig) throws SAML2Exception {
        Element signatureEle = SigManager.getSigInstance().sign(toXMLString(true, true), getID(), signingConfig);
        signatureString = XMLUtils.print(signatureEle);
        signedXMLString = XMLUtils.print(signatureEle.getOwnerDocument().getDocumentElement());
        isSigned = true;
        makeImmutable();
    }   
        
    /**
     * Sets the <code>Extensions</code> Object.
     *
     * @param extensions the <code>Extensions</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getExtensions
     */
    public void setExtensions(Extensions extensions) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.extensions = extensions;
    }
 
    /**
     * Returns the <code>Extensions</code> Object.
     *
     * @return the <code>Extensions</code> object.
     * @see #setExtensions(Extensions)
     */
    public Extensions getExtensions() {
        return extensions;
    }
    
    /**
     * Sets the value of the <code>ID</code> attribute.
     *
     * @param id the new value of <code>ID</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getID
     */
    public void setID(String id) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        this.requestId = id;
    }
    
    /**
     * Returns the value of the <code>ID</code> attribute.
     *
     * @return the value of <code>ID</code> attribute.
     * @see #setID(String)
     */
    public String getID () {
        return requestId;
    }
    
    /**
     * Sets the value of the <code>Version</code> attribute.
     *
     * @param version the value of <code>Version</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getVersion
     */
    public void setVersion(String version) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.version = version;
    }

    /** 
     * Returns the value of the <code>Version</code> attribute.
     *
     * @return value of <code>Version</code> attribute.
     * @see #setVersion(String)
     */

    public String getVersion() {
        return version;
    }
    
    /**
     * Sets the value of <code>IssueInstant</code> attribute.
     *
     * @param dateTime new value of the <code>IssueInstant</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssueInstant
     */
    public void setIssueInstant(Date dateTime) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        issueInstant = dateTime;
    }
    
    /**
     * Returns the value of <code>IssueInstant</code> attribute.
     *
     * @return value of the <code>IssueInstant</code> attribute.
     * @see #setIssueInstant(Date)
     */
    public Date getIssueInstant() {
        return issueInstant;
    }
    
    /**
     * Sets the value of the <code>Destination</code> attribute.
     *
     * @param destinationURI new value of <code>Destination</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDestination
     */
    public void  setDestination(String destinationURI) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        this.destinationURI = destinationURI;
    }
    
    /**
     * Returns the value of the <code>Destination</code> attribute.
     *
     * @return  the value of <code>Destination</code> attribute.
     * @see #setDestination(String)
     */
    public String getDestination() {
        return destinationURI;
    }
    
    /** 
     * Sets the value of the Consent property.
     *
     * @param consent ,  value of Consent property.
     * @see #getConsent
     */
    public void setConsent(String consent) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        this.consent = consent;
    }
    
    /**
     * Sets the value of the <code>Consent</code> attribute.
     *
     * @return the value of <code>Consent</code> attribute.
     * @see #setConsent(String)
     */
    public String getConsent() {
        return consent;
    }
    
    /**
     * Returns true if message is signed.
     *
     * @return true if message is signed.
     */
    public boolean isSigned() {
        return isSigned;
    }

    @Override
    public boolean isSignatureValid(Set<X509Certificate> verificationCerts)
        throws SAML2Exception {
        if (isSignatureValid == null) {
            isSignatureValid = SigManager.getSigInstance().verify(signedXMLString, getID(), verificationCerts);
        }
        return isSignatureValid.booleanValue();
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();

        if (isSigned && signedXMLString != null) {
            Document signedDoc = XMLUtils.toDOMDocument(signedXMLString);
            if (signedDoc == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
            }
            fragment.appendChild(document.importNode(signedDoc.getDocumentElement(), true));
            return fragment;
        }

        Element rootElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE, elementName,
                includeNSPrefix, declareNS);
        fragment.appendChild(rootElement);

        rootElement.setAttribute(ID, requestId);
        rootElement.setAttribute(VERSION, version);
        rootElement.setAttribute(ISSUE_INSTANT, DateUtils.toUTCDateFormat(issueInstant));

        if (isNotBlank(destinationURI)) {
            rootElement.setAttribute(DESTINATION, destinationURI);
        }
        if (isNotBlank(consent)) {
            rootElement.setAttribute(CONSENT, consent);
        }

        if (nameID != null) {
            rootElement.appendChild(nameID.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (isNotBlank(signatureString)) {
            Document signatureDoc = XMLUtils.toDOMDocument(signatureString);
            rootElement.appendChild(document.importNode(signatureDoc.getDocumentElement(), true));
        }
        if (extensions != null) {
            rootElement.appendChild(extensions.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        return fragment;
    }

    @Deprecated(forRemoval = true, since = "8.0.0")
    protected String getAttributesString() throws SAML2Exception {
        StringBuffer xml = new StringBuffer();

        xml.append("ID=\"");
        xml.append(requestId);
        xml.append("\" ");

        xml.append("Version=\"");
        xml.append(version);
        xml.append("\" ");

        xml.append("IssueInstant=\"");
        xml.append(DateUtils.toUTCDateFormat(issueInstant));
        xml.append("\" ");

        if ((destinationURI != null) && (destinationURI.length() > 0)) {
            xml.append("Destination=\"");
            xml.append(destinationURI);
            xml.append("\" ");
        }

        if ((consent != null) && (consent.length() > 0)) {
            xml.append("Consent=\"");
            xml.append(consent);
            xml.append("\" ");
        }

        return xml.toString();
    }

    @Deprecated(forRemoval = true, since = "8.0.0")
    protected String getElements(boolean includeNSPrefix, boolean declareNS) 
    throws SAML2Exception {
        StringBuffer xml = new StringBuffer();
        if (nameID != null) {
            xml.append(nameID.toXMLString(includeNSPrefix,declareNS));
        }

        if (signatureString != null && !signatureString.equals(""))  {
            xml.append(signatureString);
        }

        if (extensions != null) {
            xml.append(extensions.toXMLString(includeNSPrefix,declareNS));
        }

        return xml.toString();
    }


    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
	if (isMutable) {
	    if ((nameID != null) && (nameID.isMutable())) {
	    	nameID.makeImmutable();
	    }

	    if ((extensions != null) && (extensions.isMutable())) {
		extensions.makeImmutable();
	    }
	    isMutable=false;
	}
    }
	

    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
	return isMutable;
    }


   /* Validates the requestID in the SAML Request. */
    protected void validateID(String requestID) throws SAML2Exception {
        if (StringUtils.isEmpty(requestID)) {
            logger.debug("ID is missing in the SAMLRequest");
                throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingIDAttr"));
        }
	    // ID must be an XML NCName
        if (!XMLUtils.isNCName(requestID)) {
            logger.debug("SAMLRequest ID is not a valid XML ID (NCName): {}", requestID);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingIDAttr"));
        }
    }


   /* Validates the version in the SAML Request. */
    protected void validateVersion(String version) throws SAML2Exception {
	if ((version == null) || (version.length() == 0) ) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                                                "missingVersion"));
	} else if (!version.equals(SAML2Constants.VERSION_2_0)) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                                        "incorrectVersion"));
	}
    }

   /* Validates the IssueInstant attribute in the SAML Request. */
    protected void validateIssueInstant(String issueInstantStr)
                 	throws SAML2Exception {
	if ((issueInstantStr == null || issueInstantStr.length() == 0)) {
            throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("missingIssueInstant"));
	} else {
            try {
                issueInstant = DateUtils.stringToDate(issueInstantStr);
            } catch (ParseException e) {
		logger.debug("Error parsing IssueInstant", e);
		throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("incorrectIssueInstant"));
            }
	}
    }

   /* Validates the required elements in the SAML Request. */
    protected void validateData() throws SAML2Exception {
	validateID(requestId);
	validateVersion(version);
	if (issueInstant == null) {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("incorrectIssueInstant"));
	}
	validateIssueInstant(DateUtils.dateToString(issueInstant));
    }

    /** 
     * Parses the Docuemnt Element for this object.
     * 
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMElement(Element element) throws SAML2Exception {

        parseDOMAttributes(element);

        List childElementList = new ArrayList();
        NodeList nList = element.getChildNodes();
        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    childElementList.add(childNode);
                }
            }
        }
        ListIterator iter = childElementList.listIterator();
        parseDOMChileElements(iter);
        if (iter.hasNext()) {
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "parseDOMElement: Unexpected child element found");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    /** 
     * Parses attributes of the Docuemnt Element for this object.
     * 
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        requestId = element.getAttribute(SAML2Constants.ID);
        validateID(requestId);
        
        version = element.getAttribute(SAML2Constants.VERSION);
        validateVersion(version);

        String issueInstantStr = element.getAttribute(
                                    SAML2Constants.ISSUE_INSTANT);
        validateIssueInstant(issueInstantStr);
        
        destinationURI = element.getAttribute(SAML2Constants.DESTINATION);
        consent = element.getAttribute(SAML2Constants.CONSENT);
    }

    /** 
     * Parses child elements of the Docuemnt Element for this object.
     * 
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {

        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();

        while (iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.ISSUER.equals(localName)) {
                validateIssuer();
                nameID = assertionFactory.createIssuer(childElement);
            } else if (SAML2Constants.SIGNATURE.equals(localName)) {
                validateSignature();
                signatureString = XMLUtils.print(childElement);
                isSigned = true;
            } else if (SAML2Constants.EXTENSIONS.equals(localName)) {
                validateExtensions();
                extensions = protoFactory.createExtensions(childElement);
            } else {
                iter.previous();
                break;
            }
        }
    }

    /* validate the sequence and occurence of Issuer Element*/
    private void validateIssuer() throws SAML2Exception {
        if (nameID != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "validateIssuer: Too many Issuer Element");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        } 

        if ((signatureString != null) || (extensions != null)) {
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "validateIssuer: Issuer Element should be the " +
                    "first element in the Request");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    /* validate the sequence and occurence of Signature Element*/
    private void validateSignature() throws SAML2Exception {
        if (signatureString != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "validateSignature: Too many Signature Elements");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        } 

        if (extensions != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "validateSignature: Signature should be in front of " +
                    "Extensions");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    /* validate the sequence and occurence of Extensions Element*/
    private void validateExtensions() throws SAML2Exception {
        if (extensions != null) { 
            if (logger.isDebugEnabled()) {
                logger.debug("RequestAbstractImpl." +
                    "validateExtensions: Too many Extension Elements");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        } 
    }
}
