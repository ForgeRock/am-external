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
 * $Id: AuthnStatementImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */



package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.SubjectLocality;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This is the default implementation of interface <code>AuthnStatement</code>.
 *
 * The <code>AuthnStatement</code> element describes a statement by the
 * SAML authority asserting that the assertion subject was authenticated
 * by a particular means at a particular time. It is of type 
 * <code>AuthnStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AuthnStatementType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *     StatementAbstractType">
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         SubjectLocality" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AuthnContext"/>
 *       &lt;/sequence&gt;
 *       &lt;attribute name="AuthnInstant" use="required" 
 *       type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="SessionIndex"
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="SessionNotOnOrAfter"
 *       type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class AuthnStatementImpl implements AuthnStatement {

    private static final Logger logger = LoggerFactory.getLogger(AuthnStatementImpl.class);
    private AuthnContext authnContext = null;
    private SubjectLocality subjectLocality = null;
    private Date authnInstant = null;
    private String sessionIndex = null;
    private Date sessionNotOnOrAfter = null;
    private boolean mutable = true;

    // verifies data according to the schema.
    private void validateData()
        throws SAML2Exception
    {
        if (authnContext == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnStatementImpl.validateData: "
                    + "missing Element AuthnContext.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingElement"));
        }
        if (authnInstant == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnStatementImpl.validateData: "
                    + "missing attribute AuthnInstant.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingAttribute"));
        }
    }

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnStatementImpl.parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an AuthnStatement.
        if (!SAML2SDKUtils.checkStatement(element, "AuthnStatement")) {
            if (logger.isDebugEnabled()) {
                logger.debug("AuthnStatementImpl.parseElement: "
                    + "not AuthnStatement.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <AuthnStatement> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            Node att = atts.getNamedItem("AuthnInstant");
            if (att != null) {
                try {
                    authnInstant =
                        DateUtils.stringToDate(((Attr)att).getValue().trim());
                } catch (ParseException pe) {
                    throw new SAML2Exception(pe.getMessage());
                }
            }
            att = atts.getNamedItem("SessionIndex");
            if (att != null) {
                sessionIndex = ((Attr)att).getValue().trim();
            }
            att = atts.getNamedItem("SessionNotOnOrAfter");
            if (att != null) {
                try {
                    sessionNotOnOrAfter =
                        DateUtils.stringToDate(((Attr)att).getValue().trim());
                } catch (ParseException pe) {
                    throw new SAML2Exception(pe.getMessage());
                }
            }
        }
        // handle the sub elementsof the AuthnStatment
        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("SubjectLocality")) {
                    if (subjectLocality != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnStatementImpl."
                                +"parseElement: included more than one Subject"
                                + "Locality.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (authnContext != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnStatementImpl."
                                +"parseElement: SubjectLocality is out of "
                                + "sequence.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    subjectLocality = AssertionFactory.getInstance().
                                createSubjectLocality((Element) child);
                } else if (childName.equals("AuthnContext")) {
                    if (authnContext != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AuthnStatementImpl."
                                +"parseElement: included more than one "
                                + "AuthnContext.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    authnContext = AssertionFactory.getInstance().
                                createAuthnContext((Element) child);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("AuthnStatementImpl.parse"
                            + "Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
                }
            }
        }

        validateData();
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AuthnStatementImpl() {
    }

    /**
     * Class constructor with <code>AuthnStatement</code> in
     * <code>Element</code> format.
     */
    public AuthnStatementImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AuthnStatement</code> in xml string format.
     */
    public AuthnStatementImpl(String xmlString)
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
     * Returns the value of the <code>AuthnContext</code> property.
     *
     * @return <code>AuthnContext</code> of the statement.
     * @see #setAuthnContext(AuthnContext)
     */
    public AuthnContext getAuthnContext() {
        return authnContext;
    }

    /**
     * Sets the value of the <code>AuthnContext</code> property.
     *
     * @param value new <code>AuthnContext</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnContext()
     */
    public void setAuthnContext(AuthnContext value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authnContext = value;
    }

    /**
     * Returns the value of the <code>AuthnInstant</code> attribute.
     *
     * @return the value of the <code>AuthnInstant</code> attribute.
     * @see #setAuthnInstant(Date)
     */
    public Date getAuthnInstant() {
        return authnInstant;
    }

    /**
     * Sets the value of the <code>AuthnInstant</code> attribute.
     *
     * @param value new value of <code>AuthnInstant</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnInstant
     */
    public void setAuthnInstant(Date value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        authnInstant = value;
    }

    /**
     * Returns the value of the <code>SubjectLocality</code> property.
     *
     * @return <code>SubjectLocality</code> of the statement.
     * @see #setSubjectLocality(SubjectLocality)
     */
    public SubjectLocality getSubjectLocality() {
        return subjectLocality;
    }

    /**
     * Sets the value of the <code>SubjectLocality</code> property.
     *
     * @param value the new value of <code>SubjectLocality</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSubjectLocality()
     */
    public void setSubjectLocality(SubjectLocality value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        subjectLocality = value;
    }

    /**
     * Returns the value of the <code>SessionIndex</code> attribute.
     *
     * @return the value of the <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * Sets the value of the <code>SessionIndex</code> attribute.
     *
     * @param value new value of <code>SessionIndex</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionIndex()
     */
    public void setSessionIndex(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        sessionIndex = value;
    }

    /**
     * Returns the value of the <code>SessionNotOnOrAfter</code> attribute.
     *
     * @return the value of <code>SessionNotOnOrAfter</code> attribute.
     * @see #setSessionNotOnOrAfter(Date)
     */
    public Date getSessionNotOnOrAfter() {
        return sessionNotOnOrAfter;
    }

    /**
     * Sets the value of the <code>SessionNotOnOrAfter</code> attribute.
     *
     * @param value new <code>SessionNotOnOrAfter</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionNotOnOrAfter()
     */
    public void setSessionNotOnOrAfter(Date value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        sessionNotOnOrAfter = value;
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        if (mutable) {
            if (subjectLocality != null) {
                subjectLocality.makeImmutable();
            }
            if (authnContext != null) {
                authnContext.makeImmutable();
            }
            mutable = false;
        }
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

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        DocumentFragment fragment = document.createDocumentFragment();
        Element authnStatementElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX,
                ASSERTION_NAMESPACE_URI, "AuthnStatement", includeNSPrefix, declareNS);
        fragment.appendChild(authnStatementElement);

        authnStatementElement.setAttribute("AuthnInstant", DateUtils.toUTCDateFormat(authnInstant));
        if (isNotBlank(sessionIndex)) {
            authnStatementElement.setAttribute("SessionIndex", sessionIndex);
        }
        if (sessionNotOnOrAfter != null) {
            authnStatementElement.setAttribute("SessionNotOnOrAfter", DateUtils.toUTCDateFormat(sessionNotOnOrAfter));
        }
        if (subjectLocality != null) {
            authnStatementElement.appendChild(subjectLocality.toDocumentFragment(document, includeNSPrefix, false));
        }
        authnStatementElement.appendChild(authnContext.toDocumentFragment(document, includeNSPrefix, false));

        return fragment;
    }
}
