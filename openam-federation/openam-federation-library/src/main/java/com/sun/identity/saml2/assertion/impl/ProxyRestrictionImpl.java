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
 * $Id: ProxyRestrictionImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2018-2023 ForgeRock AS.
 */


package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.ProxyRestriction;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *  The <code>ProxyRestriction</code> specifies limitations that the
 *  asserting party imposes on relying parties that in turn wish to
 *  act as asserting parties and issue subsequent assertions of their
 *  own on the basis of the information contained in the original
 *  assertion. A relying party acting as an asserting party must not
 *  issue an assertion that itself violates the restrictions specified
 *  in this condition on the basis of an assertion containing such
 *  a condition.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class ProxyRestrictionImpl implements ProxyRestriction {

    private static final Logger logger = LoggerFactory.getLogger(ProxyRestrictionImpl.class);
    private int count;
    private boolean isMutable = true;
    private List<String> audiences = new ArrayList<>();

    public static final String PROXY_RESTRICTION_ELEMENT = "ProxyRestriction";
    public static final String COUNT_ATTR = "Count";
    public static final String AUDIENCE_ELEMENT = "Audience";

   /**
    * Default constructor
    */
    public ProxyRestrictionImpl() {
    }

    /**
     * This constructor is used to build <code>ProxyRestriction</code>
     * object from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>ProxyRestriction</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public ProxyRestrictionImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "ProxyRestrictionImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>ProxyRestriction</code>
     * object from a block of existing XML that has already been built
     * into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>ProxyRestriction</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public ProxyRestrictionImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            logger.error(
                "ProxyRestrictionImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "ProxyRestrictionImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(PROXY_RESTRICTION_ELEMENT)) {
            logger.error(
                "ProxyRestrictionImpl.processElement(): "
                + "invalid local name " + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(COUNT_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                setCount(Integer.parseInt(attrValue));
            } catch (NumberFormatException e) {
                logger.error(
                    "ProxyRestrictionImpl.processElement(): "
                    + "count is not an integer " + attrValue);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_count_number"));
            }
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            return;
        }

        int nextElem = 0;
        while (nextElem < numOfNodes) {
            Node child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(AUDIENCE_ELEMENT)) {
                        audiences.add(
                            XMLUtils.getElementValue((Element)child));
                    } else {
                        logger.error(
                            "AudienceRestrictionImpl.processElement(): "
                            + "unexpected subelement " + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.
                            getString("unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
    }

    /**
     *  Returns the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @return the count number
     */
    public int getCount() {
        return count;
    }

    /**
     *  Sets the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @param value the count number
     *  @exception SAML2Exception if the object is immutable
     */
    public void setCount(int value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        if (count < 0) {
            logger.error(
                "AudienceRestrictionImpl.setCount(): count is negative");
            throw new SAML2Exception(SAML2SDKUtils.bundle.
                 getString("negative_count_number"));
        }
        count = value;
    }

    /**
     *  Returns the list of audiences to whom the asserting party
     *  permits new assertions to be issued on the basis of this
     *  assertion.
     *
     *  @return a list of <code>String</code> represented audiences
     */
    public List<String> getAudience() {
        return audiences;
    }

    /**
     *  Sets the list of audiences to whom the asserting party permits
     *  new assertions to be issued on the basis of this assertion.
     *
     *  @param audiences a list of <code>String</code> represented audiences
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAudience(List<String> audiences) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        this.audiences = audiences;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element proxyRestrictionElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX,
                ASSERTION_NAMESPACE_URI, PROXY_RESTRICTION_ELEMENT, includeNSPrefix, declareNS);
        fragment.appendChild(proxyRestrictionElement);
        if (count >= 0) {
            proxyRestrictionElement.setAttribute(COUNT_ATTR, String.valueOf(count));
        }
        if (isNotEmpty(audiences)) {
            for (String audience : audiences) {
                String prefix = includeNSPrefix ? ASSERTION_PREFIX : "";
                Element audienceElement = document.createElement(prefix + AUDIENCE_ELEMENT);
                audienceElement.setTextContent(audience);
                proxyRestrictionElement.appendChild(audienceElement);
            }
        }
        return fragment;
    }

   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        if (isMutable) {
            if (audiences != null) {
                audiences = Collections.unmodifiableList(audiences);
            }
            isMutable = false;
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable() {
        return isMutable;
    }
}
