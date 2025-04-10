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
 * $Id: AttributeStatementImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This is a default implementation of <code>AttributeStatement</code>.
 *
 * The <code>AttributeStatement</code> element describes a statement by
 * the SAML authority asserting that the assertion subject is associated with
 * the specified attributes. It is of type <code>AttributeStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeStatementType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *       StatementAbstractType">
 *       &lt;choice maxOccurs="unbounded"&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         EncryptedAttribute"/>
 *       &lt;/choice&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class AttributeStatementImpl implements AttributeStatement {

    private static final Logger logger = LoggerFactory.getLogger(AttributeStatementImpl.class);
    private List<Attribute> attrs = null;
    private List<EncryptedAttribute> encAttrs = null;
    private boolean mutable = true;

    // Validate the object according to the schema.
    private void validateData() throws SAML2Exception {

        if ((attrs == null || attrs.isEmpty()) &&
            (encAttrs == null || encAttrs.isEmpty()))
        {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeStatementImpl."
                    + "validateData: missing Attribute or"
                    + " EncryptedAttribute element.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingElement"));
        }
    }

    // used by the constructors.
    private void parseElement(Element element) throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeStatementImpl." 
                    + "parseElement: Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an AttributeStatement.
        if (!SAML2SDKUtils.checkStatement(element, "AttributeStatement")) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeStatementImpl." 
                    +"parseElement: not AttributeStatement.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the sub elementsof the AuthnStatment
        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Attribute")) {
                    Attribute attr = AssertionFactory.getInstance().
                        createAttribute((Element) child);
                    if (attrs == null) {
                        attrs = new ArrayList();
                    }
                    attrs.add(attr);
                } else if (childName.equals("EncryptedAttribute")) {
                    EncryptedAttribute encAttr = AssertionFactory.getInstance().
                        createEncryptedAttribute((Element) child);
                    if (encAttrs == null) {
                        encAttrs = new ArrayList();
                    }
                    encAttrs.add(encAttr);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("AttributeStatementImpl."
                            + "parse Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
                }
            }
        }
        validateData();
        if (attrs != null) {
            attrs = Collections.unmodifiableList(attrs);
        }
        if (encAttrs != null) {
            encAttrs = Collections.unmodifiableList(encAttrs);
        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AttributeStatementImpl() {
    }

    /**
     * Class constructor with <code>AttributeStatement</code> in
     * <code>Element</code> format.
     */
    public AttributeStatementImpl(org.w3c.dom.Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AttributeStatement</code> in xml string
     * format.
     */
    public AttributeStatementImpl(String xmlString) throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Returns <code>Attribute</code>(s) of the statement. 
     *
     * @return List of <code>Attribute</code>(s) in the statement.
     * @see #setAttribute(List)
     */
    @Override
    public List<Attribute> getAttribute() {
        return attrs;
    }

    /**
     * Sets <code>Attribute</code>(s) of the statement.
     *
     * @param value List of new <code>Attribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttribute()
     */
    @Override
    public void setAttribute(List<Attribute> value) throws SAML2Exception {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        attrs = value;
    }

    /**
     * Returns <code>EncryptedAttribute</code>(s) of the statement. 
     *
     * @return List of <code>EncryptedAttribute</code>(s) in the statement.
     * @see #setEncryptedAttribute(List)
     */
    @Override
    public List<EncryptedAttribute> getEncryptedAttribute() {
        return encAttrs;
    }

    /**
     * Sets <code>EncryptedAttribute</code>(s) of the statement.
     *
     * @param value List of new <code>EncryptedAttribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAttribute()
     */
    @Override
    public void setEncryptedAttribute(List value) throws SAML2Exception {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        encAttrs = value;
    }

    /**
     * Makes the object immutable.
     */
    @Override
    public void makeImmutable() {
        if (mutable) {
            if (attrs != null) {
                Iterator iter = attrs.iterator();
                while (iter.hasNext()) {
                    Attribute attr = (Attribute) iter.next();
                    attr.makeImmutable();
                }
                attrs = Collections.unmodifiableList(attrs);
            }
            if (encAttrs != null) {
                encAttrs = Collections.unmodifiableList(encAttrs);
            }
            mutable = false;
        }
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *          <code>false</code> otherwise.
     */
    @Override
    public boolean isMutable() {
        return mutable;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element statementElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                "AttributeStatement", includeNSPrefix, declareNS);
        fragment.appendChild(statementElement);

        if (attrs != null) {
            for (Attribute attribute : attrs) {
                statementElement.appendChild(attribute.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (encAttrs != null) {
            for (EncryptedAttribute encryptedAttribute : encAttrs) {
                statementElement.appendChild(encryptedAttribute.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }
}
