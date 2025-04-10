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
 * $Id: AssertionBase.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml.assertion;

import static com.sun.identity.shared.xml.XMLUtils.removeLineBreaksIfEnabled;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *This object stands for <code>Assertion</code> element.An Assertion is a 
 *package of information that supplies one or more <code>Statement</code> made 
 *by an issuer. There are three kinds of assertionsL Authentication,  
 *AuthorizationDecision and Attribute assertion.
 *
 *This class is an abstract base class for all Assertion implementations and
 *encapsulates common functionality.
 *
 */
@SupportedAll
public abstract class AssertionBase {           
    private static final Logger logger = LoggerFactory.getLogger(AssertionBase.class);

    /**
    The statements variable is a HashSet of all the stataments in this assertion
    in the defined sequence
    */
    protected Set<Statement> _statements = Collections.synchronizedSet(new HashSet<>());
 
    /**
    This value specifies the SAML major version. Each assertion MUST specify 
    the SAML major version identifier.The identifier for this version of SAML 
    is 1. 
    */  
    protected  int _majorVersion = SAMLConstants.ASSERTION_MAJOR_VERSION;
   
    /**
    This value specifies the SAML minor version. Each assertion MUST specify 
    the SAML minor version identifier. The identifier for this version of SAML 
    is 0.
    */
    protected  int _minorVersion = SAMLConstants.ASSERTION_MINOR_VERSION;
   
    /**
    The _assertionID attribute specifies the assertion identifier.
    */
    protected AssertionIDReference _assertionID = null; 
       
    /**
    The Issuer attribute specifies the issuer of the assertion by means of a 
    string.
    */
    protected java.lang.String _issuer = null;
    
    /**
    The IssueInstant attribute specifies the time instant of issue in Universal 
    Coordinated Time.
    */
    protected java.util.Date _issueInstant;
   
    /**
    The <code>Conditions</code> element specifies conditions that affect the 
    validity of the asserted statement. 
    */
    protected Conditions _conditions;
   
    /**
    The <code>Advice</code> element specifies additional information related 
    to the assertion that may assist processing in certain situations but which 
    can be ignored by applications that do not support its use. 
    */
    protected AdviceBase _advice;

    protected String    xmlString       = null;
    protected String    signatureString = null;
    protected Element   signature       = null;
    protected boolean   signed          = false;
    protected boolean   validationDone  = true;

    protected static final String ASSERTION_ID_ATTRIBUTE = "AssertionID";

    /**
     * Signs the Assertion.
     *
     * @param certAlias certification Alias used to sign Assertion.
     * @exception SAMLException if it could not sign the Assertion.
     */
    public void signXML(String certAlias) throws SAMLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the Signature element of the Assertion.
     * @return Element the Signature of the Assertion in DOM element.
     */
    public Element getSignature() {
        return signature;
    }
    
    /**
     * Sets the signature for the Request 
     * @param elem ds:Signature element
     * @return A boolean value: true if the operation succeeds; false otherwise.
     */
    public boolean setSignature(Element elem) {
        if (signed) {
            return false;
        }
        if (elem == null) {
            return false;
        } else {
            signature = elem;
            signed = true;
            signatureString = XMLUtils.print(elem); 
            return true;
        }
    }

    /**
     * Creates appropriate Advice instance
     * @param adviceElement the Advice Element
     * @return the Advice instance
     */
    protected abstract AdviceBase createAdvice(Element adviceElement)
        throws SAMLException;
    
    /**
     * Create appropriate AuthorizationDecisionStatement instance
     * @param authDecisionElement
     *     the AuthorizationDecisionStatement Element
     * @return AuthorizationDecisionStatement instance
     */
    protected abstract AuthorizationDecisionStatementBase
        createAuthorizationDecisionStatement(Element authDecisionElement)
        throws SAMLException;
     
    /**
     * Creates appropriate AuthenticationStatement instance
     * @param authenticationElement
     *     the AuthenticationStatement Element
     * @return AuthenticationStatement instance
     */
    protected abstract AuthenticationStatement
        createAuthenticationStatement(Element authenticationElement)
        throws SAMLException;
     
    /**
     * Creates appropriate AttributeStatement instance
     * @param attributeElement
     *    the AttributeStatement Element
     * @return AttributeStatement instance
     */
    protected abstract AttributeStatement
        createAttributeStatement(Element attributeElement) throws SAMLException;

    /**
     * Creates appropriate AssertionIDReference instance
     * @param assertionIDRefElement
     *     the AssertionIDReference Element
     * @return AssertionIDReference instance
     */
    protected abstract AssertionIDReference
        createAssertionIDReference(Element assertionIDRefElement)
        throws SAMLException;
     
