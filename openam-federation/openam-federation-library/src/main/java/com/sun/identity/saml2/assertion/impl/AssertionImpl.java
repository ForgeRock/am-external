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
 * $Id: AssertionImpl.java,v 1.8 2009/05/09 15:43:59 mallas Exp $
 *
 * Portions Copyrighted 2015-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static java.util.stream.Collectors.toList;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.AuthzDecisionStatement;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.xmlenc.EncManager;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>Assertion</code> element is a package of information
 * that supplies one or more <code>Statement</code> made by an issuer. 
 * There are three kinds of assertions: Authentication, Authorization Decision, 
 * and Attribute assertions.
 */

public class AssertionImpl implements Assertion {
    private static final Logger logger = LoggerFactory.getLogger(AssertionImpl.class);
    private String version;
    private Date issueInstant;
    private Subject subject;
    private Advice advice;
    private Node signature;
    private Conditions conditions;
    private String id;
    private List<Element> statements = new ArrayList<>();
    private List<AuthnStatement> authnStatements = new ArrayList<>();
    private List<AuthzDecisionStatement> authzDecisionStatements = new ArrayList<>();
    private List<AttributeStatement> attributeStatements = new ArrayList<>();
    private Issuer issuer;
    private boolean isMutable = true;
    private String signedXMLString = null;
    private Boolean isSignatureValid = null;

    public static String ASSERTION_ELEMENT = "Assertion";
    public static String ASSERTION_VERSION_ATTR = "Version";
    public static String ASSERTION_ID_ATTR = "ID";
    public static String ASSERTION_ISSUEINSTANT_ATTR = "IssueInstant";
    public static String XSI_TYPE_ATTR = "xsi:type";
    public static String ASSERTION_ISSUER = "Issuer";
    public static String ASSERTION_SIGNATURE = "Signature";
    public static String ASSERTION_SUBJECT = "Subject";
    public static String ASSERTION_CONDITIONS = "Conditions";
    public static String ASSERTION_ADVICE = "Advice";
    public static String ASSERTION_STATEMENT = "Statement";
    public static String ASSERTION_AUTHNSTATEMENT = "AuthnStatement";
    public static String ASSERTION_AUTHZDECISIONSTATEMENT =
                                                "AuthzDecisionStatement";
    public static String ASSERTION_ATTRIBUTESTATEMENT =
                                         "AttributeStatement";

   /** 
    * Default constructor
    */
    public AssertionImpl() {
    }

