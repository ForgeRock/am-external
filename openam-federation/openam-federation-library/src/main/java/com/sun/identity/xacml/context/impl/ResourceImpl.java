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
 * $Id: ResourceImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2017-2021 ForgeRock AS.
 */
package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_SCHEMA_LOCATION_VALUE;
import static com.sun.identity.xacml.common.XACMLConstants.RESOURCE;
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
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Resource;

/**
 * The <code>Resource</code> element specifies information about the
 * resource to which access is requested by listing a 
 * sequence of <code>Attribute</code> elements associated with the
 * resource. it may include <code>ResourceContent</code>
 * <p>
 * <pre>
 * &lt;xs:element name="Resource" type="xacml-context:ResourceType"/&gt;
 *   &lt;xs:complexType name="ResourceType"&gt;
 *     &lt;xs:sequence&gt;
 *       &lt;xs:element ref="xacml-context:ResourceContent" minOccurs="0"/&gt;
 *       &lt;xs:element ref="xacml-context:Attribute" minOccurs="0" 
 *          maxOccurs="unbounded"/&gt;
 *    &lt;xs:sequence&gt;
 *  &lt;xs:complexType&gt;
 * </pre>
 */
@SupportedAll
public class ResourceImpl implements Resource {
    private static final Logger logger = LoggerFactory.getLogger(ResourceImpl.class);
    private List<Attribute>  attributes;
    private Element resourceContent;
    private boolean isMutable = true;

   /** 
    * Default constructor
    */
    public ResourceImpl() {
    }

    /**
     * This constructor is used to build <code>Resource</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Resource</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public ResourceImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "SubjectImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>resource</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Resource</code> object
     * @exception XACMLException if it could not process the Element
     */
    public ResourceImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "ResourceImpl.processElement(): invalid root element");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
             logger.error(
                "ResourceImpl.processElement(): local name missing");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.RESOURCE)) {
            logger.error(
                "ResourceImpl.processElement(): invalid local name " +
                 elemName);
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes > 0) {
            ContextFactory factory = ContextFactory.getInstance();
            for (int i=0; i< numOfNodes; i++) {
                Node child = (Node)nodes.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String childName = child.getLocalName();
                    // The child nodes should be <Attribute> or 
                    // <ResourceContent>
                    if (childName.equals(XACMLConstants.ATTRIBUTE)) {
                        if (attributes == null) {
                            attributes = new ArrayList<>();
                        }
                        Attribute attribute = factory.getInstance().
                            createAttribute((Element)child);
                        attributes.add(attribute);
                    } else if (childName.equals(
                            XACMLConstants.RESOURCE_CONTENT)) {
                        resourceContent = (Element)child;
                    }
                }
            }
         } else {
             /* not a schema violation
             XACMLSDKUtils.debug.error(
                "ResourceImpl.processElement(): no attributes or resource "
                +"content");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_subelements"));
            */
         }
    }

    /**
     * Returns the ResourceConent
     *
     * @return the ResourceContent of the Resource
     */
    public Element getResourceContent() {
        return resourceContent;
    }

    /**
     * Sets the ResourceContent of this Resource
     *
     * @param resourceContent  ResourceContent of this Resource. 
     * ResourceContent  is optional, so could be null.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setResourceContent(Element resourceContent) 
        throws XACMLException {
          if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        String elemName = resourceContent.getLocalName();
        if (elemName == null 
                || !elemName.equals(XACMLConstants.RESOURCE_CONTENT)) {
            logger.error(
                "StatusMessageImpl.processElement():"
                + "local name missing or incorrect");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_local_name"));
        }
        this.resourceContent = resourceContent;
    }

    /**
     * Returns zero to many <code>Attribute</code> elements of this object
     * If no attributes and present, empty <code>List</code> will be returned.
     * Typically a <code>Resource</code> element will contain an <code>
     * Attribute</code> with an <code>AttributeId</code> of
     * "urn:oasis:names:tc:xacml:1.0:resource:resource-id". Each such
     * <code>Attribute</code> SHALL be an absolute abd fully resolved 
     * representation of the identity of the single resource to which
     * access is requested.
     *
     * @return <code>List</code> containing the <code>Attribute</code> 
     * elements of this object
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
        Element resourceElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI, RESOURCE,
                includeNSPrefix, declareNS);
        fragment.appendChild(resourceElement);
        if (declareNS) {
            resourceElement.setAttribute(XSI_NS_ATTR, XSI_NS_URI);
            resourceElement.setAttribute(SCHEMA_LOCATION_ATTR, CONTEXT_SCHEMA_LOCATION_VALUE);
        }

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                resourceElement.appendChild(attribute.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (resourceContent != null) {
            Element adopted = (Element) document.importNode(resourceContent, true);
            if (includeNSPrefix && adopted.getPrefix() == null) {
                adopted = (Element) document.renameNode(adopted, "", CONTEXT_NS_PREFIX + ":" + adopted.getTagName());
            }
            resourceElement.appendChild(adopted);
        }

        return fragment;
    }

    /*
    * Makes the object immutable
    */
    public void makeImmutable() {// TODO 
    }

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
