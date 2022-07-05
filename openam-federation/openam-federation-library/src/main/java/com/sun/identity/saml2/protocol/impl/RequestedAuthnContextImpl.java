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
 * $Id: RequestedAuthnContextImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */



package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_DECLARE_STR;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.AUTH_CONTEXT_CLASS_REF;
import static com.sun.identity.saml2.common.SAML2Constants.COMPARISON;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_DECLARE_STR;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.REQ_AUTHN_CONTEXT;
import static java.util.stream.Collectors.toList;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Java content class for RequestedAuthnContextType complex type.
 * <p>The following schema fragment specifies the expected 
 *	content contained within this java content object. 
 * <p>
 * <pre>
 * &lt;complexType name="RequestedAuthnContextType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextClassRef" maxOccurs="unbounded"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDeclRef" maxOccurs="unbounded"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="Comparison" type="{urn:oasis:names:tc:SAML:2.0:protocol}AuthnContextComparisonType" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class RequestedAuthnContextImpl implements RequestedAuthnContext {
    private static final Logger logger = LoggerFactory.getLogger(RequestedAuthnContextImpl.class);
    public final String elementName = "RequestedAuthnContext";
    private boolean mutable = false;
    private List<String> authnContextClassRef = null;
    private List<Element> authnContextDeclRef = null;
    private String comparison = null;
    
    /**
     * Constructor
     */
    public RequestedAuthnContextImpl() {
        mutable = true;
    }

    /**
     * Constructor
     *
     * @param element the Document Element.
     * @throws SAML2Exception if there is an error creating this object.
     */
    public RequestedAuthnContextImpl(Element element) throws SAML2Exception {
    	parseElement(element);
        makeImmutable();
    }

    /**
     * Constructor
     *
     * @param xmlString the <code>RequestedAuthnContext</code> XML String. 
     * @throws SAML2Exception if there is an error creating this object.
     */
    public RequestedAuthnContextImpl(String xmlString) throws SAML2Exception {
    	Document doc = XMLUtils.toDOMDocument(xmlString);
	if (doc == null) {
            logger.debug("RequestedAuthnContextImpl :"
                      + "Input is null.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
	}
	parseElement(doc.getDocumentElement());
        makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
    	String eltName = element.getLocalName();
        if (eltName == null)  {
	    if (logger.isDebugEnabled()) {
	    	logger.debug("parseElement(Element): "
                    + "local name missing");
	    }
	    throw new SAML2Exception("");
        }
        
        comparison = element.getAttribute(
            SAML2Constants.COMPARISON);

        if (!(eltName.equals(elementName)))  {
	    if (logger.isDebugEnabled()) {
	    	logger.debug("RequestedAuthnContextImpl: "
                    + "invalid element");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        
        // set AuthnContextClassRef or AuthnContextDeclRef property 
    	NodeList nl = element.getChildNodes();
    	int length = nl.getLength();

    	for(int i = 0; i < length; i++) {
    	    Node child = nl.item(i);
            String childName = child.getLocalName();

    	    if(childName == null) {
                continue;
            } 

            if(childName.equals("AuthnContextClassRef")) {
                if(authnContextDeclRef != null) {
               	    logger.error("AuthnContext(Element): Should"
                             + "contain either <AuthnContextClassRef> or "
                             + "<AuthnContextDeclRef>");
                    throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));

	        }

    	        getAuthnContextClassRef().add(
                                XMLUtils.getElementValue((Element)child));
    	    } else if (childName.equals("AuthnContextDeclRef")) {
                if(authnContextClassRef != null) {
                    logger.error("AuthnContext(Element): Should"
                            + "contain either <AuthnContextClassRef> or "
                            + "<AuthnContextDeclRef>");
                    throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
	            }
                if (authnContextDeclRef == null) {
                    authnContextDeclRef = new ArrayList<>();
                }

    	        authnContextDeclRef.add((Element) child);
            }
    	}
    }
	
    /**
     * Returns the value of AuthnContextClassRef property. 
     * 
     * @return List of String representing authentication context 
     *          class reference.
     * @see #setAuthnContextClassRef(List)
     */
    public List getAuthnContextClassRef() {
        if (authnContextClassRef == null) {
            authnContextClassRef = new ArrayList<>();
        }

    	return authnContextClassRef;
    }

    /**
     * Sets the value of AuthnContextClassRef property.
     * 
     * @param value List of String representing authentication context
     *          class referance.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getAuthnContextClassRef
     */
    public void setAuthnContextClassRef(List value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
    	}

        if ((authnContextDeclRef != null) && !authnContextDeclRef.isEmpty()) {
       	    logger.error("setAuthnContextClassRef: Should"
                    + "contain either <AuthnContextClassRef> or "
                    + "<AuthnContextDeclRef>");
           throw new SAML2Exception(
             SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        
        authnContextClassRef = value;

    	return;
    }
    
    /**
     * Returns List of String representing authentication context
     *          declaration reference.
     *
     * @return List of String representing authentication context 
     *          declaration reference.
     * @see #setAuthnContextDeclRef(List)
     */
    public List getAuthnContextDeclRef() {
        if (authnContextDeclRef != null) {
            return authnContextDeclRef.stream().map(XMLUtils::print).collect(toList());
        }

    	return new ArrayList<>();
    }

    /**
     * Sets the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @param value List of String representing authentication context
     *          declaration referance.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getAuthnContextDeclRef
     */
    public void setAuthnContextDeclRef(List value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
    	}

        if ((authnContextClassRef != null) && !authnContextClassRef.isEmpty()) {
       	    logger.error("setAuthnContextDeclRef: Should"
                    + "contain either <AuthnContextClassRef> or "
                    + "<AuthnContextDeclRef>");
           throw new SAML2Exception(
             SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        if (value != null) {
            List<Element> parsedList = new ArrayList<>(value.size());
            for (Object elem : value) {
                String xml = "<saml:AuthnContextDeclRef " + ASSERTION_DECLARE_STR + PROTOCOL_DECLARE_STR + ">" +
                        elem + "</saml:AuthnContextDeclRef>";
                Document parsed = XMLUtils.toDOMDocument(xml);
                parsedList.add(parsed.getDocumentElement());
            }
            authnContextDeclRef = parsedList;
        } else {
            authnContextDeclRef = null;
        }
    }

    /**
     * Returns the value of comparison property.
     * 
     * @return An String representing comparison method.
     * @see #setComparison(String)
     */
    public String getComparison() {
    	return comparison;
    }

    /**
     * Sets the value of the comparison property.
     * 
     * @param value An String representing comparison method.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getComparison
     */
    public void setComparison(String value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        comparison = value;
    	return;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        if (authnContextClassRef != null && authnContextDeclRef != null) {
            throw new SAML2Exception("wrongInput");
        }

        DocumentFragment fragment = document.createDocumentFragment();
        Element contextElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                REQ_AUTHN_CONTEXT, includeNSPrefix, declareNS);
        fragment.appendChild(contextElement);

        if (declareNS) {
            contextElement.setAttribute("xmlns:saml", ASSERTION_NAMESPACE_URI);
        }

        if (comparison == null) {
            comparison = "exact";
        }
        contextElement.setAttribute(COMPARISON, comparison);

        String prefix = includeNSPrefix ? ASSERTION_PREFIX : "";
        if (isNotEmpty(authnContextClassRef)) {
            for (String acr : authnContextClassRef) {
                Element acrElem = document.createElement(prefix + AUTH_CONTEXT_CLASS_REF);
                acrElem.setTextContent(acr);
                contextElement.appendChild(acrElem);
            }
        }

        if (isNotEmpty(authnContextDeclRef)) {
            for (Element decl : authnContextDeclRef) {
                contextElement.appendChild(decl);
            }
        }

        return fragment;
    }

    /**
     * Makes the obejct immutable
     */
    public void makeImmutable() {
        mutable = false;

        if(authnContextClassRef != null) {
            authnContextClassRef =
                    Collections.unmodifiableList(authnContextClassRef);
        }

        if(authnContextDeclRef != null) {
            authnContextDeclRef =
                    Collections.unmodifiableList(authnContextDeclRef);
        }

        return;
    }
    
    /**
     * Returns true if the object is mutable
     *
     * @return true if the object is mutable
     */
    public boolean isMutable() {
       return mutable;
    }
}
