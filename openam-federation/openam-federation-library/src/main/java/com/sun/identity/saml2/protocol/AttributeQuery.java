/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributeQuery.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol;

import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.AttributeQueryImpl;

/**
 * This class represents the AttributeQueryType complex type.
 * <p>The following schema fragment specifies the expected
 * content contained within this java content object.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeQueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}SubjectQueryAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute" minOccurs="0" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = AttributeQueryImpl.class)
public interface AttributeQuery extends SubjectQueryAbstract {
    /**
     * Returns <code>Attribute</code> objects.
     *
     * @return the <code>Attribute</code> objects.
     * @see #setAttributes(List)
     */
    public List<Attribute> getAttributes();

    /**
     * Sets the <code>Attribute</code> objects.
     *
     * @param attributes the new <code>Attribute</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributes
     */
    public void setAttributes(List<Attribute> attributes) throws SAML2Exception;
}
