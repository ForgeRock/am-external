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
 * $Id: Status.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.protocol.impl.StatusImpl;

/**
 * This class represents the <code>StatusType</code> complex type in
 * SAML protocol schema.
 *
 * <pre>
 * &lt;complexType name="StatusType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusCode"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusMessage" minOccurs="0"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusDetail" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = StatusImpl.class)
public interface Status extends XmlSerializable {
    
    /**
     * Returns the value of the statusCode property.
     *
     * @return the value of the statusCode property
     * @see #setStatusCode(StatusCode)
     */
    public com.sun.identity.saml2.protocol.StatusCode getStatusCode();
    
    /**
     * Sets the value of the statusCode property.
     *
     * @param value the value of the statusCode property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusCode
     */
    public void setStatusCode(com.sun.identity.saml2.protocol.StatusCode value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the statusMessage property.
     *
     * @return the value of the statusMessage property
     * @see #setStatusMessage(String)
     */
    public java.lang.String getStatusMessage();
    
    /**
     * Sets the value of the statusMessage property.
     *
     * @param value the value of the statusMessage property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusMessage
     */
    public void setStatusMessage(java.lang.String value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the statusDetail property.
     *
     * @return the value of the statusDetail property
     * @see #setStatusDetail(StatusDetail)
     */
    public com.sun.identity.saml2.protocol.StatusDetail getStatusDetail();
    
    /**
     * Sets the value of the statusDetail property.
     *
     * @param value the value of the statusDetail property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusDetail
     */
    public void setStatusDetail(
    com.sun.identity.saml2.protocol.StatusDetail value)
    throws SAML2Exception;

    /**
     * Makes the obejct immutable
     */
    public void makeImmutable();
    
    /**
     * Returns true if the object is mutable, false otherwise
     *
     * @return true if the object is mutable, false otherwise
     */
    public boolean isMutable();
}
