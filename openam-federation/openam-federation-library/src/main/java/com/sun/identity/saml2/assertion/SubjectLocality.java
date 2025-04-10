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
 * $Id: SubjectLocality.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */



package com.sun.identity.saml2.assertion;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.SubjectLocalityImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/**
 * The <code>SubjectLocality</code> element specifies the DNS domain name
 * and IP address for the system entity that performed the authentication.
 * It exists as part of <code>AuthenticationStatement</code> element.
 * <p>
 * <pre>
 * &lt;complexType name="SubjectLocalityType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="Address"
 *       type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="DNSName"
 *       type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll
@JsonDeserialize(as=SubjectLocalityImpl.class)
public interface SubjectLocality extends XmlSerializable {

    /**
     * Makes the object immutable.
     */
    void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *                <code>false</code> otherwise.
     */
    boolean isMutable();

    /**
     * Returns the value of the <code>DNSName</code> attribute.
          *
     * @return the value of the <code>DNSName</code> attribute.
     * @see #setDNSName(String)
     */
    String getDNSName();

    /**
     * Sets the value of the <code>DNSName</code> attribute.
     *
     * @param value new value of the <code>DNSName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDNSName()
     */
    void setDNSName(String value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>Address</code> attribute.
     *
     * @return the value of the <code>Address</code> attribute.
     * @see #setAddress(String)
     */
    String getAddress();

    /**
     * Sets the value of the <code>Address</code> attribute.
     *
     * @param value new value of <code>Address</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAddress()
     */
    void setAddress(String value)
        throws SAML2Exception;
}
