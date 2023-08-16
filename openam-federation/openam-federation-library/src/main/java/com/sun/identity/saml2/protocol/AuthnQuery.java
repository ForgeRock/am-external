/**
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
 * $Id: AuthnQuery.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016-2019 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.AuthnQueryImpl;

/**
 * This class represents the AuthnQueryType complex type.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;complexType name="AuthnQueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}SubjectQueryAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}RequestedAuthnContext" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="SessionIndex" type="string" use="optional"/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = AuthnQueryImpl.class)
public interface AuthnQuery extends SubjectQueryAbstract {

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * @return the <code>RequestedAuthnContext</code> object.
     * @see #setRequestedAuthnContext(RequestedAuthnContext)
     */
    public RequestedAuthnContext getRequestedAuthnContext();
    
    /**
     * Sets the <code>RequestedAuthnContext</code> object.
     *
     * @param requestedAuthnContext the new <code>RequestedAuthnContext</code>
     *     object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequestedAuthnContext
     */
    public void setRequestedAuthnContext(
        RequestedAuthnContext requestedAuthnContext) throws SAML2Exception;

    /**
     * Returns the value of the <code>SessionIndex</code> attribute.
     *
     * @return value of <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex();

    /**
     * Sets the value of <code>SessionIndex</code> attribute.
     *
     * @param sessionIndex new value of the <code>SessionIndex</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionIndex
     */
    public void setSessionIndex(String sessionIndex) throws SAML2Exception;

}