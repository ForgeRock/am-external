/**
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
 * $Id: StatusCodeImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2017-2021 ForgeRock AS.
 */
package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.STATUS_CODE;
import static com.sun.identity.xacml.common.XACMLConstants.VALUE;

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
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.StatusCode;

/**
 * The <code>StatusCode</code> element is a container of 
 * one or more <code>StatusCode</code>s issuded by authorization authority.
 */
@SupportedAll
public class StatusCodeImpl implements StatusCode {


    private static final Logger logger = LoggerFactory.getLogger(StatusCodeImpl.class);
    String value = null;
    String minorCodeValue = null;
    private boolean mutable = true;

    /** 
     * Constructs a <code>StatusCode</code> object
     */
    public StatusCodeImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>StatusCode</code> object from an XML string
     *
     * @param xml string representing a <code>StatusCode</code> object
     * @throws XACMLException If the XML string could not be processed.
     */
    public StatusCodeImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "StatusCodeImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>StatusCode</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>StatusCode</code> 
     * object
     *
     * @throws XACMLException If the DOM element could not be processed.
     */
    public StatusCodeImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns the <code>value</code> of this object
     *
     * @return the <code>value</code> of this object
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the <code>value</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setValue(String value) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        if (!XACMLSDKUtils.isValidStatusCode(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        }
        this.value = value;
    }

    /**
     * Returns the <code>minorCodeValue</code> of this object
     *
     * @return the <code>minorCodeValue</code> of this object
     */
    public String getMinorCodeValue() {
        return minorCodeValue;
    }

    /**
     * Sets the <code>minorCodeValue</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setMinorCodeValue(String minorCodeValue) 
            throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        if (!XACMLSDKUtils.isValidMinorStatusCode(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        }
        this.minorCodeValue = minorCodeValue;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element statusCodeElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI,
                STATUS_CODE, includeNSPrefix, declareNS);
        fragment.appendChild(statusCodeElement);

        if (value != null) {
            statusCodeElement.setAttribute(VALUE, value);
        }

        if (minorCodeValue != null) {
            Element minorCodeElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI,
                    STATUS_CODE, includeNSPrefix, false);
            minorCodeElement.setAttribute(VALUE, minorCodeValue);
            statusCodeElement.appendChild(minorCodeElement);
        }

        return fragment;
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
    
   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        mutable = false;
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "StatusMessageImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "StatusMessageImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(STATUS_CODE)) {
            logger.error(
                    "StatusMessageImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        String attrValue = element.getAttribute(VALUE);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            logger.error(
                "StatusCodeImpl.processElement(): statuscode missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_status_code")); //i18n
        } 
        if (!XACMLSDKUtils.isValidStatusMessage(attrValue.trim())) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_value"));
        } else {
            this.value = attrValue;
        }
        //process child StatusCode element
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        List childElements = new ArrayList(); 
        int i = 0;
        while (i < numOfNodes) { 
            Node child = (Node) nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add(child);
            }
           i++;
        }
        int childCount = childElements.size();
        if (childCount > 1) {
            logger.error(
                "ResultImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count"));
        }
        if (childCount == 1) {
            Element childElement = (Element)childElements.get(0);
            elemName = childElement.getLocalName();
            if (elemName == null) {
                logger.error(
                    "StatusMessageImpl.processElement(): local name missing");
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_local_name"));
            }

            if (!elemName.equals(STATUS_CODE)) {
                logger.error(
                        "StatusMessageImpl.processElement(): invalid local name " 
                        + elemName);
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_local_name"));
            }
            attrValue = childElement.getAttribute(VALUE);
            if ((attrValue == null) || (attrValue.length() == 0)) {
                logger.error(
                    "StatusCodeImpl.processElement(): minor statuscode missing");
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_minor_status_code"));
            } 
            if (!XACMLSDKUtils.isValidStatusMessage(attrValue.trim())) {
                throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_value"));
            } else {
                this.minorCodeValue = attrValue;
            }
        } else {
        }
    }

}
