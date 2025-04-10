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
 * $Id: ArtifactResolveImpl.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */



package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.CONSENT;
import static com.sun.identity.saml2.common.SAML2Constants.DESTINATION;
import static com.sun.identity.saml2.common.SAML2Constants.ID;
import static com.sun.identity.saml2.common.SAML2Constants.ISSUE_INSTANT;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.VERSION;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.ArtifactResolve;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This is an implementation of interface <code>ArtifactResolve</code>.
 *
 * The <code>ArtifactResolve</code> message is used to request that a SAML
 * protocol message be returned in an <code>ArtifactResponse</code> message
 * by specifying an artifact that represents the SAML protocol message.
 * It has the complex type <code>ArtifactResolveType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResolveType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Artifact"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class ArtifactResolveImpl extends RequestAbstractImpl
	implements ArtifactResolve {

	private static final Logger logger = LoggerFactory.getLogger(ArtifactResolveImpl.class);
	public static final String ARTIFACT_RESOLVE = "ArtifactResolve";
	private Artifact artifact = null;
    private boolean validationDone = false;
    
    protected void validateData() throws SAML2Exception {
	super.validateData();
	if (artifact == null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("ArtifactResolveImpl.validateData: "
		    + "missing Artifact.");
	    }
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("missingElement"));
	}
    }

    private void parseElement(Element element)
        throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
	    if (logger.isDebugEnabled()) {
        	logger.debug("ArtifactResolveImpl.parseElement: "
		    + "element input is null.");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an ArtifactResolve.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("ArtifactResolve"))) {
	    if (logger.isDebugEnabled()) {
        	logger.debug("ArtifactResolveImpl.parseElement: "
		    + "not ArtifactResolve.");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <ArtifactResolve> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
	    int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("ID")) {
                    requestId = attrValue;
                } else if (attrName.equals("Version")) {
                    version = attrValue;
                } else if (attrName.equals("IssueInstant")) {
		    try {
			issueInstant = DateUtils.stringToDate(attrValue);
		    } catch (ParseException pe) {
			throw new SAML2Exception(pe.getMessage());
		    }
                } else if (attrName.equals("Destination")) {
		    destinationURI = attrValue;
                } else if (attrName.equals("Consent")) {
		    consent = attrValue;
		}
            }
        }

	// handle child elements
	NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Issuer")) {
		    if (nameID != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"
                                + "Element: included more than one Issuer.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (signatureString != null ||
			extensions != null ||
			artifact != null)
		    {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    nameID = AssertionFactory.getInstance().createIssuer(
			(Element) child);
		} else if (childName.equals("Signature")) {
		    if (signatureString != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"
                                + "Element:included more than one Signature.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (extensions != null || artifact != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    signatureString = XMLUtils.print((Element) child);
		    isSigned = true;
		} else if (childName.equals("Extensions")) {
		    if (extensions != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"
                                + "Element:included more than one Extensions.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (artifact != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    extensions = ProtocolFactory.getInstance().createExtensions(
			(Element) child);
		} else if (childName.equals("Artifact")) {
		    if (artifact != null) {
			if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResolveImpl.parse"
                                + "Element: included more than one Artifact.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    artifact = ProtocolFactory.getInstance().createArtifact(
			(Element) child);
		} else {
		    if (logger.isDebugEnabled()) {
                        logger.debug("ArtifactResolveImpl.parse"
                            + "Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
		}
	    }
	}

	validateData();
    }

    /**
     * Constructor. Caller may need to call setters to populate the
     * object.
     */
    public ArtifactResolveImpl() {
    	super(ARTIFACT_RESOLVE);
		isMutable = true;
    }

    /**
     * Class constructor with <code>ArtifactResolve</code> in
     * <code>Element</code> format.
     *
     * @param element the Document Element
     * @throws SAML2Exception if there is an creating the object.
     */
    public ArtifactResolveImpl(org.w3c.dom.Element element)
        throws SAML2Exception {
		super(ARTIFACT_RESOLVE);
		parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Class constructor with <code>ArtifactResolve</code> in xml string format.
     */
    public ArtifactResolveImpl(String xmlString)
        throws SAML2Exception {
		super(ARTIFACT_RESOLVE);
		Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Gets the <code>Artifact</code> of the request.
     *
     * @return <code>Artifact</code> of the request.
     * @see #setArtifact(Artifact)
     */
    public Artifact getArtifact() {
	return artifact;
    }

    /**
     * Sets the <code>Artifact</code> of the request.
     * 
     * @param value new <code>Artifact</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getArtifact()
     */
    public void setArtifact(Artifact value)
	throws SAML2Exception {
	if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	artifact = value;
    }

	@Override
	public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
			throws SAML2Exception {
		this.validateData();
		DocumentFragment fragment = document.createDocumentFragment();

		if (isSigned && signedXMLString != null) {
			Document doc = XMLUtils.toDOMDocument(signedXMLString);
			if (doc == null) {
				throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
			}
			fragment.appendChild(document.importNode(doc.getDocumentElement(), true));
			return fragment;
		}

		Element artifactResolveElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
				"ArtifactResolve", includeNSPrefix, declareNS);
		fragment.appendChild(artifactResolveElement);

		artifactResolveElement.setAttribute(ID, requestId);
		artifactResolveElement.setAttribute(VERSION, version);
		artifactResolveElement.setAttribute(ISSUE_INSTANT, DateUtils.toUTCDateFormat(issueInstant));

		if (isNotBlank(destinationURI)) {
			artifactResolveElement.setAttribute(DESTINATION, destinationURI);
		}
		if (isNotBlank(consent)) {
			artifactResolveElement.setAttribute(CONSENT, consent);
		}
		if (nameID != null) {
			artifactResolveElement.appendChild(nameID.toDocumentFragment(document, includeNSPrefix, declareNS));
		}
		if (signatureString != null) {
			List<Node> sigNodes = SAML2Utils.parseSAMLFragment(signatureString);
			for (Node node : sigNodes) {
				artifactResolveElement.appendChild(document.importNode(node, true));
			}
		}
		if (extensions != null) {
			artifactResolveElement.appendChild(extensions.toDocumentFragment(document, includeNSPrefix, declareNS));
		}
		artifactResolveElement.appendChild(artifact.toDocumentFragment(document, includeNSPrefix, declareNS));
		return fragment;
	}
}
