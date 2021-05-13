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
 * $Id: StatusDetailImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2017-2019 ForgeRock AS.
 */
package com.sun.identity.xacml.context.impl;

import java.util.StringTokenizer;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.StatusDetail;

/**
 * The <code>StatusCode</code> element is a container of 
 * one or more <code>Status</code>s issuded by authorization authority.
 */
@SupportedAll
public class StatusDetailImpl implements StatusDetail {


    private static final Logger logger = LoggerFactory.getLogger(StatusDetailImpl.class);
    private Element element;
    private boolean mutable = true;

    /** 
     * Constructs a <code>StatusDetail</code> object
     */
    public StatusDetailImpl() throws XACMLException {
        String xmlString = "<xacml-context:StatusDetail xmlns:xacml-context="
                + "\"urn:oasis:names:tc:xacml:2.0:context:schema:cd:04\"/>";
        element = new StatusDetailImpl(xmlString).getElement();
    }

    /** 
     * Constructs a <code>StatusDetail</code> object from an XML string
     *
     * @param xml string representing a <code>StatusDetail</code> object
     * @throws XACMLException If the XML string could not be processed.
     */
    public StatusDetailImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "StatusDetailImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>StatusDetail</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>StatusDetail</code> 
     * object
     *
     * @throws XACMLException If the DOM element could not be processed.
     */
    public StatusDetailImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (element == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "StatusMessageImpl.processElement(): local name missing");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }
        this.element = element;
    }

   /**
    * Returns a string representation
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return toXMLString(true, false);
    }

   /**
    * Returns a string representation
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
            throws XACMLException {
        String xmlString = null;
        String nsPrefix = "";
        String nsDeclaration = "";
        if (includeNSPrefix) {
            nsPrefix = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        if (declareNS) {
            nsDeclaration = XACMLConstants.CONTEXT_NS_DECLARATION;
        }
        if (element != null) {
            if (includeNSPrefix && (element.getPrefix() == null)) {
                element.setPrefix(nsPrefix);
            }
            if(declareNS) {
                StringTokenizer st = new StringTokenizer(nsDeclaration, "=");
                String nsName = st.nextToken();
                String nsUri = st.nextToken();
                if (element.getAttribute(nsName) == null) {
                    element.setAttribute(nsName, nsUri);
                }
            }
            xmlString = XMLUtils.print(element) + "\n";
        } else {
            StringBuffer sb = new StringBuffer(2000);
            sb.append("<").append(nsPrefix)
                    .append(XACMLConstants.STATUS_DETAIL)
                    .append(" ")
                    .append(nsDeclaration)
                    .append(">")
                    .append("</")
                    .append(nsPrefix)
                    .append(XACMLConstants.STATUS_DETAIL)
                    .append(">\n");
            xmlString = sb.toString();
        }
        return xmlString;
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
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "StatusMessageImpl.processElement(): local name missing");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.STATUS_DETAIL)) {
            logger.error(
                    "StatusMessageImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        this.element = element;
    }

}