    /**
     * This constructor is used to build <code>Assertion</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Assertion</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public AssertionImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "AssertionImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
        if (signature != null) {
            signedXMLString = xml;
        }   
    }

    /**
     * This constructor is used to build <code>Assertion</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Assertion</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public AssertionImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
        if (signature != null) {
            signedXMLString = XMLUtils.print(element,"UTF-8");
        }
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            logger.error(
                "AssertionImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            logger.error(
                "AssertionImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(ASSERTION_ELEMENT)) {
            logger.error(
                "AssertionImpl.processElement(): invalid local name " +
                 elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(ASSERTION_VERSION_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            logger.error(
                "AssertionImpl.processElement(): version missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_version"));
        } 
        version = attrValue;
       
        attrValue = element.getAttribute(ASSERTION_ID_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            logger.error(
                "AssertionImpl.processElement(): assertion id missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_id"));
        } 
        id = attrValue; 
     
        attrValue = element.getAttribute(ASSERTION_ISSUEINSTANT_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            logger.error(
                "AssertionImpl.processElement(): issue instant missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_issue_instant"));
        } 
        try {
            issueInstant = DateUtils.stringToDate(attrValue);   
        } catch (ParseException pe) {
            logger.error(
                "AssertionImpl.processElement(): invalid issue instant");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_date_format"));
        } 

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            logger.error(
                "AssertionImpl.processElement(): assertion has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
   
        AssertionFactory factory = AssertionFactory.getInstance();
        int nextElem = 0;
        Node child = (Node)nodes.item(nextElem);
        while (child.getNodeType() != Node.ELEMENT_NODE) {
            if (++nextElem >= numOfNodes) {
                logger.error("AssertionImpl.processElement():"
                    + " assertion has no subelements");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "missing_subelements"));
            }
            child = (Node)nodes.item(nextElem);
        }

        // The first subelement should be <Issuer>
        String childName = child.getLocalName();
        if ((childName == null) || (!childName.equals(ASSERTION_ISSUER))) {
            logger.error("AssertionImpl.processElement():"+
                                     " the first element is not <Issuer>");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelement_issuer"));
        }
        issuer = factory.getInstance().createIssuer((Element)child);
        
        if (++nextElem >= numOfNodes) {
            return;
        }
        child = (Node)nodes.item(nextElem);
        while (child.getNodeType() != Node.ELEMENT_NODE) {
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
        }

        // The next subelement may be <ds:Signature>
        childName = child.getLocalName();
        if ((childName != null) &&
            childName.equals(ASSERTION_SIGNATURE)) {
            signature = (Element) child;
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            signature = null;
        }
      
        // The next subelement may be <Subject>
        if ((childName != null) && 
            childName.equals(ASSERTION_SUBJECT)) {
            subject = factory.createSubject((Element)child);
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            subject = null;
        }

        // The next subelement may be <Conditions>
        if ((childName != null) && 
            childName.equals(ASSERTION_CONDITIONS)) {
            conditions = factory.createConditions((Element)child);
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            conditions = null;
        }
     
        // The next subelement may be <Advice>
        if ((childName != null) && 
            childName.equals(ASSERTION_ADVICE)) {
            advice = factory.createAdvice((Element)child);
            nextElem++;
        } else {
            advice = null;
        }
   
        // The next subelements are all statements    
        while (nextElem < numOfNodes) { 
            child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(ASSERTION_AUTHNSTATEMENT)) {
                        authnStatements.add(
                            factory.createAuthnStatement((Element)child));
                    } else if (childName.equals(
                        ASSERTION_AUTHZDECISIONSTATEMENT)) {
                        authzDecisionStatements.add(factory.
                            createAuthzDecisionStatement((Element)child));
                    } else if (childName.equals(
                        ASSERTION_ATTRIBUTESTATEMENT)) {
                        attributeStatements.add(factory.
                            createAttributeStatement((Element)child)); 
                    } else if ((childName != null) &&
                        childName.equals(ASSERTION_SIGNATURE)) {
                        signature = (Element) child;
                    } else {
                        String type = ((Element)child).getAttribute(
                            XSI_TYPE_ATTR);
                        if (childName.equals(ASSERTION_STATEMENT) && 
                                       (type != null && type.length() > 0)) {
                            statements.add((Element) child);
                        } else {
                            logger.error(
                                "AssertionImpl.processElement(): " + 
                                "unexpected subelement " + childName);
                            throw new SAML2Exception(SAML2SDKUtils.bundle.
                                getString("unexpected_subelement"));
                        }
                    }
                }
            }
            nextElem++;
        }
             
    }

    /**
     * Returns the version number of the assertion.
     *
     * @return The version number of the assertion.
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version number of the assertion.
     *
     * @param version the version number.
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setVersion(String version) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.version = version;
    }

    /**
     * Returns the time when the assertion was issued
     *
     * @return the time of the assertion issued
     */
    @Override
    public Date getIssueInstant() {
        return issueInstant;
    }

