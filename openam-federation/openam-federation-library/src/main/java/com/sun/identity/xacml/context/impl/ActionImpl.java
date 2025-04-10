/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ActionImpl.java,v 1.3 2008/06/25 05:48:12 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */

package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.ACTION;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_SCHEMA_LOCATION_VALUE;
import static com.sun.identity.xacml.common.XACMLConstants.SCHEMA_LOCATION_ATTR;
import static com.sun.identity.xacml.common.XACMLConstants.XSI_NS_ATTR;
import static com.sun.identity.xacml.common.XACMLConstants.XSI_NS_URI;

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
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;

/**
 * The <code>Action</code> element specifies information about the
 * action requested in the <code>Request</code> context by listing a
 * sequence of <code>Attribute</code> elements associated with the
 * action.
 * <p>
 * <pre>
 * &lt;xs:element name="Action" type="xacml-context:ActionType"/&gt;
 * &lt;xs:complexType name="ActionType"&gt;
 *    &lt;xs:sequence&gt;
 *       &lt;xs:element ref="xacml-context:Attribute" minOccurs="0"
 *       maxOccurs="unbounded"/&gt;
 *    &lt;xs:sequence&gt;
 * &lt;xs:complexType&gt;
 * </pre>
 */
@SupportedAll
public class ActionImpl implements Action {
    
    private static final Logger logger = LoggerFactory.getLogger(ActionImpl.class);
    private List<Attribute>  attributes ;
    private boolean mutable = true;
    
    
    /** Creates a new instance of ActionImpl */
    public ActionImpl() {
    }
    
    /**
     * This constructor is used to build <code>Action</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Action</code> object
     * @exception XACMLException if it could not process the XML string
     */
    
    public ActionImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                    "ActionImpl.processElement(): invalid XML input");
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "errorObtainingElement"));
        }
    }
    
    /**
     * This constructor is used to build <code>Action</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Action</code> object
     * @exception XACMLException if it could not process the Element
     */
    public ActionImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }
    
    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                    "ActionImpl.processElement(): invalid root element");
            throw new XACMLException( 
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                    "ActionImpl.processElement(): local name missing");
            throw new XACMLException( 
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_local_name"));
        }
        
        if (!elemName.equals(XACMLConstants.ACTION)) {
            logger.error(
                    "ActionImpl.processElement(): invalid local name " +
                    elemName);
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes >= 1) {
            ContextFactory factory = ContextFactory.getInstance();
            for (int nextElem = 0; nextElem < numOfNodes; nextElem++) {
                Node child = (Node)nodes.item(nextElem);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    // The child nodes should be <Attribute>
                    String attrChildName = child.getLocalName();
                    if (attrChildName.equals(XACMLConstants.ATTRIBUTE)) {
                        if (this.attributes == null) {
                            this.attributes = new ArrayList<>();
                        }
                        Attribute attribute = factory.getInstance().
                                createAttribute((Element)child);
                        attributes.add(attribute);
                    } else {
                        logger.error("ActionImpl."
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
    
    public java.util.List getAttributes() {
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
     * be determined by calling <code>mutable</code> on the object.
     */
    public void setAttributes(java.util.List attributes) throws 
        XACMLException {
        if (!mutable) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "objectImmutable"));
        }
        if (attributes != null &&  !attributes.isEmpty()) {
            if (this.attributes == null) {
                this.attributes = new ArrayList<>();
            }
            this.attributes.addAll(attributes);
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element actionElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI, ACTION,
                includeNSPrefix, declareNS);
        fragment.appendChild(actionElement);

        if (declareNS) {
            actionElement.setAttribute(XSI_NS_ATTR, XSI_NS_URI);
            actionElement.setAttribute(SCHEMA_LOCATION_ATTR, CONTEXT_SCHEMA_LOCATION_VALUE);
        }

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                actionElement.appendChild(attribute.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }

    /**
     * Makes the object immutable
     */
    public void makeImmutable() {
        mutable = false;
    }
    
    /**
     * Checks if the object is mutable
     *
     * @return <code>true</code> if the object is mutable,
     *         <code>false</code> otherwise
     */
    public boolean isMutable() {
        return mutable;
    }
    
}
