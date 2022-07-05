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
 * $Id: RequestAbstract.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */
package com.sun.identity.saml2.protocol;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.saml2.crypto.signing.SigningConfig;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/** 
 * This interface defines methods for setting and retrieving attributes and 
 * elements associated with a SAML request message used in SAML protocols.
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS)
public interface RequestAbstract extends XmlSerializable {
    
    /** 
     * Sets the <code>Issuer</code> object.
     *
     * @param nameID the new <code>Issuer</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssuer
     */
    public void setIssuer(Issuer nameID) throws SAML2Exception;
    
    /** 
     * Returns the <code>Issuer</code> Object.
     *
     * @return the <code>Issuer</code> object.
     * @see #setIssuer(Issuer)
     */
    public com.sun.identity.saml2.assertion.Issuer getIssuer();
    
    /** 
     * Returns the <code>Signature</code> Object as a string.
     *
     * @return the <code>Signature</code> object as a string.
     */
    public String getSignature();   
   
   /**
     * Signs the Request.
     *
     * @param signingConfig The signing configuration.
     * @throws SAML2Exception if it could not sign the Request.
     */
    void sign(SigningConfig signingConfig) throws SAML2Exception;
    
    /** 
     * Sets the <code>Extensions</code> Object.
     *
     * @param extensions the <code>Extensions</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getExtensions
     */
    public void setExtensions(Extensions extensions) throws SAML2Exception;
    
    /** 
     * Returns the <code>Extensions</code> Object.
     *
     * @return the <code>Extensions</code> object.
     * @see #setExtensions(Extensions)
     */
    public Extensions getExtensions();
    
    /** 
     * Sets the value of the <code>ID</code> attribute.
     *
     * @param id the new value of <code>ID</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getID
     */
    public void setID(String id) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>ID</code> attribute.
     *
     * @return the value of <code>ID</code> attribute.
     * @see #setID(String)
     */
    public String getID();
    
    /** 
     * Sets the value of the <code>Version</code> attribute.
     *
     * @param version the value of <code>Version</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getVersion
     */
    public void setVersion(String version) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Version</code> attribute.
     *
     * @return value of <code>Version</code> attribute.
     * @see #setVersion(String)
     */
    String getVersion();
    
    /** 
     * Sets the value of <code>IssueInstant</code> attribute.
     *
     * @param dateTime new value of the <code>IssueInstant</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIssueInstant	 
     */
    public void setIssueInstant(Date dateTime) throws SAML2Exception;
    
    /** 
     * Returns the value of <code>IssueInstant</code> attribute.
     *
     * @return value of the <code>IssueInstant</code> attribute.
     * @see #setIssueInstant(Date)
     */
    public java.util.Date getIssueInstant();
    
    /** 
     * Sets the value of the <code>Destination</code> attribute.
     *
     * @param destinationURI new value of <code>Destination</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDestination
     */
    public void setDestination(String destinationURI) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Destination</code> attribute.
     *
     * @return  the value of <code>Destination</code> attribute.
     * @see #setDestination(String)
     */
    public String getDestination();
    
    /** 
     * Sets the value of the <code>Consent</code> attribute.
     *
     * @param consent new value of <code>Consent</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getConsent
     */
    public void setConsent(String consent) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>Consent</code> attribute.
     *
     * @return value of <code>Consent</code> attribute.
     * @see #setConsent(String)
     */
    public String getConsent();
    
    
    /** 
     * Returns true if message is signed.
     *
     * @return true if message is signed. 
     */
    
    public boolean isSigned();
    
    
    /**
     * Return whether the signature is valid or not.
     *
     * @param verificationCerts Certificates containing the public keys which may be used for signature verification;
     *                          This certificate may also may be used to check against the certificate included in the
     *                          signature.
     * @return true if the signature is valid; false otherwise.
     * @throws SAML2Exception if the signature could not be verified
     */
    public boolean isSignatureValid(Set<X509Certificate> verificationCerts) throws SAML2Exception;
    
        
    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() ;
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
