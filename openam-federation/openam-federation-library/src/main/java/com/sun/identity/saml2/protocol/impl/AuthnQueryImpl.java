/*
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
 * $Id: AuthnQueryImpl.java,v 1.3 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.SESSION_INDEX;

import java.util.ListIterator;

import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.AuthnQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.shared.xml.XMLUtils;

public class AuthnQueryImpl extends SubjectQueryAbstractImpl
    implements AuthnQuery {

    protected RequestedAuthnContext requestedAuthnContext;
    protected String sessionIndex;

    /**
     * Constructor to create <code>AuthnQuery</code> Object .
     */
    public AuthnQueryImpl() {
        super(SAML2Constants.AUTHN_QUERY);
        isMutable = true;
    }

    /**
     * Constructor to create <code>AuthnQuery</code> Object.
     *
     * @param element the Document Element Object.
     * @throws SAML2Exception if error creating <code>AuthnQuery</code> 
     *     Object. 
     */
    public AuthnQueryImpl(Element element) throws SAML2Exception {
        super(SAML2Constants.AUTHN_QUERY);
        parseDOMElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Constructor to create <code>AuthnQuery</code> Object.
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>AuthnQuery</code> 
     *     Object. 
     */
    public AuthnQueryImpl(String xmlString) throws SAML2Exception {
        super(SAML2Constants.AUTHN_QUERY);
        Document xmlDocument = 
            XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseDOMElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * @return the <code>RequestedAuthnContext</code> object.
     * @see #setRequestedAuthnContext(RequestedAuthnContext)
     */
    public RequestedAuthnContext getRequestedAuthnContext()
    {
        return requestedAuthnContext;
    }
  
    /**
     * Sets the <code>RequestedAuthnContext</code> object.
     *
     * @param requestedAuthnContext the new <code>RequestedAuthnContext</code>
     *     object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequestedAuthnContext
     */
    public void setRequestedAuthnContext(
        RequestedAuthnContext requestedAuthnContext) throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.requestedAuthnContext = requestedAuthnContext;
    }

    /**
     * Returns the value of the <code>SessionIndex</code> attribute.
     *
     * @return value of <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * Sets the value of <code>SessionIndex</code> attribute.
     *
     * @param sessionIndex new value of the <code>SessionIndex</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionIndex
     */
    public void setSessionIndex(String sessionIndex) throws SAML2Exception{
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.sessionIndex = sessionIndex;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);
        if (isSigned && signedXMLString != null) {
            return fragment;
        }

        Element rootElement = (Element) fragment.getFirstChild();
        if (declareNS) {
            rootElement.setAttribute("xmlns:saml", ASSERTION_NAMESPACE_URI);
        }

        if (StringUtils.isNotBlank(sessionIndex)) {
            rootElement.setAttribute(SESSION_INDEX, sessionIndex);
        }

        if (requestedAuthnContext != null) {
            rootElement.appendChild(requestedAuthnContext.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        return fragment;
    }

    /**
     * Parses attributes of the Docuemnt Element for this object.
     * 
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        super.parseDOMAttributes(element);
        sessionIndex = element.getAttribute(SAML2Constants.SESSION_INDEX);
    }

    /** 
     * Parses child elements of the Docuemnt Element for this object.
     * 
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {

        super.parseDOMChileElements(iter);

        ProtocolFactory pFactory = ProtocolFactory.getInstance();
        if(iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.REQ_AUTHN_CONTEXT.equals(localName)) {
                requestedAuthnContext =
                    pFactory.createRequestedAuthnContext(childElement);
            } else {
                iter.previous();
            }
        }
    }
}
