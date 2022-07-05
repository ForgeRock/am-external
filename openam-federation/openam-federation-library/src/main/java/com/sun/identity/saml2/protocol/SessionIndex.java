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
 * $Id: SessionIndex.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2016-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.protocol.impl.SessionIndexImpl;

/**
 * This class represents the <code>SessionIndex</code> element in
 * SAML protocol schema.
 * The <code>SessionIndex</code> element specifies a session index value
 * of a authenticated principal.
 *
 * <pre>
 * &lt;element name="SessionIndex" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = SessionIndexImpl.class)
public interface SessionIndex extends XmlSerializable {
    
    /**
     * Returns the <code>SessionIndex</code> value.
     *
     * @return A String value of the <code>SessionIndex</code>
     *
     */
    public java.lang.String getValue();
}
