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
 * $Id: StatusDetail.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2016-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.protocol.impl.StatusDetailImpl;

/**
 * This class represents the <code>StatusDetailType</code> complex type in
 * SAML protocol schema.
 * The <code>StatusDetail</code> element MAY be used to specify additional
 * information concerning the status of the request.
 *
 * <pre>
 * &lt;complexType name="StatusDetailType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;any/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = StatusDetailImpl.class)
public interface StatusDetail extends XmlSerializable {

    /**
     * Returns the value of the Any property.
     *
     * @return A list containing objects of type <code>String</code>
     * @see #setAny(List)
     */
    public List<String> getAny();

    /**
     * Sets the value of the Any property.
     *
     * @param anyList
     *        A list containing objects of type <code>String</code>
     * @throws SAML2Exception if the object is immutable
     * @see #getAny
     */
    public void setAny(List<String> anyList) throws SAML2Exception;

    /**
     * Makes the obejct immutable
     */
    public void makeImmutable();

    /**
     * Returns true if the object is mutable false otherwise
     *
     * @return true if the object is mutable false otherwise
     */
    public boolean isMutable();
}
