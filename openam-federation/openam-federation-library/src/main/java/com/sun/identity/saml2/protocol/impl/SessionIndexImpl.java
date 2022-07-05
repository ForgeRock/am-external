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
 * $Id: SessionIndexImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.SESSION_INDEX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.SessionIndex;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class defines methods for adding <code>SessionIndex</code> element.
 */

public class SessionIndexImpl implements SessionIndex {
    
    private String sessionValue = null;
    
    /**
     * Constructs the <code>SessionIndex</code> Object.
     *
     * @param value A String <code>SessionIndex</code> value
     */
    public SessionIndexImpl(String value) {
        this.sessionValue = value;
    }
    
    /**
     * Returns the <code>SessionIndex</code> value.
     *
     * @return A String value of the <code>SessionIndex</code>
     *
     */
    public java.lang.String getValue() {
        return sessionValue;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        if (isNotBlank(sessionValue)) {
            Element sessionIndexElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                    SESSION_INDEX, includeNSPrefix, declareNS);
            fragment.appendChild(sessionIndexElement);
            sessionIndexElement.setTextContent(sessionValue);
        }

        return fragment;
    }
}