    /**
     * Creates appropriate AssertionIDReference instance
     * @param assertionID
     *     the AssertionID String
     * @return AssertionIDReference instance
     */
    protected abstract AssertionIDReference
        createAssertionIDReference(String assertionID) throws SAMLException;
     
    /**
     * Creates appropriate Conditions instance
     * @param conditionsElement
     *     the Conditions Element
     * @return Conditions instance
     */
    protected abstract Conditions
        createConditions(Element conditionsElement) throws SAMLException;


    /** 
     *Default constructor, declaring protected to enable extensibility
     */
    protected AssertionBase() {}
   
    /**
     * Contructor
     * This constructor is used to build <code>Assertion</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param assertionElement A <code>org.w3c.dom.Element</code> representing 
     *        DOM tree for <code>Assertion</code> object
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public AssertionBase(org.w3c.dom.Element assertionElement) throws SAMLException {
        String eltName = assertionElement.getLocalName();
        if (eltName == null)  {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                                        ("nullInput")) ;
        }
        if (!(eltName.equals("Assertion")))  {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: " +
                                              "invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("invalidElement")+ ":"+eltName) ;   
        }

        String read = assertionElement.getAttribute("Issuer");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: Issuer missing");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("missingAttribute") +
                                                 ":Issuer");
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
                logger.debug("Assertion(Element): " +
                     "included more than one Signature element.");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("moreElement"));
        }

        read = assertionElement.getAttribute("MajorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion: " +   
                                              "MajorVersion missing");
            }
            throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("missingAttribute")+":"+
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
                        SAMLUtilsCommon.bundle.getString("invalidNumber")+":"+
                        "MajorVersion");
            }
            if (ver != SAMLConstants.ASSERTION_MAJOR_VERSION) {
                if (ver < SAMLConstants.ASSERTION_MAJOR_VERSION) {
                    if (logger.isDebugEnabled())  {
                        logger.debug(
                            "Assertion: MajorVersion too low");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtilsCommon.bundle.getString(
                        "assertionVersionTooLow")
                        + ":"+"MajorVersion");
                } else {
                    if (logger.isDebugEnabled())  {
                        logger.debug(
                            "Assertion: MajorVersion too high");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtilsCommon.bundle.getString(
                            "assertionVersionTooHigh")
                            +":"+"MajorVersion");
                }
            }
        }
        read = assertionElement.getAttribute("MinorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled()) 
                logger.debug(
                    "Assertion: MinorVersion missing");
            throw new SAMLRequesterException(
                  SAMLUtilsCommon.bundle.getString("missingAttribute")
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
                        SAMLUtilsCommon.bundle.getString("invalidNumber")
                                    +":"+"MinorVersion");
            }

            if (ver < SAMLConstants.ASSERTION_MINOR_VERSION_ZERO) {
                if (logger.isDebugEnabled())  {
                    logger.debug(
                        "Assertion: MinorVersion too low");
                }
                throw new SAMLVersionMismatchException(
                        SAMLUtilsCommon.bundle.getString(
                        "assertionVersionTooLow"));
            } else if (ver > SAMLConstants.ASSERTION_MINOR_VERSION_ONE) {
                if (logger.isDebugEnabled())  {
                    logger.debug(
                    "Assertion: MinorVersion too high");
                }
                throw new SAMLVersionMismatchException(
                     SAMLUtilsCommon.bundle.getString("assertionVersionTooHigh")
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
                SAMLUtilsCommon.bundle.getString("missingAttribute")
                +":"+"AssertionID");
        }
        else  {
            _assertionID = createAssertionIDReference(read);
        }

        read = assertionElement.getAttribute("IssueInstant");
        if ((read == null) || (read.length() == 0)) {
            if (logger.isDebugEnabled())  {
                logger.debug(
                "Assertion: IssueInstant missing");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("missingAttribute")
                +":"+"IssueInstant");
        }
        else  {
            try {
                _issueInstant = DateUtils.stringToDate(read);
            } catch (ParseException pe) {
                if (logger.isDebugEnabled()) 
                    logger.debug(
                    "Assertion: could not parse IssueInstant", pe);
               throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString(
                    "wrongInput") + " " + pe.getMessage());
            }
        }
        boolean statementFound = false;
        NodeList nl = assertionElement.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String childName = child.getLocalName();
            switch (childName) {
            case "Conditions":
                _conditions = createConditions((Element) child);
                break;
            case "Advice":
                _advice = createAdvice((Element) child);
                break;
            case "AuthenticationStatement":
                _statements.add(createAuthenticationStatement((Element) child));
                statementFound = true;
                break;
            case "AuthorizationDecisionStatement":
                _statements.add(createAuthorizationDecisionStatement(
                        (Element) child));
                statementFound = true;
                break;
            case "AttributeStatement":
                _statements.add(createAttributeStatement((Element) child));
                statementFound = true;
                break;
            case "Signature":
                signature = (Element) child;
                break;
            default:
                logger.debug("Assertion: invalid element in Assertion");
                throw new SAMLRequesterException("invalidElement");
            }
        }
        if (!statementFound) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Assertion: mandatory statement missing");
            }
            throw new SAMLRequesterException("missingStatement");
        }
            
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
    public boolean isTimeValid() {
        if (_conditions == null)  {
            return true;
        }
        else  {
            
            return _conditions.checkDateValidity(currentTimeMillis());
        }
    }
 
    /**
     *Contructor
     *This constructor is used to populate the data members:
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
    public AssertionBase(String assertionID,java.lang.String issuer, 
        Date issueInstant, Set<Statement> statements) throws SAMLException
    {
        if ((issuer == null) || (issuer.length() == 0)) {
            if (logger.isDebugEnabled())  {
                logger.debug(
                "Assertion:  null input specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        if (statements.size() == 0 ) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion:mandatory statement"
                    + " missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "missingStatement")) ;   
        } else {
            _statements.addAll(statements);
        }
        _assertionID = createAssertionIDReference(assertionID);
        _issuer = issuer;
        if (issueInstant != null)  {
            _issueInstant = issueInstant;
        }
        else {
            _issueInstant = newDate();
        }
    }

    /**
     * This constructor is used to populate the data members: the
     * <code>assertionID</code>, the issuer, time when assertion issued, the 
     * conditions when creating a new assertion and a set of
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
    public AssertionBase(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, Set<Statement> statements)
        throws SAMLException
    {
        if ((issuer == null) || (issuer.length() == 0) || (conditions == null))
        {
            if (logger.isDebugEnabled()) {
                logger.debug(
                "Assertion:  null input specified");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("nullInput")) ;   
        }
        if (statements.size() == 0 ) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion:mandatory statement"
                    + " missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "missingStatement")) ;   
        } else {
            _statements.addAll(statements);
        }
        _assertionID = createAssertionIDReference(assertionID);
        _issuer = issuer;
        if (issueInstant != null)  {
            _issueInstant = issueInstant;
        } else  {
            _issueInstant = newDate();
        }
        _conditions = conditions;
    }
   
    /**
     * This constructor is used to populate the data members: the 
     * <code>ssertionID</code>, the issuer, time when assertion issued,
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
    public AssertionBase(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, AdviceBase advice, 
        Set<Statement> statements) throws SAMLException
    {
        if ((issuer == null) || (issuer.length() == 0) || (conditions == null))
        {
            if (logger.isDebugEnabled()) {
                logger.debug(
                "Assertion:  null input specified");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("nullInput")) ;   
        }
        if (statements.size() == 0 ) {
            if (logger.isDebugEnabled())  {
                logger.debug("Assertion:mandatory statement"
                    + " missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "missingStatement")) ;   
        } else {
            _statements.addAll(statements);
        }
        _assertionID = createAssertionIDReference(assertionID);
        _issuer = issuer;
        if (issueInstant != null)  {
            _issueInstant = issueInstant;
        } else  {
            _issueInstant = newDate();
        }
        _conditions = conditions;
        if (advice != null)  {
            _advice = advice;
        }
    }

    /**
     *Adds a statement to this <code>Assertion</code>
     *@param statement <code>Statement</code> to be added
     *@return boolean indicating success or failure of operation.
     */
    public boolean addStatement(Statement statement) {
        if (signed) {
            return false;
        }
        if (statement == null) {
           return false;
        }
        _statements.add(statement);
        return true;
    }
   
