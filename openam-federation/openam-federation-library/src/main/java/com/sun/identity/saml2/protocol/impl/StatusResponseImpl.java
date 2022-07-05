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
 * $Id: StatusResponseImpl.java,v 1.4 2008/06/25 05:48:01 qcheng Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */
package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.CONSENT;
import static com.sun.identity.saml2.common.SAML2Constants.DESTINATION;
import static com.sun.identity.saml2.common.SAML2Constants.ID;
import static com.sun.identity.saml2.common.SAML2Constants.INRESPONSETO;
import static com.sun.identity.saml2.common.SAML2Constants.ISSUE_INSTANT;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.VERSION;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusResponse;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This class defines methods for setting and retrieving attributes and
 * elements associated with a SAML response message used in SAML protocols.
 * This class is the base class for all SAML Responses.
 */

public abstract class StatusResponseImpl implements StatusResponse {
    
    private static final Logger logger = LoggerFactory.getLogger(StatusResponseImpl.class);
    protected String version = null;
    protected Date issueInstant = null;
    protected String destination = null;
    protected String signatureString = null;
    protected Extensions extensions = null;
    protected String consent = null;
    protected String inResponseTo = null;
    protected Status status = null;
    protected String responseId = null;
    protected Issuer issuer = null;
    protected boolean isSigned = false;
    protected Boolean isSignatureValid = null;
    protected boolean isMutable = false;
    protected PublicKey publicKey = null;
    protected String  signedXMLString = null;
    protected String elementName;

    /**
     * Sets the container XML element name for this specific Status Response subtype.
     *
     * @param elementName the element name such as {@code ManageNameIDResponse}. Don't include the namespace prefix.
     */
    protected StatusResponseImpl(String elementName) {
        this.elementName = elementName;
    }

    /**
     * Returns the value of the version property.
     *
     * @return the value of the version property
     * @see #setVersion(String)
     */
    public java.lang.String getVersion() {
        return version;
    }
    
