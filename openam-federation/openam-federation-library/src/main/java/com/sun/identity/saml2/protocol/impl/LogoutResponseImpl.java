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
 * $Id: LogoutResponseImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class implements the <code>LogoutResponse</code> element in
 * SAML protocol schema.
 * It provides all the methods required by <code>LogoutResponse</code>
 */

public class LogoutResponseImpl extends StatusResponseImpl
implements LogoutResponse {
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     */
    public LogoutResponseImpl() {
        super(SAML2Constants.LOGOUT_RESPONSE);
        isMutable = true;
    }
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     * @param element the Document Element of <code>LogoutResponse</code> object.
     * @throws SAML2Exception if <code>LogoutResponse</code> cannot be created.
     */
    
    public LogoutResponseImpl(Element element) throws SAML2Exception {
        super(SAML2Constants.LOGOUT_RESPONSE);
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>LogoutResponse</code> cannot be created.
     */
    public LogoutResponseImpl(String xmlString) throws SAML2Exception {
        super(SAML2Constants.LOGOUT_RESPONSE);
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }
    
    /**
     * Makes this object immutable.
     *
     */
    public void makeImmutable() {
        super.makeImmutable();
    }
    
    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /**
     * Parses the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    private void parseElement(Element element) throws SAML2Exception {
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        responseId = element.getAttribute(SAML2Constants.ID);
        validateID(responseId);
        
        version = element.getAttribute(SAML2Constants.VERSION);
        validateVersion(version);
        
        String issueInstantStr = element.getAttribute(
        SAML2Constants.ISSUE_INSTANT);
        validateIssueInstant(issueInstantStr);
        
        destination = element.getAttribute(SAML2Constants.DESTINATION);
        consent = element.getAttribute(SAML2Constants.CONSENT);
        inResponseTo = element.getAttribute(SAML2Constants.INRESPONSETO);
        
        NodeList nList = element.getChildNodes();
        
        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
                    if (cName.equals(SAML2Constants.ISSUER)) {
                        issuer =
                        assertionFactory.createIssuer((Element)childNode);
                    } else if (cName.equals(SAML2Constants.SIGNATURE)) {
                        signatureString=
                        XMLUtils.getElementString((Element)childNode);
                        isSigned = true; 
                    } else if (cName.equals(SAML2Constants .EXTENSIONS)) {
                        extensions =
                        protoFactory.createExtensions((Element)childNode);
                    } else if (cName.equals(SAML2Constants.STATUS)) {
                        status =
                        protoFactory.createStatus((Element)childNode);
                        validateStatus();
                    }
                }
            }
        }
    }
    
}