    /**
     * Set the time when the assertion was issued
     *
     * @param issueInstant the issue time of the assertion
     * @exception SAML2Exception if the object is immutable
    */
    @Override
    public void setIssueInstant(Date issueInstant) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.issueInstant = issueInstant;
    }
   
    /**
     * Returns the subject of the assertion
     *
     * @return the subject of the assertion
     */
    @Override
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the subject of the assertion
     *
     * @param subject the subject of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setSubject(Subject subject) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.subject = subject;
    }

    /**
     * Returns the advice of the assertion
     *
     * @return the advice of the assertion
     */
    @Override
    public Advice getAdvice() {
        return advice;
    }

    /**
     * Sets the advice of the assertion
     *
     * @param advice the advice of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setAdvice(Advice advice) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.advice = advice;
    }

    /**
     * Returns the signature of the assertion
     *
     * @return the signature of the assertion
     */
    @Override
    public String getSignature() {
        return XMLUtils.print(signature);
    }

    /**
     * Returns the conditions of the assertion
     *
     * @return the conditions of the assertion
     */
    @Override
    public Conditions getConditions() {
        return conditions;
    }

    /**
     * Sets the conditions of the assertion
     *
     * @param conditions the conditions of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setConditions(Conditions conditions) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.conditions = conditions;
    }

    /**
     * Returns the id of the assertion
     *
     * @return the id of the assertion
     */
    @Override
    public String getID() {
        return id;
    }

    /**
     * Sets the id of the assertion
     *
     * @param id the id of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setID(String id) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.id = id;
    }

    /**
     * Returns the statements of the assertion
     *
     * @return the statements of the assertion
     */
    @Override
    public List<Object> getStatements() {
        if (statements != null) {
            return statements.stream().map(XMLUtils::print).collect(toList());
        }
        return null;
    }

    /**
     * Returns the Authn statements of the assertion
     *
     * @return the Authn statements of the assertion
     */
    @Override
    public List<AuthnStatement> getAuthnStatements() {
        return authnStatements;
    }

    /**
     * Returns the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @return the <code>AuthzDecisionStatements</code> of the assertion
     */
    @Override
    public List<AuthzDecisionStatement> getAuthzDecisionStatements() {
        return authzDecisionStatements;
    }

    /**
     * Returns the attribute statements of the assertion
     *
     * @return the attribute statements of the assertion
     */
    @Override
    public List<AttributeStatement> getAttributeStatements() {
        return attributeStatements;
    }

    /**
     * Sets the statements of the assertion
     *
     * @param statements the statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setStatements(List<Object> statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        if (statements != null) {
            List<Element> parsedStatements = new ArrayList<>(statements.size());
            for (Object statement : statements) {
                Document parsed = XMLUtils.toDOMDocument((String) statement);
                parsedStatements.add(parsed.getDocumentElement());
            }
            this.statements = parsedStatements;
        } else {
            this.statements = null;
        }
    }

    /**
     * Sets the <code>AuthnStatements</code> of the assertion
     *
     * @param statements the <code>AuthnStatements</code> of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setAuthnStatements(List<AuthnStatement> statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        authnStatements = statements;
    }

    /**
     * Sets the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @param statements the <code>AuthzDecisionStatements</code> of
     * the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setAuthzDecisionStatements(List<AuthzDecisionStatement> statements)
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        authzDecisionStatements = statements;
    }

    /**
     * Sets the attribute statements of the assertion
     *
     * @param statements the attribute statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setAttributeStatements(List<AttributeStatement> statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        attributeStatements = statements;
    }

    /**
     * Returns the issuer of the assertion
     *
     * @return the issuer of the assertion
     */
    @Override
    public Issuer getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer of the assertion
     *
     * @param issuer the issuer of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    @Override
    public void setIssuer(Issuer issuer) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.issuer = issuer;
    }

    /**
     * Return whether the assertion is signed 
     *
     * @return true if the assertion is signed; false otherwise.
     */
    @Override
    public boolean isSigned() {
        return (signature != null);
    }

    @Override
    public boolean isSignatureValid(Set<X509Certificate> verificationCerts)
    throws SAML2Exception {

        if (isSignatureValid == null) {            
            if (signedXMLString == null) {
                signedXMLString = toXMLString(true, true);
            }
            isSignatureValid = SigManager.getSigInstance().verify(signedXMLString, getID(), verificationCerts);
        }
        return isSignatureValid.booleanValue();
    }

    @Override
    public void sign(SigningConfig signingConfig) throws SAML2Exception {
        Element signatureElement = SigManager.getSigInstance().sign(toXMLString(true, true), getID(), signingConfig);
        signature = signatureElement;
        signedXMLString = XMLUtils.print(signatureElement.getOwnerDocument().getDocumentElement(), "UTF-8");
        makeImmutable();  
    }

    @Override
    public EncryptedAssertion encrypt(EncryptionConfig encryptionConfig, String recipientEntityID)
            throws SAML2Exception {
        Element el = EncManager.getEncInstance().encrypt(toXMLString(true, true),
                encryptionConfig, recipientEntityID, "EncryptedAssertion");
        return AssertionFactory.getInstance().createEncryptedAssertion(el);
    }

    /**
     * Gets the validity of the assertion evaluating its conditions if
     * specified.
     *
     * @return false if conditions is invalid based on it lying between
     *         <code>NotBefore</code> (current time inclusive) and
     *         <code>NotOnOrAfter</code> (current time exclusive) values 
     *         and true otherwise or if no conditions specified.
     */
    @Override
    public boolean isTimeValid() {
        if (conditions == null)  {
            return true;
        }
        else  {
            return conditions.checkDateValidity(currentTimeMillis());
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        if (!isMutable && isSigned()) {
            Document parsedDocument = XMLUtils.toDOMDocument(signedXMLString);
            fragment.appendChild(document.adoptNode(parsedDocument.getDocumentElement()));
            return fragment;
        }
        Element assertionElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                ASSERTION_ELEMENT, includeNSPrefix, declareNS);
        fragment.appendChild(assertionElement);

        if (isBlank(version)) {
            logger.error("AssertionImpl.toDocumentFragment(): version missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missing_assertion_version"));
        }
        assertionElement.setAttribute(ASSERTION_VERSION_ATTR, version);
        if (isBlank(id)) {
            logger.error("AssertionImpl.toDocumentFragment(): assertion id missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missing_assertion_id"));
        }
        assertionElement.setAttribute(ASSERTION_ID_ATTR, id);
        if (issueInstant == null) {
            logger.error("AssertionImpl.toDocumentFragment(): issue instant missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missing_issue_instant"));
        }
        assertionElement.setAttribute(ASSERTION_ISSUEINSTANT_ATTR, DateUtils.toUTCDateFormat(issueInstant));
        if (issuer == null) {
            logger.error("AssertionImpl.toDocumentFragment(): issuer missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missing_subelement_issuer"));
        }
        assertionElement.appendChild(issuer.toDocumentFragment(document, includeNSPrefix, false));

        if (signature != null) {
            assertionElement.appendChild(document.importNode(signature, true));
        }
        if (subject != null) {
            assertionElement.appendChild(subject.toDocumentFragment(document, includeNSPrefix, false));
        }
        if (conditions != null) {
            assertionElement.appendChild(conditions.toDocumentFragment(document, includeNSPrefix, false));
        }
        if (advice != null) {
            assertionElement.appendChild(advice.toDocumentFragment(document, includeNSPrefix, false));
        }
        if (statements != null) {
            for (Element statement : statements) {
                assertionElement.appendChild(document.importNode(statement, true));
            }
        }
        if (authnStatements != null) {
            for (AuthnStatement statement : authnStatements) {
                assertionElement.appendChild(statement.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (authzDecisionStatements != null) {
            for (AuthzDecisionStatement statement : authzDecisionStatements) {
                assertionElement.appendChild(statement.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (attributeStatements != null) {
            for (AttributeStatement statement : attributeStatements) {
                assertionElement.appendChild(statement.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }

    /**
    * Makes the object immutable
    */
   @Override
    public void makeImmutable() {
        if (isMutable) {
            if (authnStatements != null) {
                int length = authnStatements.size();
                for (int i = 0; i < length; i++) {
                    AuthnStatement authn = 
                        (AuthnStatement)authnStatements.get(i);
                    authn.makeImmutable();
                }
                authnStatements = Collections.unmodifiableList(
                                             authnStatements);
            }
            if (authzDecisionStatements != null) {
                int length = authzDecisionStatements.size();
                for (int i = 0; i < length; i++) {
                    AuthzDecisionStatement authz =
                        (AuthzDecisionStatement)authzDecisionStatements.get(i);
                    authz.makeImmutable();
                }
                authzDecisionStatements = Collections.unmodifiableList(
                                              authzDecisionStatements);
            }
            if (attributeStatements != null) {
                int length = attributeStatements.size();
                for (int i = 0; i < length; i++) {
                    AttributeStatement attr =
                        (AttributeStatement)attributeStatements.get(i);
                    attr.makeImmutable();
                }
                attributeStatements = Collections.unmodifiableList(
                                              attributeStatements);
            }
            if (statements != null) {
                statements = Collections.unmodifiableList(statements);
            }
            if (conditions != null) {
                conditions.makeImmutable();
            }
            if (issuer != null) {
                issuer.makeImmutable();
            }
            if (subject != null) {
                subject.makeImmutable();
            }
            if (advice != null) {
                advice.makeImmutable();
            }
            isMutable = false;
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
   @Override
    public boolean isMutable() {
        return isMutable;
    }
}