    /**
     * Sets the value of the version property.
     *
     * @param value the value of the version property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getVersion
     */
    public void setVersion(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.version = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the issueInstant property.
     *
     * @return the value of the issueInstant property
     * @see #setIssueInstant(java.util.Date)
     */
    public java.util.Date getIssueInstant() {
        return issueInstant;
    }
    
    /**
     * Sets the value of the issueInstant property.
     *
     * @param value the value of the issueInstant property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssueInstant
     */
    public void setIssueInstant(java.util.Date value) throws SAML2Exception {
        if (isMutable) {
            this.issueInstant = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the destination property.
     *
     * @return the value of the destination property
     * @see #setDestination(String)
     */
    public java.lang.String getDestination() {
        return destination;
    }
    
    /**
     * Sets the value of the destination property.
     *
     * @param value the value of the destination property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getDestination
     */
    public void setDestination(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.destination = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the signature element, the <code>StatusResponse</code> contains
     * as <code>String</code>.
     * It returns null if the <code>StatusResponse</code> has no signature.
     *
     * @return <code>String</code> representation of the signature.
     *
     */
    public String getSignature() {
        return signatureString;
    }

    @Override
    public void sign(SigningConfig signingConfig) throws SAML2Exception {
        Element signatureEle = SigManager.getSigInstance().sign(toXMLString(true, true), getID(), signingConfig);
        signatureString = XMLUtils.print(signatureEle);
        signedXMLString = XMLUtils.print(signatureEle.getOwnerDocument().getDocumentElement(), "UTF-8");
        isSigned = true;
        makeImmutable();
    }

    /**
     * Returns the value of the extensions property.
     *
     * @return the value of the extensions property
     * @see #setExtensions(Extensions)
     */
    public Extensions getExtensions() {
        return extensions;
    }
    
    /**
     * Sets the value of the extensions property.
     *
     * @param value the value of the extensions property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getExtensions
     */
    public void setExtensions(Extensions value) throws SAML2Exception {
        if (isMutable) {
            this.extensions = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the consent property.
     *
     * @return the value of the consent property
     * @see #setConsent(String)
     */
    public java.lang.String getConsent() {
        return consent;
    }
    
    /**
     * Sets the value of the consent property.
     *
     * @param value the value of the consent property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getConsent
     */
    public void setConsent(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.consent = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the inResponseTo property.
     *
     * @return the value of the inResponseTo property
     * @see #setInResponseTo(String)
     */
    public java.lang.String getInResponseTo() {
        return inResponseTo;
    }
    
    /**
     * Sets the value of the inResponseTo property.
     *
     * @param value the value of the inResponseTo property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getInResponseTo
     */
    public void setInResponseTo(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.inResponseTo = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the status property.
     *
     * @return the value of the status property
     * @see #setStatus(Status)
     */
    public com.sun.identity.saml2.protocol.Status getStatus() {
        return status;
    }
    
    /**
     * Sets the value of the status property.
     *
     * @param value the value of the status property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatus
     */
    public void setStatus(com.sun.identity.saml2.protocol.Status value)
    throws SAML2Exception {
        if (isMutable) {
            this.status = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the id property.
     *
     * @return the value of the id property
     * @see #setID(String)
     */
    public java.lang.String getID() {
        return responseId;
    }
    
    /**
     * Sets the value of the id property.
     *
     * @param value the value of the id property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getID
     */
    public void setID(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.responseId = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the value of the issuer property.
     *
     * @return the value of the issuer property
     * @see #setIssuer(Issuer)
     */
    public Issuer getIssuer() {
        return issuer;
    }
    
    /**
     * Sets the value of the issuer property.
     *
     * @param value the value of the issuer property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getIssuer
     */
    public void setIssuer(Issuer value)
    throws SAML2Exception {
        if (isMutable) {
            this.issuer = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns whether the <code>StatusResponse</code> is signed or not.
     *
     * @return true if the <code>StatusResponse</code> is signed.
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
            Document parsed = XMLUtils.toDOMDocument(signedXMLString);
            if (parsed == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
            }
            fragment.appendChild(document.adoptNode(parsed.getDocumentElement()));
            return fragment;
        }
        Element responseElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                elementName, includeNSPrefix, declareNS);
        fragment.appendChild(responseElement);

        responseElement.setAttribute(ID, responseId);
        responseElement.setAttribute(VERSION, version);
        responseElement.setAttribute(ISSUE_INSTANT, DateUtils.toUTCDateFormat(issueInstant));

        if (isNotBlank(destination)) {
            responseElement.setAttribute(DESTINATION, destination);
        }
        if (isNotBlank(consent)) {
            responseElement.setAttribute(CONSENT, consent);
        }
        if (isNotBlank(inResponseTo)) {
            responseElement.setAttribute(INRESPONSETO, inResponseTo);
        }

        if (issuer != null) {
            responseElement.appendChild(issuer.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (isNotBlank(signatureString)) {
            List<Node> sigNodes = SAML2Utils.parseSAMLFragment(signatureString);
            for (Node node : sigNodes) {
                responseElement.appendChild(document.adoptNode(node));
            }
        }
        if (extensions != null) {
            responseElement.appendChild(extensions.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (status != null) {
            responseElement.appendChild(status.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            if ((issuer != null) && (issuer.isMutable())) {
                issuer.makeImmutable();
            }
            if ((extensions != null) && (extensions.isMutable())) {
                extensions.makeImmutable();
            }
            if ((status != null) && (status.isMutable())) {
                status.makeImmutable();
            }
            isMutable = false;
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
    
   /* Validates the responseId in the SAML Response. */
    protected void validateID(String responseId) throws SAML2Exception {
        if ((responseId == null) || (responseId.length() == 0 )) {
            logger.debug("ID is missing in the SAMLResponse");
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingIDAttr"));
        }
    }
    
    
   /* Validates the version in the SAML Response. */
    protected void validateVersion(String version) throws SAML2Exception {
        if ((version == null) || (version.length() == 0) ) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
            "missingVersion"));
        } else if (!version.equals(SAML2Constants.VERSION_2_0)) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
            "incorrectVersion"));
        }
    }
    
   /* Validates the IssueInstant attribute in the SAML Response. */
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
    
    /* Validates the Status element in the SAML Response. */
    protected void validateStatus()
    throws SAML2Exception {
        if (status == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingStatus"));
        }
    }    
    
   /* Validates the required elements in the SAML Response. */
    protected void validateData() throws SAML2Exception {
        validateID(responseId);
        validateVersion(version);
        if (issueInstant == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("incorrectIssueInstant"));
        }
        validateIssueInstant(DateUtils.dateToString(issueInstant));
        validateStatus();        
    }
}
