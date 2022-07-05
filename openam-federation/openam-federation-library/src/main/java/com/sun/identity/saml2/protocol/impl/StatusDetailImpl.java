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
 * $Id: StatusDetailImpl.java,v 1.2 2008/06/25 05:48:01 qcheng Exp $
 *
 * Portions Copyrighted 2018-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.STATUS_DETAIL;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.StatusDetail;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class defines methods for adding <code>StatusDetail</code> elements.
 */

public class StatusDetailImpl implements StatusDetail {

    private boolean isMutable = false;
    private List<String> statusDetailList = null;

    /**
     * Constructs the <code>StatusDetail</code> Object.
     *
     */
    public StatusDetailImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>StatusDetail</code> Object.
     *
     * @param element the Document Element of <code>StatusDetail</code> object.
     * @throws SAML2Exception if <code>StatusDetail</code> cannot be created.
     */

    public StatusDetailImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>StatusDetail</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>StatusDetail</code> cannot be created.
     */
    public StatusDetailImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Sets the <code>StatusDetail</code> object.
     *
     * @param value List of XML Strings <code>StatusDetail</code> objects
     * @throws SAML2Exception if the object is immutable.
     * @see #getAny()
     */
    public void setAny(List<String> value) throws SAML2Exception {
        if (isMutable) {
            this.statusDetailList = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Returns the list of <code>StatusDetail</code> object.
     *
     * @return a List of XML Strings <code>StatusDetail</code> objects.
     * @see #setAny(List)
     */
    public List<String> getAny() {
        return statusDetailList;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        if (isNotEmpty(statusDetailList)) {
            Element detailElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                    STATUS_DETAIL, includeNSPrefix, declareNS);
            fragment.appendChild(detailElement);
            for (String detail : statusDetailList) {
                List<Node> parsed = SAML2Utils.parseSAMLFragment(detail);
                for (Node node : parsed) {
                    detailElement.appendChild(document.adoptNode(node));
                }
            }
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            isMutable = false;
        }
    }

    /**
     * Returns value true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }

    /* Parses the <code>StatusDetail</code> Element. */
    private void parseElement(Element element) {
        NodeList nList = element.getChildNodes();
        if ((statusDetailList == null) || (statusDetailList.isEmpty())) {
            statusDetailList = new ArrayList();
        }
        if ((nList != null) && (nList.getLength() > 0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getLocalName() != null) {
                    statusDetailList.add(XMLUtils.print(childNode));
                }
            }
            if ((statusDetailList != null) && (!statusDetailList.isEmpty())) {
                statusDetailList =
                Collections.unmodifiableList(statusDetailList);
            }
        }
    }
}
