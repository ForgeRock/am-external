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
 * $Id: ExtensionsImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.EXTENSIONS;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static java.util.stream.Collectors.toList;
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
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.shared.xml.XMLUtils;

/** 
 * The class defines methods for adding protcol message extension elements.
 */

public class ExtensionsImpl implements Extensions {
    
    private boolean isMutable=false;
    private List<Node> extensionsList = null;

    /**
     * Constructor to create the <code>Extensions</code> Object.
     *
     */
    public ExtensionsImpl() {
	isMutable=true;	
    }

    /**
     * Constructor to create the <code>Extensions</code> Object.
     *
     * @param element the Document Element of <code>Extensions</code> object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */

    public ExtensionsImpl(Element element) throws SAML2Exception {
	parseElement(element);
    }

    /**
     * Constructor to create the <code>Extensions</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    public ExtensionsImpl(String xmlString) throws SAML2Exception {
	Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString);
	if (xmlDocument == null) {
            throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
	}
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Sets the <code>Extensions</code> object.
     *
     * @param value List of XML Strings <code>Extensions</code> objects
     * @throws SAML2Exception if the object is immutable.
     * @see #getAny()
     */
    public void setAny(List value) throws SAML2Exception {
		if (isMutable) {
			if (value != null) {
				List<Node> extensions = new ArrayList<>(value.size());
				for (Object obj : value) {
					extensions.addAll(SAML2Utils.parseSAMLFragment((String) obj));
				}
				extensionsList = extensions;
			} else {
				extensionsList = null;
			}
		} else {
			throw new SAML2Exception(
				SAML2SDKUtils.bundle.getString("objectImmutable"));
		}
    }
		
    /** 
     * Returns the list of <code>Extensions</code> object.
     *
     * @return a List of XML Strings <code>Extensions</code> objects.
     * @see #setAny(List)
     */
    public List getAny() {
    	if (extensionsList != null) {
    		return extensionsList.stream().map(XMLUtils::print).collect(toList());
		} else {
    		return null;
		}
    }

	@Override
	public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
			throws SAML2Exception {
    	DocumentFragment fragment = document.createDocumentFragment();

		if (isNotEmpty(extensionsList)) {
			Element extensionsElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
					EXTENSIONS, includeNSPrefix, declareNS);
			fragment.appendChild(extensionsElement);

			for (Node node : extensionsList) {
				extensionsElement.appendChild(document.importNode(node, true));
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

    /* Parses the Extensions Element. */
    private void parseElement(Element element) {
	NodeList nList = element.getChildNodes();
	if ((extensionsList == null) || (extensionsList.isEmpty())) {
	    extensionsList = new ArrayList();
	}
	if ((nList != null) && (nList.getLength() > 0)) {
	    for (int i = 0; i < nList.getLength(); i++) {
		Node childNode = nList.item(i);
	        if (childNode.getLocalName() != null) {
		    extensionsList.add(childNode);
		}
	    }
	    if ((extensionsList != null) && (!extensionsList.isEmpty())) {
		extensionsList = Collections.unmodifiableList(extensionsList);
	    }
	}
    }
}
