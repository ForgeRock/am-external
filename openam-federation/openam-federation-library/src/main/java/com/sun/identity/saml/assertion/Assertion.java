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
 * $Id: Assertion.java,v 1.3 2008/06/25 05:47:31 qcheng Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 *
 */
package com.sun.identity.saml.assertion;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.saml.xmlsig.SignatureProvider;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *This object stands for <code>Assertion</code> element. An Assertion is a
 *package of information that supplies one or more <code>Statement</code> made 
 *by an issuer. There are three kinds of assertionsL Authentication, 
 *AuthorizationDecision and Attribute assertion.
 */
@SupportedAll
public class Assertion extends AssertionBase {          

    private static final Logger logger = LoggerFactory.getLogger(Assertion.class);

    /**
     * Signs the Assertion.
     *
     * @param certAlias certification Alias used to sign Assertion.
     * @exception SAMLException if it could not sign the Assertion.
     */
    public void signXML(String certAlias) throws SAMLException {
        if (signed) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion.signXML: the assertion is "
                    + "already signed.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("alreadySigned"));
        }

        if (certAlias == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assetion.signXML: couldn't obtain "
                    + "this site's cert alias.");
            }
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("cannotFindCertAlias"));
        }

        SignatureProvider signatureProvider = InjectorHolder.getInstance(SignatureProvider.class);
        if ((_majorVersion == 1) && (_minorVersion == 0)) {
            logger.debug("Assetion.signXML: sign with version 1.0");
            signatureString = signatureProvider.signXML(this.toString(true, true),
                                              certAlias);
            // this block is used for later return of signature element by
            // getSignature() method
            signature = 
                XMLUtils.toDOMDocument(signatureString)
                        .getDocumentElement();

        } else {
            logger.debug("Assetion.signXML: sign with version 1.1");
            Document doc = XMLUtils.toDOMDocument(this.toString(true, true)
            );
            // sign with SAML 1.1 spec & include cert in KeyInfo
            signature = signatureProvider.signXML(doc, certAlias, null,
                ASSERTION_ID_ATTRIBUTE, getAssertionID(), true, null);
            signatureString = XMLUtils.print(signature);
        }
        signed = true;
        xmlString = this.toString(true, true);
    }

    /** 
     *Default constructor
     *Declaring protected to enable extensibility
     */
    protected Assertion() {
        super();
    }
   
    /**
     * Constructs <code>Assertion</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param assertionElement A <code>org.w3c.dom.Element</code> representing 
     *        DOM tree for <code>Assertion</code> object
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public Assertion(org.w3c.dom.Element assertionElement) 
        throws SAMLException 
    {
        parseAssertionElement(assertionElement);
    }

    protected void parseAssertionElement(Element assertionElement)
        throws SAMLException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Assertion.parseAssertionElement:");
        }

        String eltName = assertionElement.getLocalName();
        if (eltName == null)  {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString
                                        ("nullInput")) ;
        }
        if (!(eltName.equals("Assertion")))  {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString
                ("invalidElement")+ ":"+eltName) ;   
        }

        String read = assertionElement.getAttribute("Issuer");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: Issuer missing");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("missingAttribute") +":"+"Issuer");
        } else  {
            _issuer = read;
        }

        List<Node> signs = XMLUtils.getElementsByTagNameNS1(assertionElement,
                                        SAMLConstants.XMLSIG_NAMESPACE_URI,
                                        SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            // delay the signature validation till user call isSignatureValid()
            xmlString = XMLUtils.print(assertionElement);
            signed = true;
            validationDone = false;
        } else if (signsSize != 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion(Element): included more than"
                    + " one Signature element.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("moreElement"));
        }

        read = assertionElement.getAttribute("MajorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion: MajorVersion missing");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("missingAttribute")+":"+
                        "MajorVersion");
        }
        else  {
            int ver;
            try {
                ver = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                logger.error(
                        "Assertion: invalid integer in MajorVersion", ne);
                throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("invalidNumber")+":"+
                        "MajorVersion");
            }
            if (ver != SAMLConstants.ASSERTION_MAJOR_VERSION) {
                if (ver < SAMLConstants.ASSERTION_MAJOR_VERSION) {
                    if (logger.isDebugEnabled())  {
                        logger.debug(
                            "Assertion: MajorVersion too low");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooLow")
                        + ":"+"MajorVersion");
                } else {
                    if (logger.isDebugEnabled())  {
                        logger.debug(
                            "Assertion: MajorVersion too high");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooHigh")
                            +":"+"MajorVersion");
                }
            }
        }
        read = assertionElement.getAttribute("MinorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled()) 
                logger.debug("Assertion: MinorVersion missing");
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"MinorVersion");
        }
        else  {
            int ver;
            try {
                ver = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                logger.error(
                        "Assertion: invalid integer in MinorVersion", ne);
                throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("invalidNumber")
                                    +":"+"MinorVersion");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Assertion.parseAssertionElement: " +
                       "minMinorVersion = " + getMinAssertionMinorVersion() +
                       ", maxMinorVersion = " + getMaxAssertionMinorVersion());
            }

            if (ver < getMinAssertionMinorVersion()) {
                if (logger.isDebugEnabled())  {
                    logger.debug("Assertion: MinorVersion too low");
                }
                throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooLow"));
            } else if (ver > getMaxAssertionMinorVersion()) {
                if (logger.isDebugEnabled())  {
                    logger.debug("Assertion: MinorVersion too high");
                }
                throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooHigh")
                                    +":"+"MinorVersion");
            } else {
                _minorVersion=ver;
            }
        }
        read = assertionElement.getAttribute("AssertionID");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled()) 
                logger.debug("Assertion: AssertionID missing");
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"AssertionID");
        }
        else  {
            _assertionID = new AssertionIDReference(read);
        }

        read = assertionElement.getAttribute("IssueInstant");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion: IssueInstant missing");
            }
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"IssueInstant");
        } else  {
            try {
                _issueInstant = DateUtils.stringToDate(read);
            } catch (ParseException pe) {
                if (logger.isDebugEnabled()) 
                    logger.debug(
                    "Assertion: could not parse IssueInstant", pe);
               throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                        "wrongInput") + " " + pe.getMessage());
            }
        }

        NodeList nl = assertionElement.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String childName = child.getLocalName();
            if (childName.equals("Conditions"))
                _conditions = new Conditions((Element)child);
            else if (childName.equals("Advice"))
                _advice = new Advice((Element)child);
            else if (childName.equals("AuthenticationStatement")) {
                _statements.add(new AuthenticationStatement((Element)child));
            }
            else if (childName.equals("AuthorizationDecisionStatement")) {
                _statements.add(new AuthorizationDecisionStatement(
                        (Element)child));
            }
            else if (childName.equals("AttributeStatement")) {
                _statements.add(new AttributeStatement((Element)child));
            }
            else if (childName.equals("Signature")) {
                signature = (Element) child;
            }
            else if (!processUnknownElement((Element)child)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Assertion: invalid element in Assertion");
                }
                throw new SAMLRequesterException("invalidElement");
            }
        }
        if (_statements.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Assertion: mandatory statement missing");
            }
            throw new SAMLRequesterException("missingStatement");
        }
    } 
   
 
    /**
     *  Constructs <code>Assertion</code> object and populate the data members:
     * <code>assertionID</code>, the issuer, time when assertion issued and a
     * set of <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>assertionID</code> attribute contained within
     *        this <code>Assertion</code> if null, an <code>assertionID</code>
     *        is generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification.if null, current time is used.
     * @param statements set of <code>Statement</code> objects within this 
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>, 
     *        <code>AuthorizationDecisionStatement</code> and
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Set<Statement> statements) throws SAMLException
    {
        super(assertionID, issuer, issueInstant, statements);
    }

    /**
     * Constructs <code>Assertion</code> object and  populate the data members:
     * the <code>assertionID</code>, the issuer, time when assertion issued,  
     * the conditions when creating a new assertion and a set of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>AssertionID</code> contained within this 
     *        <code>Assertion</code> if null its generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification. if null, current time is used.
     * @param conditions <code>Conditions</code> under which the this 
     *        <code>Assertion</code> is valid.
     * @param statements Set of <code>Statement</code> objects within this 
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>,
     *        <code>AuthorizationDecisionStatement</code> and 
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, Set<Statement> statements)
        throws SAMLException
    {
        super(assertionID, issuer, issueInstant, conditions, statements);
    }
   
    /**
     * Constructs <code>Assertion</code> object and populate the data members:
     * the <code>AssertionID</code>, the issuer, time when assertion issued,
     * the conditions when creating a new assertion , <code>Advice</code>
     * applicable to this <code>Assertion</code> and a set of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>AssertionID</code> object contained within this
     *        <code>Assertion</code> if null its generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant Time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification. if null, current time is used.
     * @param conditions <code>Conditions</code> under which the this 
     *        <code>Assertion</code> is valid.
     * @param advice <code>Advice</code> applicable for this
     *        <code>Assertion</code>.
     * @param statements Set of <code>Statement</code> objects within this 
     *         <code>Assertion</code>. It could be of type
     *         <code>AuthenticationStatement</code>,
     *         <code>AuthorizationDecisionStatement</code> and 
     *         <code>AttributeStatement</code>. Each Assertion can have
     *         multiple type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, Advice advice, 
        Set<Statement> statements) throws SAMLException
    {
        super(assertionID, issuer, issueInstant, conditions, advice,statements);
    }

    /**
     * Returns the advice of an assertion.
     *
     * @return <code>Advice</code> object containing advice information of the
     *         assertion.
     */
    public Advice getAdvice() {
        return (Advice)_advice; 
    }

    protected AdviceBase createAdvice(Element adviceElement) throws SAMLException {
        return new Advice(adviceElement);
    }

    protected AuthorizationDecisionStatementBase createAuthorizationDecisionStatement(Element authDecisionElement)
            throws SAMLException {
        return new AuthorizationDecisionStatement(authDecisionElement);
    }

    protected AuthenticationStatement createAuthenticationStatement(Element authenticationElement)
            throws SAMLException {
        return new AuthenticationStatement(authenticationElement);
    }

    protected AttributeStatement createAttributeStatement(Element attributeElement) throws SAMLException {
        return new AttributeStatement(attributeElement);
    }

    protected AssertionIDReference createAssertionIDReference(Element assertionIDRefElement) throws SAMLException {
        return new AssertionIDReference(assertionIDRefElement);
    }

    protected AssertionIDReference createAssertionIDReference(String assertionID) {
        return new AssertionIDReference(assertionID);
    }

    protected Conditions createConditions(Element conditionsElement) throws SAMLException {
        return new Conditions(conditionsElement);
    }

    protected boolean processUnknownElement(Element element) throws SAMLException {
        logger.debug("Assertion.processUnknownElement:");
        return false;
    }

    protected int getMinAssertionMinorVersion() {
        return SAMLConstants.ASSERTION_MINOR_VERSION_ZERO;
    }

    protected int getMaxAssertionMinorVersion() {
        return SAMLConstants.ASSERTION_MINOR_VERSION_ONE;
    }
}
