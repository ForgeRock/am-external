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
 * $Id: StatusImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2017-2025 Ping Identity Corporation.
 */
package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.STATUS;

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
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusDetail;
import com.sun.identity.xacml.context.StatusMessage;

/**
 * The <code>Status</code> element is a container of 
 * one or more <code>Status</code>s issuded by authorization authority.
 */
@SupportedAll
public class StatusImpl implements Status {

    private static final Logger logger = LoggerFactory.getLogger(StatusImpl.class);
    private StatusCode statusCode;
    private StatusMessage statusMessage;
    private StatusDetail statusDetail;
    private boolean mutable = true;

    /** 
     * Constructs a <code>Status</code> object
     */
    public StatusImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>Status</code> object from an XML string
     *
     * @param xml string representing a <code>Status</code> object
     * @throws XACMLException If the XML string could not be processed.
     */
    public StatusImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "StatusImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>Status</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>Status</code> 
     * object
     *
     * @throws XACMLException If the DOM element could not be processed.
     */
    public StatusImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }


    /**
     * Returns the <code>StatusCode</code> of this object
     *
     * @return the <code>StatusCode</code> of this object
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the <code>StatusCode</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setStatusCode(StatusCode statusCode) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (statusCode == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }
        this.statusCode = statusCode;
    }

    /**
     * Returns the <code>StatusMessage</code> of this object
     *
     * @return the <code>StatusMessage</code> of this object
     */
    public StatusMessage getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the <code>StatusMessage</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setStatusMessage(StatusMessage statusMessage) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the <code>StatusDetail</code> of this object
     *
     * @return the <code>StatusDetail</code> of this object
     */
    public StatusDetail getStatusDetail() {
        return statusDetail;
    }

    /**
     * Sets the <code>StatusDetail</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setStatusDetail(StatusDetail statusDetail) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        this.statusDetail = statusDetail;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element statusElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI, STATUS,
                includeNSPrefix, declareNS);
        fragment.appendChild(statusElement);

        if (statusCode != null) {
            statusElement.appendChild(statusCode.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (statusMessage != null) {
            statusElement.appendChild(statusMessage.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (statusDetail != null) {
            statusElement.appendChild(statusDetail.toDocumentFragment(document, includeNSPrefix, declareNS));
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
        if (mutable) {
            if (statusCode != null) {
                statusCode.makeImmutable();
            }
            if (statusMessage != null) {
                statusMessage.makeImmutable();
            }
            if (statusDetail != null) {
                statusDetail.makeImmutable();
            }
            mutable = false;
        }
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "StatusImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "StatusImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.STATUS)) {
            logger.error(
                "StatusImpl.processElement(): invalid local name " + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }

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
        if (childCount < 1) {
            logger.error(
                "StatusImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count")); //FIXME: add i18n key
        } else if (childCount > 3) {
            logger.error(
                "StatusImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count")); //FIXME: add i18n key
        }
        Element firstChild = (Element)childElements.get(0);
        String firstChildName = firstChild.getLocalName();
        if (firstChildName.equals(XACMLConstants.STATUS_CODE)) {
            statusCode =  ContextFactory.getInstance()
                    .createStatusCode(firstChild);
        } else {
            logger.error(
                "StatusImpl.processElement(): invalid first child element: " 
                        + firstChildName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_first_child")); //FIXME: add i18n key
        }
        //process statusMessage element
        if (childCount > 1) {
            Element secondChild = (Element)childElements.get(1);
            String secondChildName = secondChild.getLocalName();
            if (secondChildName.equals(
                        XACMLConstants.STATUS_MESSAGE)) {
                statusMessage =  ContextFactory.getInstance()
                        .createStatusMessage(secondChild);

            } else if (secondChildName.equals(
                    XACMLConstants.STATUS_DETAIL)) {
                if (childCount == 2) {
                    statusDetail =  ContextFactory.getInstance()
                            .createStatusDetail(secondChild);
                } else {
                    logger.error(
                        "StatusImpl.processElement(): "
                                + "invalid second child element: " 
                                + secondChildName);
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_second_child")); //FIXME: add i18n key
                }
            }
            if (childCount > 2) {
                Element thirdChild = (Element)childElements.get(2);
                String thirdChildName = thirdChild.getLocalName();
                if (thirdChildName.equals(
                            XACMLConstants.STATUS_DETAIL)) {
                    statusDetail =  ContextFactory.getInstance()
                            .createStatusDetail(thirdChild);
                } else {
                    logger.error(
                        "StatusImpl.processElement(): invalid third child element: " 
                                + thirdChildName);
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_third_child")); //FIXME: add i18n key
                }
            }
        }
    }

}
