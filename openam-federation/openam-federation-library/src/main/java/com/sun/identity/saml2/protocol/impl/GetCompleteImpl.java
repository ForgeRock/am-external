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
 * $Id: GetCompleteImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.GETCOMPLETE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.GetComplete;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This class contains methods for the <code>GetComplete</code> 
 * Element in the SAMLv2 Protocol Schema. 
 * <code>GetComplete</code> Element specifies a URI which resolves to 
 * the complete IDPList.
 */
public class GetCompleteImpl implements GetComplete {

    private String getCompleteURI;
    private boolean isMutable=false;

    /**
     *  Constructor creates <code>GetComplete</code> object 
     */
    public GetCompleteImpl() {
	isMutable=true;
    }

    /**
     * Constructor creates <code>GetComplete</code> object 
     *
     * @param element the Document Element object.
     * @throws SAML2Exception if error creating <code>GetComplete</code> object.
     */
    public GetCompleteImpl(Element element) throws SAML2Exception {
	parseElement(element);
    }


    /**
     * Constructor creates <code>GetComplete</code> object 
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>GetComplete</code> object.
     */
    public GetCompleteImpl(String xmlString) throws SAML2Exception {
	Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString);
	if (xmlDocument == null) {
            throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("errorObtainingElement"));
	}
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of the <code>GetComplete</code> URI.
     *
     * @return value of the <code>GetComplete</code> URI.
     * @see #setValue(String)
     */
    public String getValue() {
	return getCompleteURI;
    }
    
    /** 
     * Sets the value of the <code>GetComplete<code> URI.
     *
     * @param value new value of the <code>GetComplete<code> URI.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception {
	if (isMutable) {
	    getCompleteURI = value;
	} else {
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element getCompleteElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                GETCOMPLETE, includeNSPrefix, declareNS);
        fragment.appendChild(getCompleteElement);
        getCompleteElement.setTextContent(getCompleteURI);
        return fragment;
    }

    /**
     * Makes this object immutable. 
     *
     */
    public void makeImmutable()  {
	if (isMutable) {
	    isMutable=false;
	}
    }
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
	return isMutable;
    }

    /* Parse the GetComplete element. */
    void parseElement(Element element) {
	getCompleteURI = XMLUtils.getValueOfValueNode((Node)element);
    }
}