    /**
     *Set the time when the assertion was issued
     *@param issueInstant : <code>java.util.Date</code> representing the time
     *                      of the assertion 
     *@return A boolean indicating the success of the operation.
     */
    protected boolean setIssueInstant(java.util.Date issueInstant) {
        if (signed) {
            return false;
        }
        if (issueInstant == null) {
            return false;
        }
        _issueInstant = issueInstant;
        return true; 
    }
      
    /**
     *Set the <code>AssertionID</code> for this assertion
     *@param assertionID : a String representing id of this 
     *                     assertion.
     *@return A boolean indicating the success of the operation.
     */
    protected boolean setAssertionID(String assertionID) {
        if (signed) {
            return false;
        }
        if(assertionID == null) {
            return false;
        }
        try {
            _assertionID = createAssertionIDReference(assertionID);
        } catch (Exception e ) {
            if (logger.isDebugEnabled()) {
                logger.debug("Assertion: Exception in setting"
                    + " assertion id: "+e.getMessage());
            }
            return false;
        }
        return true; 
    }
      
    /**
     *Sets the issuer for an assertion
     *@param issuer : a string representing the issuer of the assertion 
     *@return A boolean indicating the success of the operation.
     */
    protected boolean setIssuer(java.lang.String issuer) {
        if (signed) {
            return false;
        }
        if ((issuer == null) || (issuer.length() == 0)) {
            return false;
        }
        _issuer = issuer;
        return true; 
    }
      
