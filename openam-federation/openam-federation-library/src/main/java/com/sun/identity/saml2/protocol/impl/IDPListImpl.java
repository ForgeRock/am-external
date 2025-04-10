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
 * $Id: IDPListImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.IDPLIST;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import com.sun.identity.saml2.protocol.GetComplete;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class specifies the identity providers trusted by the requester
 * to authenticate the presenter.
 */

public class IDPListImpl implements IDPList {

    private static final Logger logger = LoggerFactory.getLogger(IDPListImpl.class);
    private List<IDPEntry> idpEntryList = null;
    private GetComplete getComplete;
    private boolean isMutable=false;

    /**
     * Constructor creates the <code>IDPList</code> Object.
     */

    public IDPListImpl() {
        isMutable=true;
    }

    /**
     * Constructor to create the <code>IDPList</code> Object.
     *
     * @param element Document Element of <code>IDPList</code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */

    public IDPListImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructor to create the <code>IDPList</code> Object.
     *
     * @param xmlString the XML String Representation of
     *        <code>IDPList</code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */

    public IDPListImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
                XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the list of <code>IDPEntry</code> Objects.
     *
     * @return the list of <code>IDPEntry</code> objects.
     * @see #setIDPEntries(List)
     */
    public List<IDPEntry> getIDPEntries() {
        return idpEntryList ;
    }

    /**
     * Sets the list of <code>IDPEntry</code> Objects.
     *
     * @param idpEntryList list of <code>IDPEntry</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPEntries
     */
    public void setIDPEntries(List<IDPEntry> idpEntryList) throws SAML2Exception {
        if (isMutable) {
            this.idpEntryList = idpEntryList;
        } else {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @return the <code>GetComplete</code> object.
     * @see #setGetComplete(GetComplete)
     */
    public GetComplete getGetComplete() {
        return getComplete;
    }

    /**
     * Sets the <code>GetComplete<code> Object.
     *
     * @param getComplete the new <code>GetComplete</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getGetComplete
     */

    public void setGetComplete(GetComplete getComplete) throws SAML2Exception {
        if (isMutable) {
            this.getComplete = getComplete;
        } else {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateIDPEntryList(idpEntryList);

        DocumentFragment fragment = document.createDocumentFragment();
        Element idpListElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE, IDPLIST,
                includeNSPrefix, declareNS);
        fragment.appendChild(idpListElement);

        for (IDPEntry idpEntry : idpEntryList) {
            idpListElement.appendChild(idpEntry.toDocumentFragment(document, includeNSPrefix, false));
        }
        if (getComplete != null) {
            idpListElement.appendChild(getComplete.toDocumentFragment(document, includeNSPrefix, false));
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable()  {
	if (isMutable) {
	    if ((idpEntryList != null) && (!idpEntryList.isEmpty())) {
		Iterator i = idpEntryList.iterator();
		while (i.hasNext()) {
		    IDPEntry idpEntry = (IDPEntry) i.next();
		    if ((idpEntry != null) && (idpEntry.isMutable())) {
			idpEntry.makeImmutable();
		    }
		}
	    }

	    if ((getComplete != null) && (getComplete.isMutable())) {
		getComplete.makeImmutable();
	    }
            isMutable=false;
	}
    }

    /**
     * Returns true if object is mutable.
     *
     * @return true if the object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }

    /* Parse the IDPList Element */
    void parseElement(Element element) throws SAML2Exception {

        ProtocolFactory protoFactory = ProtocolFactory.getInstance();

        // Get the IDPEntry Element, can be 1 or more
	NodeList nList = element.getChildNodes();

        if ((nList == null) || (nList.getLength()==0)) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noIDPEntry"));
        }
	if (idpEntryList == null) {
	    idpEntryList = new ArrayList<>();
	}
	for (int i = 0; i < nList.getLength(); i++) {
            Node childNode = nList.item(i);
	    String cName = childNode.getLocalName();
            if (cName != null)  {
		if (cName.equals(SAML2Constants.IDPENTRY)) {
		    validateIDPEntry();
		    idpEntryList.add(
		        protoFactory.createIDPEntry(XMLUtils.print(childNode)));
                } else if (cName.equals(SAML2Constants.GETCOMPLETE)) {
			validateGetComplete();
                	Element getCompleteElement = (Element)childNode;
			getComplete =
		    	   protoFactory.createGetComplete(getCompleteElement);
		}
	    }
	}
	validateIDPEntryList(idpEntryList);
	idpEntryList=Collections.unmodifiableList(idpEntryList);
    }

    /* Validates the existance of IDPEntries */
    private void validateIDPEntryList(List<IDPEntry> idpEntryList) throws SAML2Exception {
        if ((idpEntryList == null) || (idpEntryList.isEmpty())) {
            logger.debug("IDPEntry Object is required");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noIDPEntry"));
        }
    }

    /* Validates the IDPEntry sequence */
    private void validateIDPEntry() throws SAML2Exception {
	if (getComplete != null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("IDPList Element should be the "
				     + "first element" );
	    }
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidProxyCount"));
        }
    }

    /* Validate the existance of GetComplete Object. */
    private void validateGetComplete() throws SAML2Exception {
	if (getComplete != null) {
            logger.debug("Too may GetComplete Elements");
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }
}
