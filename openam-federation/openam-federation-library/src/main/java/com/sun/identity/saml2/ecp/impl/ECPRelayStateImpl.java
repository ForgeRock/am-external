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
 * $Id: ECPRelayStateImpl.java,v 1.2 2008/06/25 05:47:46 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.ecp.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ACTOR;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.MUST_UNDERSTAND;
import static com.sun.identity.saml2.common.SAML2Constants.RELAY_STATE;
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
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This class implements <code>ECPRelayState</code> element.
 * It provides all the methods required by <code>ECPRelayState</code>
 */
public class ECPRelayStateImpl implements ECPRelayState {

    private static final Logger logger = LoggerFactory.getLogger(ECPRelayStateImpl.class);
    private String value;
    private Boolean mustUnderstand;
    private String actor;
    private boolean isMutable = false;

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     */
    public ECPRelayStateImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     * @param element the Document Element of ECP <code>RelayState</code>
     *     object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    
    public ECPRelayStateImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    public ECPRelayStateImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of the <code>RelayState</code>.
     *
     * @return value of the <code>RelayState</code>.
     * @see #setValue(String)
     */
    public String getValue() {
        return value;
    }
    
    /** 
     * Sets the value of the <code>RelayState</code>.
     *
     * @param value new value of the <code>RelayState</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception {
        if (isMutable) {
            this.value = value;
        } else {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
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

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element relayStateElement = XMLUtils.createRootElement(document, ECP_PREFIX, ECP_NAMESPACE, RELAY_STATE,
                includeNSPrefix, declareNS);
        fragment.appendChild(relayStateElement);

        relayStateElement.setAttribute("xmlns:soap-env", SOAP_ENV_NAMESPACE);
        relayStateElement.setAttribute(SOAP_ENV_PREFIX + MUST_UNDERSTAND, mustUnderstand.toString());
        relayStateElement.setAttribute(SOAP_ENV_PREFIX + ACTOR, actor);

        relayStateElement.setTextContent(value);

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
                logger.debug("ECPRelayStateImpl.parseElement:" +
                     " Input is null.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!SAML2Constants.RELAY_STATE.equals(localName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRelayStateImpl.parseElement:" +
                    " element local name should be " +
                    SAML2Constants.RELAY_STATE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPRelayState"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!SAML2Constants.ECP_NAMESPACE.equals(namespaceURI)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRelayStateImpl.parseElement:" +
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

        value = XMLUtils.getElementValue(element);

        validateData();
    }

    protected void validateData() throws SAML2Exception {
        if (value == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRelayStateImpl.validateData: " +
                    "value is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("missingECPRelayState"));
        }

        if (mustUnderstand == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRelayStateImpl.validateData: " +
                    "mustUnderstand is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingMustUnderstandECPRelayState"));
        }

        if (actor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRelayStateImpl.validateData: " +
                    "actor is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingActorECPRelayState"));
        }
    }
}
