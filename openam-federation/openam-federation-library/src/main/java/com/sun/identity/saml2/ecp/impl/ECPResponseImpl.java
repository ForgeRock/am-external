/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ECPResponseImpl.java,v 1.2 2008/06/25 05:47:47 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.ecp.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ACTOR;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_CONSUMER_SVC_URL;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.MUST_UNDERSTAND;
import static com.sun.identity.saml2.common.SAML2Constants.SOAP_ENV_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.SOAP_ENV_PREFIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.ecp.ECPResponse;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This class implements <code>ECPResponse</code> element.
 * It provides all the methods required by <code>ECPResponse</code>
 */
public class ECPResponseImpl implements ECPResponse {

    private static final Logger logger = LoggerFactory.getLogger(ECPResponseImpl.class);
    private static final String RESPONSE = "Response";
    private Boolean mustUnderstand;
    private String actor;
    private String assertionConsumerServiceURL;
    private boolean isMutable = false;

    /**
     * Constructs the <code>ECPResponse</code> Object.
     *
     */
    public ECPResponseImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     * @param element the Document Element of ECP <code>Response</code> object.
     * @throws SAML2Exception if <code>ECPResponse</code> cannot be created.
     */
    
    public ECPResponseImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>ECPResponse</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>ECPResponse</code> cannot be created.
     */
    public ECPResponseImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns value of <code>mustUnderstand</code> attribute.
     *
     * @return value of <code>mustUnderstand</code> attribute.
     */
    public Boolean isMustUnderstand() {
        return mustUnderstand;
    }
    
    /** 
     * Sets the value of the <code>mustUnderstand</code> attribute.
     *
     * @param mustUnderstand the value of <code>mustUnderstand</code>
     *     attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setMustUnderstand(Boolean mustUnderstand)
        throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.mustUnderstand = mustUnderstand;
    }

    /**
     * Returns value of <code>actor</code> attribute.
     *
     * @return value of <code>actor</code> attribute
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets the value of <code>actor</code> attribute.
     *
     * @param actor the value of <code>actor</code> attribute
     * @throws SAML2Exception if the object is immutable.
     */
    public void setActor(String actor) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.actor = actor;
    }

    /** 
     * Returns the value of the <code>AssertionConsumerServiceURL</code>
     * attribute.
     *
     * @return the value of <code>AssertionConsumerServiceURL</code> attribute.
     * @see #setAssertionConsumerServiceURL(String)
     */
    public String getAssertionConsumerServiceURL() {
        return assertionConsumerServiceURL;
    }

    /** 
     * Sets the value of the <code>AssertionConsumerServiceURL</code> 
     * attribute.
     *
     * @param url the value of <code>AssertionConsumerServiceURL</code> 
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionConsumerServiceURL
     */
    public void setAssertionConsumerServiceURL(String url)
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.assertionConsumerServiceURL = url;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element responseElement = XMLUtils.createRootElement(document, ECP_PREFIX, ECP_NAMESPACE, RESPONSE,
                includeNSPrefix, declareNS);
        fragment.appendChild(responseElement);

        responseElement.setAttribute("xmlns:soap-env", SOAP_ENV_NAMESPACE);
        responseElement.setAttribute(SOAP_ENV_PREFIX + MUST_UNDERSTAND, mustUnderstand.toString());
        responseElement.setAttribute(SOAP_ENV_PREFIX + ACTOR, actor);
        responseElement.setAttribute(ASSERTION_CONSUMER_SVC_URL, assertionConsumerServiceURL);

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
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable(){
        return isMutable;
    }

    /* Parses the NameIDPolicy Element */
    private void parseElement(Element element) throws SAML2Exception {
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.parseElement:" +
                     " Input is null.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!RESPONSE.equals(localName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.parseElement:" +
                    " element local name should be " + RESPONSE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPResponse"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!SAML2Constants.ECP_NAMESPACE.equals(namespaceURI)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.parseElement:" +
                    " element namespace should be " +
                    SAML2Constants.ECP_NAMESPACE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPNamesapce"));
        }

        String str = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.MUST_UNDERSTAND);
        mustUnderstand = SAML2SDKUtils.StringToBoolean(str);

        actor = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.ACTOR);

        assertionConsumerServiceURL = XMLUtils.getNodeAttributeValue(element,
            SAML2Constants.ASSERTION_CONSUMER_SVC_URL);

        validateData();
    }

    protected void validateData() throws SAML2Exception {
        if (assertionConsumerServiceURL == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.validateData: " +
                    "AssertionConsumerServiceURL is missing in the " +
                    "ecp:Response");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingAssertionConsumerServiceURLECPResponse"));
        }

        if (mustUnderstand == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.validateData: " +
                    "mustUnderstand is missing in the ecp:Response");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingMustUnderstandECPResponse"));
        }

        if (actor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPResponseImpl.validateData: " +
                    "actor is missing in the ecp:Response");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingActorECPResponse"));
        }
    }
}
