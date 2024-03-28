/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AssertionIDRefImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2019-2023 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_ID_REF;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class represents the AssertionIDRef element.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;element name="AssertionIDRef" type="NCName"/&gt;
 * </pre>
 *
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class AssertionIDRefImpl implements AssertionIDRef {
    
    private static final Logger logger = LoggerFactory.getLogger(AssertionIDRefImpl.class);
    private String value;
    private boolean mutable = true;

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AssertionIDRefImpl() {
    }

    /**
     * Class constructor with <code>AssertionIDRef</code> in
     * <code>Element</code> format.
     *
     * @param element A <code>Element</code> representing DOM tree for
     *     <code>AssertionIDRef</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public AssertionIDRefImpl(Element element) throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AssertionIDRef</code> in xml string format.
     *
     * @param xmlString A <code>String</code> representing a
     *     <code>AssertionIDRef</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public AssertionIDRefImpl(String xmlString) throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Returns the value of the <code>AssertionIDRef</code>.
     *
     * @return the value of this <code>AssertionIDRef</code>.
     * @see #setValue(String)
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value of this <code>AssertionIDRef</code>.
     *
     * @param value new <code>AssertionIDRef</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    public void setValue(String value) throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "objectImmutable"));
        }
        this.value = value;
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return true if the object is mutable; false otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        if (isBlank(value)) {
            if (logger.isDebugEnabled()) {
                logger.debug("AssertionIDRefImpl.toDocumentFragment: AssertionIDRef value is null or empty.");
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString("emptyElementValue"));
        }
        DocumentFragment fragment = document.createDocumentFragment();
        Element assertionIDRefElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX,
                ASSERTION_NAMESPACE_URI, ASSERTION_ID_REF, includeNSPrefix, declareNS);
        fragment.appendChild(assertionIDRefElement);
        assertionIDRefElement.setTextContent(value);
        return fragment;
    }

    private void parseElement(Element element) throws SAML2Exception
    {

        if (element == null) {
            logger.debug("AssertionIDRefImpl.parseElement:"+
                " Input is null.");
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullInput"));
        }

        String tag = element.getLocalName();
        if (!SAML2Constants.ASSERTION_ID_REF.equals(tag)) {
            logger.debug("AssertionIDRefImpl.parseElement: " +
                "Element local name is not AssertionIDRef.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("wrongInput"));
        }

        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "AssertionIDRefImpl.parseElement: " +
                            "AssertionIDRef can't have child element.");
                    }
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "wrongInput"));
                }
            }
        }

        value = XMLUtils.getElementValue(element);
        if ((value == null) || (value.trim().length() == 0)) {
            if (logger.isDebugEnabled()) {
                logger.debug("AssertionIDRefImpl.parseElement: " +
                    "AssertionIDRef value is null or empty.");
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "emptyElementValue"));
        }
        mutable = false;
    }
}
