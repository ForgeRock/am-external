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
 * $Id: AssertionIDRef.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 * Portions Copyrighted 2018-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.AssertionIDRefImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/**
 * This class represents the AssertionIDRef element.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;element name="AssertionIDRef" type="NCName"/&gt;
 * </pre>
 *
 */
@SupportedAll
@JsonDeserialize(as=AssertionIDRefImpl.class)
public interface AssertionIDRef extends XmlSerializable {
    
    /**
     * Returns the value of the <code>AssertionIDRef</code>.
     *
     * @return the value of this <code>AssertionIDRef</code>.
     * @see #setValue(String)
     */
    public String getValue();

    /**
     * Sets the value of this <code>AssertionIDRef</code>.
     *
     * @param value new <code>AssertionIDRef</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    void setValue(String value) throws SAML2Exception;
    
    /**
     * Makes the object immutable.
     */
    void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable; <code>false</code>
     *     otherwise.
     */
    boolean isMutable();
}
