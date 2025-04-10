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
 * $Id: NewIDImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.protocol.NewID;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.xmlenc.EncManager;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This class identifies the new ID entitie in an 
 * <code>ManageNameIDRequest</code> message.
 */
public class NewIDImpl implements NewID {
    public final String elementName = "NewID";
    private String newID;

    /**
     * Constructor to create the <code>NewID</code> Object.
     *
     * @param element Document Element of <code>NewID</code> Object.
     * @throws SAML2Exception if <code>NewID<code> cannot be created.
     */

    public NewIDImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructor to create the <code>NewID</code> Object.
     *
     * @param value of the <code>NewID<code>.
     * @throws SAML2Exception if <code>NewID<code> cannot be created.
     */
    public NewIDImpl(String value) throws SAML2Exception {
        newID = value;	
    }

    /** 
     * Returns the value of the <code>NewID</code> URI.
     *
     * @return value of the <code>NewID</code> URI.
     * @see #NewIDImpl(String)
     */
    public String getValue() {
        return newID;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        if (isNotBlank(newID)) {
            Element newIDElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                    elementName, includeNSPrefix, declareNS);
            fragment.appendChild(newIDElement);
            newIDElement.setTextContent(newID);
        }
        return fragment;
    }

    @Override
    public NewEncryptedID encrypt(EncryptionConfig encryptionConfig, String recipientEntityID) throws SAML2Exception {
        Element el = EncManager.getEncInstance().encrypt(toXMLString(true, true),
                encryptionConfig, recipientEntityID, "NewEncryptedID");
        return ProtocolFactory.getInstance().createNewEncryptedID(el);
    }
    
    void parseElement(Element element) {
        newID = XMLUtils.getValueOfValueNode((Node)element);
    }
}
