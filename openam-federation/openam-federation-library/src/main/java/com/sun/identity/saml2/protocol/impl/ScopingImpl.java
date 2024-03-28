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
 * $Id: ScopingImpl.java,v 1.5 2009/03/12 20:32:41 huacui Exp $
 *
 * Portions Copyrighted 2014-2023 ForgeRock AS.
 */

package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.SCOPING;
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

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequesterID;
import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This class defines methods to retrieve Identity Providers and context/limitations related to proxying of the request
 * message.
 */

@SupportedAll(scriptingApi = true, javaApi = false)
public class ScopingImpl implements Scoping {

    private static final Logger logger = LoggerFactory.getLogger(ScopingImpl.class);
    private static final String PROXYCOUNT = "ProxyCount";
    private IDPList idpList;
    private List<RequesterID> requesterIDList = null;
    private boolean isMutable = false;
    private Integer proxyCount;


    /**
     * Constructor to create Scoping object.
     */
    public ScopingImpl() {
	isMutable=true;
    }

    /**
     * Constructor to create the <code>Scoping</code> Object.
     *
     * @param element Document Element of <code>Scoping</code> Object.
     * @throws SAML2Exception if <code>Scoping<code> cannot be created.
     */

    public ScopingImpl(Element  element) throws SAML2Exception {
	parseElement(element);
    }

    /**
     * Constructor to create the <code>Scoping</code> Object.
     *
     * @param xmlString XML String Representation of <code>Scoping</code>
     *        Object.
     * @throws SAML2Exception if <code>Scoping<code> cannot be created.
     */

    public ScopingImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument = XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the <code>IDPList</code> Object.
     *
     * @return the <code>IDPList</code> object.
     * @see #setIDPList(IDPList)
     */
    public IDPList getIDPList() {
	return idpList;
    }

    /**
     * Sets the <code>IDPList</code> Object.
     *
     * @param idpList the new <code>IDPList</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPList
     */
    public void setIDPList(IDPList idpList) throws SAML2Exception {
	if (isMutable) {
	    this.idpList = idpList;
	 } else {
	    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    /**
     * Returns a list of <code>RequesterID</code> Objects.
     *
     * @return list of <code>RequesterID</code> objects.
     * @see #setRequesterIDs(List)
     */
    public List<RequesterID> getRequesterIDs() {
	return requesterIDList;
    }

    /**
     * Sets the <code>RequesterID</code> Object.
     *
     * @param value list of <code>RequesterID</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequesterIDs
     */
    public void setRequesterIDs(List<RequesterID> value) throws SAML2Exception {
	if (isMutable) {
	    requesterIDList = value;
	} else {
	    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    /**
     * Returns the value of <code>ProxyCount</code> attribute.
     *
     * @return the value of <code>ProxyCount</code> attribute value.
     */
    public Integer getProxyCount() {
	return proxyCount;
    }

    /**
     * Sets the value of <code>ProxyCount</code> attribute.
     *
     * @param proxyCount new value of <code>ProxyCount</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */

    public void setProxyCount(Integer proxyCount) throws SAML2Exception {
	if (isMutable) {
	    this.proxyCount = proxyCount;
	} else {
	    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();

        if (idpList != null || isNotEmpty(requesterIDList) || proxyCount != null) {
            Element scopingElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                    SCOPING, includeNSPrefix, declareNS);
            fragment.appendChild(scopingElement);

            if (proxyCount != null) {
                scopingElement.setAttribute(PROXYCOUNT, proxyCount.toString());
            }
            if (idpList != null) {
                scopingElement.appendChild(idpList.toDocumentFragment(document, includeNSPrefix, false));
            }
            if (isNotEmpty(requesterIDList)) {
                for (RequesterID requesterID : requesterIDList) {
                    scopingElement.appendChild(requesterID.toDocumentFragment(document, includeNSPrefix, false));
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
            if (idpList != null) {
                idpList.makeImmutable();
            }
            if (requesterIDList != null) {
                for (RequesterID reqID : requesterIDList) {
                    reqID.makeImmutable();
                }
            }
            isMutable = false;
        }
    }

    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable(){
	return isMutable;
    }


    private void parseElement(Element element) throws SAML2Exception {

	String proxyCountStr =  element.getAttribute("ProxyCount");
	if (proxyCountStr != null && proxyCountStr.length() > 0) {
	    proxyCount = new Integer(proxyCountStr);
	    validateProxyCount(proxyCount);
        }

	NodeList nList = element.getChildNodes();
	if ((nList != null) && (nList.getLength()>0)) {
            if (requesterIDList == null) {
                requesterIDList = new ArrayList<RequesterID>();
            }
	    for (int i = 0; i < nList.getLength(); i++) {
		Node childNode = nList.item(i);
		String cName = childNode.getLocalName();
                if (cName != null) {
                    if (cName.equals(SAML2Constants.IDPLIST)) {
                        validateIDPList();
                        idpList = ProtocolFactory.getInstance().createIDPList((Element) childNode);
                    } else if (cName.equals(SAML2Constants.REQUESTERID)) {
                        RequesterID reqID = ProtocolFactory.getInstance().createRequesterID((Element) childNode);
                        requesterIDList.add(reqID);
                    }
                }
	    }
            if (requesterIDList != null && !requesterIDList.isEmpty()) {
                requesterIDList = Collections.unmodifiableList(requesterIDList);
            }
	}
    }

   /* validate the sequence of IDPList element */
    private void validateIDPList() throws SAML2Exception {
        if (idpList != null) {
            logger.debug("Too many IDPList Elements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
        if (requesterIDList != null && !requesterIDList.isEmpty()) {
            logger.debug("IDPList should be the first element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    /* validate the proxy count  */
    private void validateProxyCount(Integer proxyCount) throws SAML2Exception {
	if (proxyCount != null
                && (proxyCount.intValue() < 0 || proxyCount.intValue() > SAML2Constants.MAX_INT_VALUE)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("ProxyCount value should be a nonnegative Integer");
	    }
	    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("invalidProxyCount"));
	}
    }
}