    /**
     *Sets the advice for an assertion 
     *@param advice : a linked list representing the advice information 
     *@return A boolean indicating the success of the operation.
     */
    public boolean setAdvice(AdviceBase advice) {
        if (signed) {
            return false;
        }
        if (advice == null)  {
            return false;
        }
        _advice = advice;
        return true; 
    }
       
    /**
     *Sets the Conditions information for an assertion 
     *@param conditions a linked list representing the conditions information 
     *@return A boolean indicating the success of the operation.
     */                  
    public boolean setConditions(Conditions  conditions) {
        if (signed) {
            return false;
        }
        if ( conditions == null)  {
            return false;
        }
        _conditions = conditions;
        return true; 
    }                                                                    
   
    /**
     * Returns the minor version number of an assertion.
     *
     * @return The minor version number of an assertion.
     */
    public int getMinorVersion() {
        return _minorVersion; 
    }
    
    /**
     * Sets the minor version number of an assertion.
     *
     * @param minorVersion minor version.
     */
    public void setMinorVersion(int minorVersion) {
        this._minorVersion = minorVersion; 
    }
    
    /**
     * Returns the major version number of an assertion.
     *
     * @return The major version number of an assertion. 
     */
    public int getMajorVersion() {
        return _majorVersion; 
    }
   
    /**
     * Sets the major version number of an assertion.
     *
     * @param majorVersion major version.
     */
    public void setMajorVersion(int majorVersion) {
        this._majorVersion = majorVersion; 
    }
    
    /**                 
     * Returns the time when the assertion was issued.
     *
     * @return The time in <code>java.util.Date</code> format.
     */
    public Date getIssueInstant() {
        return _issueInstant; 
    }
   
    /**
     * Returns the issuer of an assertion.
     *
     * @return The issuer of an assertion. 
    */
    public java.lang.String getIssuer() {
        return _issuer; 
    }

    /**
     * Returns the assertion ID.
     *
     * @return Assertion ID of the assertion.
     */
    public String getAssertionID() {
        return _assertionID.getAssertionIDReference();
    }
      
    /**
     * Returns the conditions of an assertion.
     *
     * @return <code>Conditions</code> object containing conditions for an
     *          assertion being valid.
    */
    public Conditions getConditions() {
        return _conditions; 
    }

   
    /**
     * Returns a set of <code>Statement</code> contained within this assertion.
     *
     * @return a set of <code>Statement</code> contained within this assertion. 
     */
    public Set<Statement> getStatement() {
        return _statements;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Assertion&gt;</code>.
     */
    public String toString() {
        // call toString() with includeNS true by default and declareNS false
        return toString(true, false);
    }

    /**
     * Returns a String representation of the <code>&lt;Assertion&gt;</code>
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace 
     *        name example <code>&lt;saml:Assertion&gt;</code>
     * @param declareNS if true includes the namespace within the generated
     *        XML.
     * @return The valid XML for this element
     */
    public String toString(boolean includeNS, boolean declareNS) {
        if (signed && (xmlString != null)) {
            return xmlString;
        }

        StringBuilder xml = new StringBuilder(3000);
        String NS="";
        String appendNS="";
        if (declareNS) {
            NS=SAMLConstants.assertionDeclareStr;
        }
        if (includeNS) {
            appendNS="saml:";
        }
        String dateStr = null;
        if (_issueInstant != null)  {
            dateStr = DateUtils.toUTCDateFormat(_issueInstant);         
        }

        xml.append("<").append(appendNS).append("Assertion").append(" ").
            append(NS).append(" ").append("MajorVersion").append("=\"").
            append(_majorVersion).append("\"").append(" ").
            append("MinorVersion").append("=\"").append(_minorVersion).
            append("\"").append(" ").append("AssertionID=\"").
            append(_assertionID.getAssertionIDReference()).append("\"").
            append(" ").append("Issuer").append("=\"").append(_issuer).
            append("\"").append(" ").append("IssueInstant").append("=\"").
            append(dateStr).append("\"").
            append(" ").append(">").append(SAMLConstants.NL);
        if (_conditions != null) {
            xml.append(_conditions.toString(includeNS, false));
        }
        if (_advice != null) {
            xml.append(_advice.toString(includeNS, false));
        }
        for (Statement st : getStatement()) {
            xml.append(st.toString(includeNS, false));
        }
        if (signed && (signatureString != null)) {
            xml.append(signatureString);
        }
        xml.append(SAMLUtilsCommon.makeEndElementTagXML("Assertion", includeNS));
        return removeLineBreaksIfEnabled(xml.toString());
    }
}
