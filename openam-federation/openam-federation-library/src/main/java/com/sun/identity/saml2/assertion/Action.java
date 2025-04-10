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
 * $Id: Action.java,v 1.2 2008/06/25 05:47:39 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.assertion;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/**
 * The <code>Action</code> element specifies an action on the specified
 * resource for which permission is sought. Its type is <code>ActionType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ActionType"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="Namespace" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@SupportedAll
public interface Action extends XmlSerializable {

    /**
     * Makes the object immutable.
     */
    public void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable; <code>false</code>
     *                otherwise.
     */
    public boolean isMutable();

    /**
     * Returns the value of the <code>Action</code>.
     *
     * @return the value of this <code>Action</code>.
     * @see #setValue(String)
     */
    public String getValue();

    /**
     * Sets the value of this <code>Action</code>.
     *
     * @param value new <code>Action</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    public void setValue(String value)
        throws SAML2Exception;

    /**
     * Returns the value of <code>Namespace</code> attribute.
     *
     * @return the value of <code>Namespace</code> attribute.
     * @see #setNamespace(String)
     */
    public String getNamespace();

    /**
     * Sets the value of the <code>Namespace</code> attribute.
     *
     * @param value new value of <code>Namespace</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNamespace()
     */
    public void setNamespace(String value)
        throws SAML2Exception;

}

