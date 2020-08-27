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
 * $Id: AttributeStatement.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */


package com.sun.identity.saml.assertion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

/** 
 *The <code>AttributeStatement</code> element supplies a statement by the issuer
 *that the specified subject is associated with the specified attributes. 
 */
@SupportedAll
public class AttributeStatement extends SubjectStatement {
    private static final Logger logger = LoggerFactory.getLogger(AttributeStatement.class);
    private List _attributes = null;
    
    /**
     *Dafault constructor 
     */
    protected AttributeStatement() {
    }
    
    /**
     * Constructs an <code>AttributStatement</code> element from an existing 
     * XML block
     * @param element representing a DOM tree element 
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AttributeStatement(Element element)throws SAMLException {
        // make sure input is not null
        if (element == null) {
            logger.debug("AttributeStatement: null input.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }

        // check if it's an AttributeStatement
        boolean valid = SAMLUtilsCommon.checkStatement(element,
                        "AttributeStatement");
        if (!valid) {
            logger.debug("AttributeStatement: Wrong input.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }

        //Handle the children elements of AttributeStatement  
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = currentNode.getLocalName();
                    String tagNS = currentNode.getNamespaceURI(); 
                    if ((tagName == null) || tagName.length() == 0 ||
                        tagNS == null || tagNS.length() == 0) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                  "AttributeStatement: " +
                                  " The tag name or tag namespace of child" +
                                  " element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("Subject") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (this._subject != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                   "AttributeStatement: "+
                                   "should not contain more than one subject.");
                             }
                             throw new SAMLRequesterException(
                                 SAMLUtilsCommon.bundle.getString(
                                 "oneElement"));
                            
                        } else { 
                            this._subject = 
                                createSubject((Element)currentNode); 
                        }
                    } else if (tagName.equals("Attribute") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (_attributes == null) {
                            _attributes = new ArrayList(); 
                        }
                        if (!_attributes.add(createAttribute(
                            (Element)currentNode))) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                    "AttributeStatement:"+ 
                                    " failed to add to the Attribute list.");
                            }
                            throw new SAMLRequesterException(
                                SAMLUtilsCommon.bundle.getString(
                                    "addListError"));   
                        }
                    } else {
                         if (logger.isDebugEnabled()) {
                             logger.debug(
                                 "AttributeStatement:" +
                                 "wrong element:" + tagName); 
                         }
                         throw new SAMLRequesterException(
                             SAMLUtilsCommon.bundle.getString("wrongInput"));
                    }
                } //end of if (currentNode.getNodeType() == Node.ELEMENT_NODE) 
            } // end of for loop 
        }  // end of if (nodeCount > 0)
        
        // check if the subject is null 
        if (this._subject == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                "AttributeStatement: missing Subject");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        }     
        //check if the attribute is null
        if (_attributes == null || _attributes.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeStatement: " +
                             "should at least contain one Attribute element.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        }
    }

    /**
     * Constructs an instance of <code>AttributeStatement</code>.
     *
     * @param subject (required) A Subject object.
     * @param attribute (one or more) A List of Attribute objects.
     * @exception SAMLException if there is an error in the sender.
     */
    public AttributeStatement(Subject subject, List attribute) 
        throws SAMLException {
        // check if the subject is null 
        if (subject == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeStatement: " +
                                              "missing subject.");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("missingElement"));
        } else {
            this._subject = subject;      
        }
        // check if containing any Attribute                            
        if (attribute == null || attribute.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                "AttributeStatement: Attribute is required.");  
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("missingElement")); 
        }  
        if (_attributes == null) {
            _attributes = new ArrayList();   
        }
        _attributes = attribute;             
    }     
    
    /**
     *Gets attribute from Attribute statement
     *@return A list of Attributes contained in this statement
     */
    public List getAttribute() {
        return _attributes; 
    }
    
    
    /**
     *Gets the type of statement.  
     *@return an Integer which is Statement.ATTRIBUTE_STATEMENT. 
     */
    public int getStatementType() {
        return Statement.ATTRIBUTE_STATEMENT;
    }

    /** 
     *Creates a String representation of the  attribute statement
     *@return A string  representation of  the <code>AttributeStatement</code>
     *       element
     */                              
    public String toString() {
        return toString(true, false); 
    }
    
    /** 
     * Returns a String representation of the Attribute statement.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     *@return A string representation of the
     *        <code>&lt;saml:AttributeStatement&gt;</code> element.
     */
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(3000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        result.append("<").append(prefix).
               append("AttributeStatement ").append(uri).append(">\n");         
    
        result.append(this._subject.toString(includeNS, false)); 
        Iterator iter = _attributes.iterator(); 
        while (iter.hasNext()) {
            Attribute att = (Attribute)iter.next(); 
            result.append(att.toString(includeNS, false)); 
        }
        result.append("</").append(prefix).append("AttributeStatement>\n");
        return(result.toString());
    }

    protected Subject createSubject(Element subjectElement)
        throws SAMLException {
        return new Subject(subjectElement);
    }
    
    protected Attribute createAttribute(Element attributeElement)
        throws SAMLException {
        return new Attribute(attributeElement);
    }
}
