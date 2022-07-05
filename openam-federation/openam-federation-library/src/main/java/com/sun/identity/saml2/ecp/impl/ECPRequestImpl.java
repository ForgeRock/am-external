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
 * $Id: ECPRequestImpl.java,v 1.2 2008/06/25 05:47:47 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.ecp.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ACTOR;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.ECP_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.ISPASSIVE;
import static com.sun.identity.saml2.common.SAML2Constants.MUST_UNDERSTAND;
import static com.sun.identity.saml2.common.SAML2Constants.PROVIDER_NAME;
import static com.sun.identity.saml2.common.SAML2Constants.SOAP_ENV_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.SOAP_ENV_PREFIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.ecp.ECPRequest;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * This class implements <code>ECPRequest</code> element.
 * It provides all the methods required by <code>ECPRequest</code>
 */
public class ECPRequestImpl implements ECPRequest {

    private static final Logger logger = LoggerFactory.getLogger(ECPRequestImpl.class);
    private static final String REQUEST = "Request";
    private Issuer issuer;
    private IDPList idpList;
    private Boolean mustUnderstand;
    private String actor;
    private String providerName;
    private Boolean isPassive;
    private boolean isMutable = false;

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     */
    public ECPRequestImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     * @param element the Document Element of ECP <code>Request</code> object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequestImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequestImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the value of the issuer attribute.
     *
     * @return the value of the issuer attribute.
     * @see #setIssuer(Issuer)
     */
    public Issuer getIssuer() {
        return issuer;
    }

    /**
     * Sets the value of the issuer attribute.
     *
     * @param issuer the value of the issuer attribute
     * @throws SAML2Exception if the object is immutable
     * @see #getIssuer
     */
    public void setIssuer(Issuer issuer) throws SAML2Exception {
        if (isMutable) {
            this.issuer = issuer;
        } else {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /** 
     * Returns the <code>IDPList</code> Object.
     *
     * @return the <code>IDPList</code> object.
     * @see #setIDPList(IDPList)
     */
    public IDPList getIDPList() {
        return idpList;
    }
    
    /** 
     * Sets the <code>IDPList</code> Object.
     *
     * @param idpList the new <code>IDPList</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPList
     */
    public void setIDPList(IDPList idpList) throws SAML2Exception {
        if (isMutable) {
            this.idpList = idpList;
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

    /** 
     * Returns the <code>ProviderName</code> attribute value.
     *
     * @return value of the <code>ProviderName</code> attribute value.
     * @see #setProviderName(String)
     */
    public String getProviderName() {
        return providerName;
    }

    /** 
     * Sets the <code>ProviderName</code> attribute value.
     *
     * @param providerName value of the <code>ProviderName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProviderName
     */
    public void setProviderName(String providerName) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.providerName = providerName;
    }

    /** 
     * Returns the value of the <code>isPassive</code> attribute.
     *
     * @return value of <code>isPassive</code> attribute.
     */
    public Boolean isPassive() {
        return isPassive;
    }

    /** 
     * Sets the value of the <code>IsPassive</code> attribute.
     *
     * @param isPassive value of <code>IsPassive</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setIsPassive(Boolean isPassive) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.isPassive = isPassive;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element requestElement = XMLUtils.createRootElement(document, ECP_PREFIX, ECP_NAMESPACE, REQUEST,
                includeNSPrefix, declareNS);
        fragment.appendChild(requestElement);

        requestElement.setAttribute("xmlns:soap-env", SOAP_ENV_NAMESPACE);
        requestElement.setAttribute(SOAP_ENV_PREFIX + MUST_UNDERSTAND, mustUnderstand.toString());
        requestElement.setAttribute(SOAP_ENV_PREFIX + ACTOR, actor);

        if (providerName != null) {
            requestElement.setAttribute(PROVIDER_NAME, providerName);
        }

        if (isPassive != null) {
            requestElement.setAttribute(ISPASSIVE, isPassive.toString());
        }

        requestElement.appendChild(issuer.toDocumentFragment(document, includeNSPrefix, declareNS));

        if (idpList != null) {
            requestElement.appendChild(idpList.toDocumentFragment(document, includeNSPrefix, declareNS));
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
                logger.debug("ECPRequestImpl.parseElement:" +
                     " Input is null.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!REQUEST.equals(localName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.parseElement:" +
                    " element local name should be " + REQUEST);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPRequest"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!ECP_NAMESPACE.equals(namespaceURI)) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.parseElement:" +
                    " element namespace should be " +
                    ECP_NAMESPACE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPNamesapce"));
        }

        String str = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.MUST_UNDERSTAND);
        mustUnderstand = SAML2SDKUtils.StringToBoolean(str);

        actor = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.ACTOR);

        providerName = XMLUtils.getNodeAttributeValue(element,
            SAML2Constants.PROVIDER_NAME);

        str = XMLUtils.getNodeAttributeValue(element, SAML2Constants.ISPASSIVE);
        isPassive = SAML2SDKUtils.StringToBoolean(str);

        NodeList nList = element.getChildNodes();
        if ((nList !=null) && (nList.getLength() >0)) {
            for(int i=0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String cName = childNode.getLocalName() ;
                if (cName.equals(SAML2Constants.ISSUER)) {
                    validateIssuer(); 
                    issuer = AssertionFactory.getInstance().createIssuer(
                        (Element)childNode);
                } else if (cName.equals(SAML2Constants.IDPLIST)) {
                    validateIDPList();
                    idpList = ProtocolFactory.getInstance().createIDPList(
                        (Element) childNode);                   
                } else {
                     if (logger.isDebugEnabled()) {
                         logger.debug(
                             "ECPRequestImpl.parseElement: " +
                             "ECP Request has invalid child element");
                     }
                     throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                         "invalidElementECPReq"));
                }
            }

        }
        validateData();
    }

    private void validateIssuer() throws SAML2Exception {
        if (issuer != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateIssuer: " +
                    "ECP Request has too many Issuer Element");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqTooManyIssuer"));
        }
        if (idpList != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateIssuer: " +
                    "Issuer should be first child element in ECP Request");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqIssuerNotFirst"));
        }
    }

    private void validateIDPList() throws SAML2Exception {
        if (idpList != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateIssuer: " +
                    "ECP Request has too many IDPList Element");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqTooManyIDPList"));
        }
    }

    protected void validateData() throws SAML2Exception {
        if (mustUnderstand == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateData: " +
                    "mustUnderstand is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingMustUnderstandECPRequest"));
        }

        if (actor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateData: " +
                    "actor is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingActorECPRequest"));
        }

        if (issuer == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ECPRequestImpl.validateData: " +
                    "Issuer is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingIssuerECPRequest"));
        }
    }
}
