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
 * $Id: ResponseImpl.java,v 1.4 2009/12/16 05:26:39 ericow Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.protocol.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This is an implementation of interface <code>Response</code>.
 *
 * The <code>Response</code> message element is used when a response consists
 * of a list of zero or more assertions that satisfy the request. It has the
 * complex type <code>ResponseType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Assertion"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedAssertion"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class ResponseImpl extends StatusResponseImpl implements Response {

    private static final Logger logger = LoggerFactory.getLogger(ResponseImpl.class);
    private List<Assertion> assertions = null;
    private List<EncryptedAssertion> encAssertions = null;

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public ResponseImpl() {
        super(SAML2Constants.RESPONSE);
        isMutable = true;
    }

    /**
     * Class constructor with <code>Response</code> in
     * <code>Element</code> format.
     *
     * @param element the Document Element.
     * @throws SAML2Exception if there is an error.
     */
    public ResponseImpl(Element element) throws SAML2Exception {
        super(SAML2Constants.RESPONSE);
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element, "UTF-8");
        }
    }

    /**
     * Class constructor with <code>Response</code> in xml string format.
     *
     * @param xmlString the Response String..
     * @throws SAML2Exception if there is an error.
     */
    public ResponseImpl(String xmlString) throws SAML2Exception {
        super(SAML2Constants.RESPONSE);
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Returns <code>Assertion</code>(s) of the response.
     *
     * @return List of <code>Assertion</code>(s) in the response.
     * @see #setAssertion(List)
     */
    public List<Assertion> getAssertion() {
        return assertions;
    }

    /**
     * Sets Assertion(s) of the response.
     *
     * @param value List of new <code>Assertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertion()
     */
    public void setAssertion(List<Assertion> value) throws SAML2Exception {
        if (isMutable) {
            this.assertions = value;
        } else {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Returns <code>EncryptedAssertion</code>(s) of the response.
     *
     * @return List of <code>EncryptedAssertion</code>(s) in the response.
     * @see #setEncryptedAssertion(List)
     */
    public List<EncryptedAssertion> getEncryptedAssertion() {
        return encAssertions;
    }

    /**
     * Sets <code>EncryptedAssertion</code>(s) of the response.
     *
     * @param value List of new <code>EncryptedAssertion</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAssertion()
     */
    public void setEncryptedAssertion(List<EncryptedAssertion> value) throws SAML2Exception {
        if (isMutable) {
            this.encAssertions = value;
        } else {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            if (assertions != null) {
                for (Assertion assertion : assertions) {
                    assertion.makeImmutable();
                }
                assertions = Collections.unmodifiableList(assertions);
            }
            if (encAssertions != null) {
                encAssertions = Collections.unmodifiableList(encAssertions);
            }
            super.makeImmutable();
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);
        if (isSigned && signedXMLString != null) {
            return fragment;
        }

        Element rootElement = (Element) fragment.getFirstChild();

        if (assertions != null) {
            for (Assertion assertion : assertions) {
                rootElement.appendChild(assertion.toDocumentFragment(document, includeNSPrefix, declareNS));
            }
        }

        if (encAssertions != null) {
            for (EncryptedAssertion encryptedAssertion : encAssertions) {
                rootElement.appendChild(encryptedAssertion.toDocumentFragment(document, includeNSPrefix, declareNS));
            }
        }

        return fragment;
    }

    private void parseElement(Element element) throws SAML2Exception {
        verifyElementIsNotNull(element);
        verifyElementIsResponse(element);
        handleElementAttributes(element);
        handleChildElements(element);
        super.validateData();
        if (assertions != null) {
            for (Assertion assertion : assertions) {
                assertion.makeImmutable();
            }
            assertions = Collections.unmodifiableList(assertions);
        }
        if (encAssertions != null) {
            encAssertions = Collections.unmodifiableList(encAssertions);
        }
        isMutable = false;
    }

    private void verifyElementIsNotNull(Element element) throws SAML2Exception {
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ResponseImpl.parseElement: element input is null.");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("nullInput"));
        }
    }

    private void verifyElementIsResponse(Element element) throws SAML2Exception {
        String tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("Response"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("ResponseImpl.parseElement: "
                        + "not Response.");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("wrongInput"));
        }
    }

    private void handleElementAttributes(Node element) throws SAML2Exception {
        NamedNodeMap atts = element.getAttributes();
        if (atts != null) {
            for (int i = 0; i < atts.getLength(); i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                switch (attrName) {
                case "ID":
                    responseId = attrValue;
                    break;
                case "InResponseTo":
                    inResponseTo = attrValue;
                    break;
                case "Version":
                    version = attrValue;
                    break;
                case "IssueInstant":
                    try {
                        issueInstant = DateUtils.stringToDate(attrValue);
                    } catch (ParseException pe) {
                        throw new SAML2Exception(pe.getMessage());
                    }
                    break;
                case "Destination":
                    destination = attrValue;
                    break;
                case "Consent":
                    consent = attrValue;
                    break;
                }
            }
        }
    }

    private void handleChildElements(Element element) throws SAML2Exception {
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            Node child = nl.item(i);
            String childName = child.getLocalName();
            if (childName != null) {
                switch (childName) {
                case "Issuer" -> {
                    if (issuer != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element: included more than one Issuer.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (signatureString != null || extensions != null || status != null || assertions != null ||
                            encAssertions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:wrong sequence.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    issuer = AssertionFactory.getInstance().createIssuer((Element) child);
                }
                case "Signature" -> {
                    if (signatureString != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:included more than one Signature.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (extensions != null || status != null || assertions != null || encAssertions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:wrong sequence.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    signatureString = XMLUtils.print(child,"UTF-8");
                    isSigned = true;
                }
                case "Extensions" -> {
                    if (extensions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:included more than one Extensions.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (status != null || assertions != null || encAssertions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:wrong sequence.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    extensions = ProtocolFactory.getInstance().createExtensions((Element) child);
                }
                case "Status" -> {
                    if (status != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element: included more than one Status.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (assertions != null || encAssertions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse Element:wrong sequence.");
                        }
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    status = ProtocolFactory.getInstance().createStatus((Element) child);
                }
                case "Assertion" -> {
                    if (assertions == null) {
                        assertions = new ArrayList<>();
                    }
                    Element canoEle = SAMLUtils.getCanonicalElement(child);
                    if (canoEle == null) {
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorCanonical"));
                    }
                    assertions.add(AssertionFactory.getInstance().createAssertion(canoEle));
                }
                case "EncryptedAssertion" -> {
                    if (encAssertions == null) {
                        encAssertions = new ArrayList<>();
                    }
                    encAssertions.add(AssertionFactory.getInstance().createEncryptedAssertion((Element) child));
                }
                default -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ResponseImpl.parse Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("invalidElement"));
                }
                }
            }
        }
    }
}
