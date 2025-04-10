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
 * $Id: Response.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */



package com.sun.identity.saml2.protocol;

import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;

/**
 * The <code>Response</code> message element is used when a response consists
 * of a list of zero or more assertions that satisfy the request. It has the
 * complex type <code>ResponseType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Assertion"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedAssertion"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = ResponseImpl.class)
public interface Response extends StatusResponse {

    /**
     * Returns <code>Assertion</code>(s) of the response.
     *
     * @return List of <code>Assertion</code>(s) in the response.
     * @see #setAssertion(List)
     */
    public List<Assertion> getAssertion();

    /**
     * Sets Assertion(s) of the response.
     *
     * @param value List of new <code>Assertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertion()
     */
    public void setAssertion(List<Assertion> value)
	throws SAML2Exception;

    /**
     * Returns <code>EncryptedAssertion</code>(s) of the response.
     *
     * @return List of <code>EncryptedAssertion</code>(s) in the response.
     * @see #setEncryptedAssertion(List)
     */
    public List<EncryptedAssertion> getEncryptedAssertion();

    /**
     * Sets <code>EncryptedAssertion</code>(s) of the response.
     *
     * @param value List of new <code>EncryptedAssertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAssertion()
     */
    public void setEncryptedAssertion(List<EncryptedAssertion> value)
	throws SAML2Exception;

}
