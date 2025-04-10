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
 * $Id: ActionImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.Action;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class is an implementation of interface <code>Action</code>.
 * The <code>Action</code> element specifies an action on the specified
 * resource for which permission is sought. Its type is <code>ActionType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ActionType"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string"&gt;
 *       &lt;attribute name="Namespace" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
public class ActionImpl implements Action {

    private static final Logger logger = LoggerFactory.getLogger(ActionImpl.class);
    private String action = null;
    private String namespace = null;
    private boolean mutable = true;

    // used by constructors
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            logger.debug("ActionImpl.parseElement:"+
                " Input is null.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an Action.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("Action"))) {
            logger.debug("ActionImpl.parseElement: not Action.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attribute of <Action> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            Node att = atts.getNamedItem("Namespace");
            if (att != null) {
                namespace = ((Attr)att).getValue().trim();
            }
        }
        if (namespace == null || namespace.length() == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("ActionImpl.parseElement: "+
                    "Namespace is empty or missing.");
            }
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("missingAttribute"));
        }

        //handle the children elements of <Action>
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ActionImpl.parseElement: "
                            + "Illegal value of the element.");
                    }
                    throw new SAML2Exception(
                              SAML2SDKUtils.bundle.getString("wrongInput"));
                }
            }
        }
        action = XMLUtils.getElementValue(element);
        // check if the action is null.
        if (action == null || action.trim().length() == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("ActionImpl.parseElement: "+
                    "Action value is null or empty.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("missingElementValue"));
        }

        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public ActionImpl() {
    }

    /**
     * Class constructor with <code>Action</code> in <code>Element</code>
     * format.
     */
    public ActionImpl(org.w3c.dom.Element element)
        throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>Action</code> in xml string format.
     */
    public ActionImpl(String xmlString)
        throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
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

    /**
     * Returns the value of the value property.
     *
     * @return A String label for the action.
     * @see #setValue(String)
     */
    public String getValue() {
        return action;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value A String lable for the action to be set.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    public void setValue(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        action = value;
    }

    /**
     * Returns the value of the <code>Namespace</code> property.
     *
     * @return A String representing <code>Namespace</code> attribute.
     * @see #setNamespace(String)
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the value of the <code>Namespace</code> property.
     *
     * @param value A String representing <code>Namespace</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNamespace()
     */
    public void setNamespace(java.lang.String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        namespace = value;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        if (isBlank(action)) {
            logger.debug("ActionImpl.toDocumentFragment: Action value is null or empty.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("emptyElementValue"));
        }

        if (isBlank(namespace)) {
            logger.debug("ActionImpl.toDocumentFragment: Namespace is empty or missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingAttribute"));
        }


        DocumentFragment fragment = document.createDocumentFragment();
        Element actionElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                "Action", includeNSPrefix, declareNS);
        fragment.appendChild(actionElement);

        actionElement.setAttribute("Namespace", namespace);
        actionElement.setTextContent(action);

        return fragment;
    }
}
