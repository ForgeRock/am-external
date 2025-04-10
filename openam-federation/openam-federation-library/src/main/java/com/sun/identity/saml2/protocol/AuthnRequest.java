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
 * $Id: AuthnRequest.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;

/** 
 *  The <code>AuthnRequest</code> interface defines methods for properties
 *  required by an authentication request.
 *
 */
 @SupportedAll

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        defaultImpl = AuthnRequestImpl.class)
public interface AuthnRequest extends RequestAbstract {
    
    /** 
     * Returns the <code>Subject</code> object. 
     *
     * @return the <code>Subject</code> object. 
     * @see #setSubject(Subject)
     */
    Subject getSubject();
    
    /** 
     * Sets the <code>Subject</code> object. 
     *
     * @param subject the new <code>Subject</code> object. 
     * @throws SAML2Exception if the object is immutable.
     * @see #getSubject
     */
    void setSubject(Subject subject) throws SAML2Exception;
    
    /** 
     * Returns the <code>NameIDPolicy</code> object.
     *
     * @return the <code>NameIDPolicy</code> object. 
     * @see #setNameIDPolicy(NameIDPolicy)
     */
    NameIDPolicy getNameIDPolicy();
    
    /** 
     * Sets the <code>NameIDPolicy</code> object. 
     *
     * @param nameIDPolicy the new <code>NameIDPolicy</code> object. 
     * @throws SAML2Exception if the object is immutable.
     * @see #getNameIDPolicy
     */

    void setNameIDPolicy(NameIDPolicy nameIDPolicy)
    throws SAML2Exception;
    
    /** 
     * Returns the <code>Conditions</code> object.
     *
     * @return the <code>Conditions</code> object. 
     * @see #setConditions(Conditions)
     */
    Conditions getConditions();
    
    /** 
     * Sets the <code>Conditions</code> object. 
     *
     * @param conditions the new <code>Conditions</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getConditions
     */

    void setConditions(Conditions conditions) throws SAML2Exception;
    
    /** 
     * Returns the <code>RequestedAuthnContext</code> object. 
     *
     * @return the <code>RequestAuthnContext</code> object. 
     * @see #setRequestedAuthnContext(RequestedAuthnContext)
     */

    RequestedAuthnContext getRequestedAuthnContext();
    
    /** 
     * Sets the <code>RequestedAuthnContext</code>. 
     *
     * @param reqAuthnContext the new <code>RequestedAuthnContext</code>
     *        object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequestedAuthnContext
     */
    void setRequestedAuthnContext(RequestedAuthnContext reqAuthnContext)
    throws SAML2Exception;
    
    /** 
     * Sets the <code>Scoping</code> object. 
     *
     * @param scoping the new <code>Scoping</code> Object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getScoping
     */
    void setScoping(Scoping scoping) throws SAML2Exception;
    
    /** 
     * Returns the <code>Scoping</code> object. 
     *
     * @return the <code>Scoping</code> object. 
     * @see #setScoping(Scoping)
     */
    Scoping getScoping();
    
    /** 
     * Returns value of <code>isForceAuthn</code> attribute.
     *
     * @return value of <code>isForceAuthn</code> attribute, or null if the attribute is not present.
     */
    Boolean isForceAuthn();
    
    /** 
     * Sets the value of the <code>ForceAuthn</code> attribute.
     *
     * @param value the value of <code>ForceAuthn</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    void setForceAuthn(Boolean value)
    throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>isPassive</code> attribute.
     *
     * @return value of <code>isPassive</code> attribute, or null if the attribute is not present.
     */
    Boolean isPassive();
    
    /** 
     * Sets the value of the <code>IsPassive</code> attribute.
     *
     * @param value Value of <code>IsPassive</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    void setIsPassive(Boolean value)
    throws SAML2Exception;
    
    /** 
     * Sets the value of the <code>ProtocolBinding</code> attribute.
     *
     * @param protocolBinding value of the <code>ProtocolBinding</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProtocolBinding
     */
    void setProtocolBinding(String protocolBinding)
    throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>ProtocolBinding</code> attribute.
     *
     * @return the value of <code>ProtocolBinding</code> attribute.
     * @see #setProtocolBinding(String)
     */
    String getProtocolBinding();
    
    /** 
     * Returns the value of the <code>AssertionConsumerServiceURL</code>
     * attribute.
     *
     * @return the value of <code>AssertionConsumerServiceURL</code> attribute.
     * @see #setAssertionConsumerServiceURL(String)
     */
    String getAssertionConsumerServiceURL();
    
    /** 
     * Sets the value of the <code>AssertionConsumerServiceURL</code> 
     * attribute.
     *
     * @param url the value of <code>AssertionConsumerServiceURL</code> 
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionConsumerServiceURL
     */
    void setAssertionConsumerServiceURL(String url) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>AssertionConsumerServiceIndex</code> 
     * attribute.
     *
     * @return value of the <code>AssertionConsumerServiceIndex</code> attribute.
     * @see #setAssertionConsumerServiceIndex(Integer)
     */
    Integer getAssertionConsumerServiceIndex();
    
    /** 
     * Sets the value of the <code>AssertionConsumerServiceIndex</code> 
     * attribute.
     *
     * @param index value of the <code>AssertionConsumerServiceIndex</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionConsumerServiceIndex
     */
    void setAssertionConsumerServiceIndex(Integer index)
    throws SAML2Exception;
    

    /**
     * Returns the value of the <code>AttributeConsumingServiceIndex</code>
     * attribute.
     *
     * @return value of the <code>AttributeConsumingServiceIndex</code> attribute.
     * @see #setAttributeConsumingServiceIndex(Integer)
     */
    Integer getAttributeConsumingServiceIndex();

    /**
     * Sets the value of the <code>AttributeConsumingServiceIndex</code>
     * attribute.
     *
     * @param index value of the <code>AttributeConsumingServiceIndex</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributeConsumingServiceIndex
     */
    void setAttributeConsumingServiceIndex(Integer index)
    throws SAML2Exception;

    /** 
     * Sets the <code>ProviderName</code> attribute value.
     *
     * @param providerName value of the <code>ProviderName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProviderName
     */
    void setProviderName(String providerName) throws SAML2Exception;
    
    /** 
     * Returns the <code>ProviderName</code> attribute value.
     *
     * @return value of the <code>ProviderName</code> attribute value.
     * @see #setProviderName(String)
     */
    String getProviderName();
}
