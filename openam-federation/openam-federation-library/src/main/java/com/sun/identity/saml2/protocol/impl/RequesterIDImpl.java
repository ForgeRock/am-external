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
 * $Id: RequesterIDImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;


import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.REQUESTERID;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.RequesterID;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This interface identifies the requester entities in an 
 * <code>AuthnRequest</code> message.
 */

public class RequesterIDImpl implements RequesterID {
            
     private String requesterIdURI;
     private boolean isMutable=false;

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     */

    public RequesterIDImpl() {
	isMutable=true;
    }

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     *
     * @param element Document Element of <code>RequesterID</code> Object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */

    public RequesterIDImpl(Element element) throws SAML2Exception {
	parseElement(element);
    }

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     *
     * @param xmlString XML String Representation of <code>RequesterID</code>
     *	      object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */
    public RequesterIDImpl(String xmlString) throws SAML2Exception {
	Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString);
	if (xmlDocument == null) {
            throw new SAML2Exception(	
		    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
	}
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of the <code>RequesterID</code> URI.
     *
     * @return value of the <code>RequesterID</code> URI.
     * @see #setValue(String)
     */
    public String getValue() {
	return requesterIdURI;
    }
    
    /** 
     * Sets the value of the <code>RequesterID</code> URI.
     *
     * @param value of the <code>RequesterID<code> URI.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception {
	if (isMutable) {
	    requesterIdURI=value;
	} else {
	    throw new SAML2Exception("objectImmutable");
	}
    }

	@Override
	public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
			throws SAML2Exception {
    	DocumentFragment fragment = document.createDocumentFragment();
    	if (isNotBlank(requesterIdURI)) {
			Element requesterIdElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
					REQUESTERID, includeNSPrefix, declareNS);
			fragment.appendChild(requesterIdElement);
			requesterIdElement.setTextContent(requesterIdURI);
		}

		return fragment;
	}

    /**
     * Makes this object immutable. 
     */
    public void makeImmutable() {
	if (isMutable) {
	    isMutable=false;
	}
    }
    
    /** 
     * Returns value true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
	return isMutable;
    }

    void parseElement(Element element) {
        requesterIdURI = XMLUtils.getValueOfValueNode((Node)element);

    }
}
