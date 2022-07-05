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
 * $Id: SubjectImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */

package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.SUBJECT;
import static com.sun.identity.xacml.common.XACMLConstants.SUBJECT_CATEGORY;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Subject;

/**
 * The <code>Subject</code> element specifies information about a
 * subject of the <code>Request</code> context by listing a 
 * sequence of <code>Attribute</code> elements associated with the
 * subject. A subject is an entity associated with the access request.
 * <p>
 * <pre>
 * &lt;xs:complexType name="SubjectType"&gt;
 *  &lt;xs:sequence&gt;
 *   &lt;xs:element ref="xacml-context:Attribute" minOccurs="0"
 *      maxOccurs="unbounded"/&gt;
 * &lt;xs:sequence&gt;
 * &lt;xs:attribute name="SubjectCategory" type="xs:anyURI" 
 *  default="urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"/&gt;
 * &lt;xs:complexType&gt;
 * </pre>
 */
@SupportedAll
public class SubjectImpl implements Subject {
    private static final Logger logger = LoggerFactory.getLogger(SubjectImpl.class);
    private List<Attribute> attributes ;
    private URI subjectCategory;
    private Attribute subjectCategoryAttribute;
    private boolean isMutable = true;
    private boolean needToCreateSubjectCategory = false;

   /** 
    * Default constructor
    */
    public SubjectImpl() {
    }

    /**
     * This constructor is used to build <code>Subject</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Subject</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public SubjectImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "SubjectImpl.processElement(): invalid XML input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Subject</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Subject</code> object
     * @exception XACMLException if it could not process the Element
     */
    public SubjectImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "SubjectImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
             logger.error(
                "SubjectImpl.processElement(): local name missing");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.SUBJECT)) {
            logger.error(
                "SubjectImpl.processElement(): invalid local name " +
                 elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes >= 1) {
            ContextFactory factory = ContextFactory.getInstance();
            for (int nextElem = 0; nextElem < numOfNodes; nextElem++) {
                Node child = (Node)nodes.item(nextElem);
                if ((child.getNodeType() == Node.ELEMENT_NODE) ||
                    (child.getNodeType() == Node.ATTRIBUTE_NODE )) {
                    // The child nodes should be <Attribute> 
                    // or <SubjectCategory>
                    String attrChildName = child.getLocalName();
                    if (attrChildName.equals(XACMLConstants.ATTRIBUTE)) {
                        if (this.attributes == null) {
                        this.attributes = new ArrayList<>();
                        }
                        Attribute attribute = factory.getInstance().
                                createAttribute((Element)child);
                        attributes.add(attribute);
                    } else if (attrChildName.equals(
                            XACMLConstants.SUBJECT_CATEGORY)) {
                        try {
                            subjectCategory = new URI (child.getNodeValue());
                        } catch ( Exception e) {
                            throw new XACMLException(
                                XACMLSDKUtils.xacmlResourceBundle.getString( 
                                    "attribute_not_uri"));
                        }
                    } else {
                        logger.error("RequestImpl."
                            +"processElement(): Invalid element :"
                            +attrChildName);
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString( 
                                "invalid_element"));
                    }
                }
            }
         }
    }
    /**
     * Returns zero to many <code>Attribute</code> elements of this object
     * If no attributes and present, empty <code>List</code> will be returned.
     * Typically a <code>Subject</code> element will contain an <code>
     * Attribute</code> with an <code>AttributeId</code> of
     * "urn:oasis:names:tc:xacml:1.0:subject:subject-id", containing 
     * the identity of the <code>Subject</code>
     *
     * @return the <code>Attribute</code> elements of this object
     */
    public List getAttributes() { 
        return attributes;
    }

    /**
     * Sets the <code>Attribute</code> elements of this object
     *
     * @param attributes <code>Attribute</code> elements of this object
     * attributes could be an empty <code>List</code>, if no attributes
     * are present.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributes(List attributes) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (attributes != null &&  !attributes.isEmpty()) {
            if (this.attributes == null) {
                this.attributes = new ArrayList<>();
            }
            this.attributes.addAll(attributes);
        }
    }

    /**
     * Returns the <code>SubjectCategory</code> of this object.
     * This is optional so could be null if not defined.
     * This attribute indicates the role that the parent <code>Subject</code> 
     * played in the formation of the access request. If this attribute is not 
     * present in the <code>Subject</code> element, then the
     * default value of 
     * urn:oasis:names:tc:xacml:1.0:subject-category:access-subject SHALL be
     * used, indicating that the <code>Subject</code> represents the entity 
     * ultimately responsible for initiating the access request.
     *
     * @return <code>URI</code> representing the 
     * <code>SubjectCategory</code> of this  object.
     */
    public URI getSubjectCategory() {
        try {
            if (subjectCategory == null) {
                subjectCategory = new URI(XACMLConstants.ACCESS_SUBJECT);
            }
        } catch (Exception e) { // cant do anything, return null
        }
        return subjectCategory;
    }

    /**
     * Sets the <code>SubjectCategory</code> of this object
     *
     * @param subjectCategory <code>URI</code> 
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setSubjectCategory(URI subjectCategory) throws 
        XACMLException 
    {
        if (!isMutable) {
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (subjectCategory != null) {
            this.subjectCategory = subjectCategory;
        } /*else {
            needToCreateSubjectCategory = true;
             try {
               subjectCategory = new URI(SUBJECT_CATEGORY_DEFAULT);
               List values = new ArrayList();
               values.add(subjectCategory.toString());
                 subjectCategoryAttribute = 
                     XACMLSDKUtils.createAttribute(values, new URI(
                     SUBJECT_CATEGORY_ID), new URI(URI_DATATYPE), null);
             } catch ( Exception e) {
                 throw new XACMLException(e);
             }
        }*/
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element subjectElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI, SUBJECT,
                includeNSPrefix, declareNS);
        fragment.appendChild(subjectElement);

        if (subjectCategory != null) {
            subjectElement.setAttribute(SUBJECT_CATEGORY, subjectCategory.toString());
        }

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                subjectElement.appendChild(attribute.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }

    /**
    * Makes the object immutable
    */
    public void makeImmutable() {}

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return isMutable;
    }
    
}
