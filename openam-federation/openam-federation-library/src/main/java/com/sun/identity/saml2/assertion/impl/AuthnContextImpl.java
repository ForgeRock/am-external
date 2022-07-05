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
 * $Id: AuthnContextImpl.java,v 1.3 2008/06/25 05:47:43 qcheng Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */
package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

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

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;



/**
 * This is the default implementation of interface <code>AuthnContext</code>.
 *
 * The <code>AuthnContext</code> element specifies the context of an
 * authentication event. The element can contain an authentication context
 * class reference, an authentication declaration or declaration reference,
 * or both. Its type is <code>AuthnContextType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AuthnContextType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;sequence&gt;
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextClassRef"/>
 *             &lt;choice minOccurs="0"&gt;
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *               AuthnContextDecl"/>
 *               &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *               AuthnContextDeclRef"/>
 *             &lt;/choice&gt;
 *           &lt;/sequence&gt;
 *           &lt;choice&gt;
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextDecl"/>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *             AuthnContextDeclRef"/>
 *           &lt;/choice&gt;
 *         &lt;/choice&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AuthenticatingAuthority" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class AuthnContextImpl implements AuthnContext {

    private static final Logger logger = LoggerFactory.getLogger(AuthnContextImpl.class);
    private String authnContextClassRef = null;
    private String authnContextDecl = null;
    private String authnContextDeclRef = null;
    private List<String> authenticatingAuthority = null;
    private boolean mutable = true;

    // verify the data according to the schema.
    private void validateData() throws SAML2Exception {
        if ((authnContextClassRef == null ||
                authnContextClassRef.trim().length() == 0) &&
            (authnContextDecl == null ||
                authnContextDecl.trim().length() == 0) &&
            (authnContextDeclRef == null ||
                authnContextDeclRef.trim().length() == 0))
        {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnContextImpl.validateData: "
                    + "missing AuthnContextClassRef or AuthnContextDecl(Ref).");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingElement"));
        }
        
        if (authnContextDecl != null && authnContextDecl.trim().length() != 0
                && authnContextDeclRef != null
                && authnContextDeclRef.trim().length() != 0)
        {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnContextImpl.validateData: "
                    + "AuthnContextDecl and AuthnContextDeclRef cannot "
                    + "present the same time.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    // used by the constructors.
    private void parseElement(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnContextImpl.parseElement:" 
                    +" Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an AuthnContext.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("AuthnContext"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnContextImpl.parseElement:"
                    + " not AuthnContext.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("AuthnContextClassRef")) {
                    if (authnContextClassRef != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl.parse"
                                + "Element: included more than one AuthnContext"
                                + "ClassRef.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (authnContextDecl != null ||
                        authnContextDeclRef != null ||
                        authenticatingAuthority != null)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl.parse"
                                + "Element: AuthnContextClassRef should be "
                                + "the first child element.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    authnContextClassRef =
                        XMLUtils.getElementValue((Element)child);
                    if (authnContextClassRef == null ||
                        authnContextClassRef.trim().length() == 0)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl."
                                +"parseElement: value for AuthnContextClassRef "
                                + "is empty.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.
                                getString("missingElementValue"));
                    }
                } else if (childName.equals("AuthnContextDecl")) {
                    if (authnContextDecl != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl.parse"
                                + "Element: included more than one AuthnContext"
                                + "Decl.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    authnContextDecl = XMLUtils.print(child);
                    if (authnContextDecl == null ||
                        authnContextDecl.trim().length() == 0)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl."
                                +"parseElement: value for AuthnContextDecl "
                                + "is empty.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.
                                getString("missingElementValue"));
                    }
                } else if (childName.equals("AuthnContextDeclRef")) {
                    if (authnContextDeclRef != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl.parse"
                                + "Element: included more than one AuthnContext"
                                + "DeclRef.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    authnContextDeclRef =
                        XMLUtils.getElementValue((Element)child);
                    if (authnContextDeclRef == null ||
                        authnContextDeclRef.trim().length() == 0)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl."
                                +"parseElement: value for AuthnContextDeclRef "
                                + "is empty.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.
                                getString("missingElementValue"));
                    }
                } else if (childName.equals("AuthenticatingAuthority")) {
                    String authority = XMLUtils.getElementValue((Element)child);
                    if (authority == null || authority.trim().length() == 0) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnContextImpl."
                                +"parseElement: value for"
                                +" AuthenticatingAuthority is empty.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.
                                getString("missingElementValue"));
                    }
                    if (authenticatingAuthority == null) {
                        authenticatingAuthority = new ArrayList<String>();
                    }
                    authenticatingAuthority.add(authority);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("AuthnContextImpl."
                            +"parseElement: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
                }
            }
        }

        // Commenting this until we get a fix from WSIT
        // validateData();
        if (authenticatingAuthority != null) {
            authenticatingAuthority =
                Collections.unmodifiableList(authenticatingAuthority);
        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AuthnContextImpl() {
    }

    /**
     * Class constructor with <code>AuthnContext</code> in <code>Element</code>
     * format.
     */
    public AuthnContextImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AuthnContext</code> in xml string format.
     */
    public AuthnContextImpl(String xmlString)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        if (mutable && (authenticatingAuthority != null)) {
            authenticatingAuthority =
                    Collections.unmodifiableList(authenticatingAuthority);
        }
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *                <code>false</code> otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns the value of the <code>AuthnContextClassRef</code> property.
     *
     * @return the value of the <code>AuthnContextClassRef</code>.
     */
    public java.lang.String getAuthnContextClassRef() {
        return authnContextClassRef;
    }

    /**
     * Sets the value of the <code>AuthnContextClassRef</code> property.
     *
     * @param value new <code>AuthenticationContextClassRef</code>.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *                if the object is immutable.
     */
    public void setAuthnContextClassRef(java.lang.String value)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authnContextClassRef = value;
    }

    /**
     * Returns the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @return A String representing authentication context
     *                 declaration reference.
     */
    public java.lang.String getAuthnContextDeclRef() {
        return authnContextDeclRef;
    }

    /**
     * Sets the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @param value A String representation of authentication context
     *                declaration reference.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *                if the object is immutable.
     */
    public void setAuthnContextDeclRef(java.lang.String value)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authnContextDeclRef = value;
    }

    /**
     * Returns the value of the <code>AuthnContextDecl</code> property.
     *
     * @return An XML String representing authentication context declaration.
     */
    public java.lang.String getAuthnContextDecl() {
        return authnContextDecl;
    }

    /**
     * Sets the value of the <code>AuthnContextDecl</code> property.
     *
     * @param value An XML String representing authentication context
     *                declaration.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *                if the object is immutable.
     */
    public void setAuthnContextDecl(java.lang.String value)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authnContextDecl = value;
    }

    /**
     * Sets the value of the <code>AuthenticatingAuthority</code> property.
     *
     * @param value List of Strings representing authenticating authority.
     * @throws SAML2Exception If the object is immutable.
     */
    public void setAuthenticatingAuthority(List<String> value) throws SAML2Exception {
        if (!mutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authenticatingAuthority = value;
    }

    /**
     * Returns the value of the <code>AuthenticatingAuthority</code> property.
     *
     * @return List of Strings representing
     *                <code>AuthenticatingAuthority</code>.
     */
    public List<String> getAuthenticatingAuthority() {
        return authenticatingAuthority;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element authnContextElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                "AuthnContext", includeNSPrefix, declareNS);
        fragment.appendChild(authnContextElement);

        String prefix = includeNSPrefix ? ASSERTION_PREFIX : "";
        if (isNotBlank(authnContextClassRef)) {
            Element accr = document.createElement(prefix + "AuthnContextClassRef");
            accr.setTextContent(authnContextClassRef);
            authnContextElement.appendChild(accr);
        }
        if (isNotBlank(authnContextDecl)) {
            Document parsedDecl = XMLUtils.toDOMDocument(authnContextDecl);
            if (parsedDecl != null) {
                authnContextElement.appendChild(document.adoptNode(parsedDecl.getDocumentElement()));
            } else {
                logger.error("AuthnContextImpl.toDocumentFragment(): AuthnContext declaration is malformed: {}",
                        authnContextDecl);
            }
        } else if (isNotBlank(authnContextDeclRef)) {
            Element declRef = document.createElement(prefix + "AuthnContextDeclRef");
            declRef.setTextContent(authnContextDeclRef);
            authnContextElement.appendChild(declRef);
        }
        if (authenticatingAuthority != null) {
            for (String authority : authenticatingAuthority) {
                if (isNotBlank(authority)) {
                    Element authorityElement = document.createElement(prefix + "AuthenticatingAuthority");
                    authorityElement.setTextContent(authority);
                    authnContextElement.appendChild(authorityElement);
                }
            }
        }

        return fragment;
    }
}
