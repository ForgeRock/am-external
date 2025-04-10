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
 * $Id: StatusCodeImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions copyright 2014-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.STATUS_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.VALUE;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class defines methods for <code>StatusCode</code> element.
 */

public class StatusCodeImpl implements StatusCode {
    
    private static final Logger logger = LoggerFactory.getLogger(StatusCodeImpl.class);
    private boolean isMutable = false;
    private StatusCode statusCode = null;
    private String statusCodeValue = null;
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     */
    public StatusCodeImpl() {
        isMutable=true;
    }
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     *
     * @param element the Document Element of <code>StatusCode</code> object.
     * @throws SAML2Exception if <code>StatusCode</code> cannot be created.
     */
    
    public StatusCodeImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>StatusCode</code> cannot be created.
     */
    public StatusCodeImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }
    
    /**
     * Returns the value of the statusCode property.
     *
     * @return the value of the statusCode property
     * @see #setStatusCode(StatusCode)
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }
    
    /**
     * Sets the value of the statusCode property.
     *
     * @param value the value of the statusCode property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusCode
     */
    public void setStatusCode(StatusCode value) throws SAML2Exception {
        if (isMutable) {
            this.statusCode = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the status code value of the value property.
     *
     * @return the value of the value property
     * @see #setValue(String)
     */
    public java.lang.String getValue() {
        return statusCodeValue;
    }
    
    /**
     * Sets the status code value of the value property.
     *
     * @param value the value of the value property to be set
     * @exception SAML2Exception if the object is immutable
     * @see #getValue
     */
    public void setValue(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.statusCodeValue = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        if (isNotBlank(statusCodeValue)) {
            Element codeElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE, STATUS_CODE,
                    includeNSPrefix, declareNS);
            fragment.appendChild(codeElement);

            codeElement.setAttribute(VALUE, statusCodeValue);
            if (statusCode != null) {
                codeElement.appendChild(statusCode.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            if ((statusCode != null) && (statusCode.isMutable())) {
                statusCode.makeImmutable();
            }
            isMutable = false;
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
    
    /* Parses the <code>StatusCode</code> Element. */
    private void parseElement(Element element) throws SAML2Exception {
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        statusCodeValue = element.getAttribute(SAML2Constants.VALUE);
        validateStatusCodeValue(statusCodeValue);
        
        NodeList nList = element.getChildNodes();
        if ((nList != null) && (nList.getLength() > 0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName();
                if (cName != null) {
                    if (cName.equals(SAML2Constants.STATUS_CODE)) {
                        statusCode =
                        protoFactory.createStatusCode((Element)childNode);
                    }
                }
            }
        }
    }
    
    /* validates the required attribute Value */
    void validateStatusCodeValue(String statusCodeValue) throws SAML2Exception {
        if ((statusCodeValue == null) || (statusCodeValue.length() == 0)) {
            logger.debug("statusCodeValue is required");
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingStatusCodeValue"));
        }
    }
}
