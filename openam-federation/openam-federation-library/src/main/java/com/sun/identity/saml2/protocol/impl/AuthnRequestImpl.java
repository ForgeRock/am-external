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
 * $Id: AuthnRequestImpl.java,v 1.8 2009/06/09 20:28:32 exu Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_CONSUMER_SVC_INDEX;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_CONSUMER_SVC_URL;
import static com.sun.identity.saml2.common.SAML2Constants.ATTR_CONSUMING_SVC_INDEX;
import static com.sun.identity.saml2.common.SAML2Constants.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.CONSENT;
import static com.sun.identity.saml2.common.SAML2Constants.DESTINATION;
import static com.sun.identity.saml2.common.SAML2Constants.FORCEAUTHN;
import static com.sun.identity.saml2.common.SAML2Constants.ID;
import static com.sun.identity.saml2.common.SAML2Constants.ISPASSIVE;
import static com.sun.identity.saml2.common.SAML2Constants.ISSUE_INSTANT;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOBINDING;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_PREFIX;
import static com.sun.identity.saml2.common.SAML2Constants.PROVIDER_NAME;
import static com.sun.identity.saml2.common.SAML2Constants.VERSION;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * The <code>AuthnRequestImpl</code> implements the
 * </code>AuthnRequest</code> interface , defines methods for
 * properties required by a saml request.
 */

@SupportedAll(scriptingApi = true, javaApi = false)
public class AuthnRequestImpl extends RequestAbstractImpl
			      implements AuthnRequest {
    private static final Logger logger = LoggerFactory.getLogger(AuthnRequestImpl.class);
    private Subject subject;
    private NameIDPolicy nameIDPolicy;
    private Conditions conditions;
    private RequestedAuthnContext reqAuthnContext;
    private Scoping scoping;
    private Integer assertionConsumerSvcIndex;
    private Integer attrConsumingSvcIndex;
    private String providerName;
    private Boolean forceAuthn;
    private Boolean isPassive;
    private String protocolBinding;
    private String assertionConsumerServiceURL;

    /**
     * Constructor to create <code>AuthnRequest</code> Object .
     */

    public AuthnRequestImpl() {
        super(AUTHN_REQUEST);
	    isMutable = true;
    }

    /**
     * Constructor to create <code>AuthnRequest</code> Object.
     *
     * @param element the Document Element Object.
     * @throws SAML2Exception if error creating <code>AuthnRequest</code>
     *         Object.
     */
    public AuthnRequestImpl(Element element) throws SAML2Exception {
        super(AUTHN_REQUEST);
        parseDOMElement(element);
	    if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Constructor to create <code>AuthnRequest</code> Object.
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>AuthnRequest</code>
     *         Object.
     */
    public AuthnRequestImpl(String xmlString) throws SAML2Exception {
        super(AUTHN_REQUEST);
        Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString);
        if (xmlDocument == null) {
            throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseDOMElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Returns the <code>Subject</code> object.
     *
     * @return the <code>Subject</code> object.
     * @see #setSubject(Subject)
     */
    public Subject getSubject() {
	return subject;
    }

    /**
     * Sets the <code>Subject</code> object.
     *
     * @param subject the new <code>Subject</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSubject
     */
    public void setSubject(Subject subject) throws SAML2Exception {
         if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
	this.subject = subject;
    }

    /**
     * Returns the <code>NameIDPolicy</code> object.
     *
     * @return the <code>NameIDPolicy</code> object.
     * @see #setNameIDPolicy(NameIDPolicy)
     */
    public NameIDPolicy getNameIDPolicy() {
	return nameIDPolicy;
    }

    /**
     * Sets the <code>NameIDPolicy</code> object.
     *
     * @param nameIDPolicy the new <code>NameIDPolicy</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNameIDPolicy
     */

    public void setNameIDPolicy(NameIDPolicy nameIDPolicy)
    throws SAML2Exception {
	if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	this.nameIDPolicy = nameIDPolicy;
    }

    /**
     * Returns the <code>Conditions</code> object.
     *
     * @return the <code>Conditions</code> object.
     * @see #setConditions(Conditions)
     */
    public Conditions getConditions() {
	return conditions;
    }

    /**
     * Sets the <code>Conditions</code> object.
     *
     * @param conditions the new <code>Conditions</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getConditions
     */

    public void setConditions(Conditions conditions) throws SAML2Exception {
       if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	this.conditions = conditions;
    }

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * @return the <code>RequestAuthnContext</code> object.
     * @see #setRequestedAuthnContext(RequestedAuthnContext)
     */

    public RequestedAuthnContext getRequestedAuthnContext() {
	return reqAuthnContext;
    }

    /**
     * Sets the <code>RequestedAuthnContext</code>.
     *
     * @param reqAuthnContext the new <code>RequestedAuthnContext</code>
     *        object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequestedAuthnContext
     */
    public void setRequestedAuthnContext(RequestedAuthnContext reqAuthnContext)
    throws SAML2Exception {
	if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
	this.reqAuthnContext = reqAuthnContext;
    }

    /**
     * Sets the <code>Scoping</code> object.
     *
     * @param scoping the new <code>Scoping</code> Object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getScoping
     */
    public void setScoping(Scoping scoping) throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	this.scoping = scoping;
    }

    /**
     * Returns the <code>Scoping</code> object.
     *
     * @return the <code>Scoping</code> object.
     * @see #setScoping(Scoping)
     */
    public Scoping getScoping() {
        return scoping;
    }

    /**
     * Returns value of <code>isForceAuthn</code> attribute.
     *
     * @return value of <code>isForceAuthn</code> attribute.
     */
    public Boolean isForceAuthn() {
        return forceAuthn;
    }

    /**
     * Sets the value of the <code>ForceAuthn</code> attribute.
     *
     * @param value the value of <code>ForceAuthn</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setForceAuthn(Boolean value) throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	forceAuthn = value;
    }

    /**
     * Returns the value of the <code>isPassive</code> attribute.
     *
     * @return value of <code>isPassive</code> attribute.
     */
    public Boolean isPassive() {
	return isPassive;
    }

    /**
     * Sets the value of the <code>IsPassive</code> attribute.
     *
     * @param value Value of <code>IsPassive</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setIsPassive(Boolean value) throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	isPassive = value;
    }

    /**
     * Sets the value of the <code>ProtocolBinding</code> attribute.
     *
     * @param protocolBinding value of the <code>ProtocolBinding</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProtocolBinding
     */
    public void setProtocolBinding(String protocolBinding)
    throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	this.protocolBinding = protocolBinding;
    }

    /**
     * Returns the value of the <code>ProtocolBinding</code> attribute.
     *
     * @return the value of <code>ProtocolBinding</code> attribute.
     * @see #setProtocolBinding(String)
     */
    public String getProtocolBinding() {
        return protocolBinding;
    }

    /**
     * Returns the value of the <code>AssertionConsumerServiceURL</code>
     * attribute.
     *
     * @return the value of <code>AssertionConsumerServiceURL</code> attribute.
     * @see #setAssertionConsumerServiceURL(String)
     */
    public String getAssertionConsumerServiceURL() {
        return assertionConsumerServiceURL;
    }

    /**
     * Sets the value of the <code>AssertionConsumerServiceURL</code>
     * attribute.
     *
     * @param url the value of <code>AssertionConsumerServiceURL</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionConsumerServiceURL()
     */
    public void setAssertionConsumerServiceURL(String url)
	    throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	assertionConsumerServiceURL = url;
    }

    /**
     * Returns the value of the <code>AssertionConsumerServiceIndex</code>
     * attribute.
     *
     * @return value of the <code>AssertionConsumerServiceIndex<code>
     *         attribute.
     * @see #setAssertionConsumerServiceIndex(Integer)
     */
    public Integer getAssertionConsumerServiceIndex() {
        return assertionConsumerSvcIndex;
    }

    /**
     * Sets the value of the <code>AssertionConsumerServiceIndex</code>
     * attribute.
     *
     * @param index value of the <code>AssertionConsumerServiceIndex</code>
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionConsumerServiceIndex
     */
    public void setAssertionConsumerServiceIndex(Integer index)
    throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	assertionConsumerSvcIndex = index ;
    }


    /**
     * Returns the value of the <code>AttributeConsumingServiceIndex</code>
     * attribute.
     *
     * @return value of the <code>AttributeConsumingServiceIndex<code>
     *		attribute.
     * @see #setAttributeConsumingServiceIndex(Integer)
     */
    public Integer getAttributeConsumingServiceIndex() {
	return attrConsumingSvcIndex;
    }

    /**
     * Sets the value of the <code>AttributeConsumingServiceIndex</code>
     * attribute.
     *
     * @param index value of the <code>AttributeConsumingServiceIndex</code>
     * attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributeConsumingServiceIndex
     */
    public void setAttributeConsumingServiceIndex(Integer index)
    throws SAML2Exception {
	if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
	attrConsumingSvcIndex = index ;
     }


    /**
     * Sets the <code>ProviderName</code> attribute value.
     *
     * @param providerName value of the <code>ProviderName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProviderName
     */
    public void setProviderName(String providerName) throws SAML2Exception {
        if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	this.providerName = providerName ;
    }

    /**
     * Returns the <code>ProviderName</code> attribute value.
     *
     * @return value of the <code>ProviderName</code> attribute value.
     * @see #setProviderName(String)
     */
    public String getProviderName() {
        return providerName;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        validateData();
        validateAssertionConsumerServiceIndex(assertionConsumerSvcIndex);
        validateAttributeConsumingServiceIndex(attrConsumingSvcIndex);

        DocumentFragment fragment = document.createDocumentFragment();
        if (isSigned && signedXMLString != null) {
            Document signedDoc = XMLUtils.toDOMDocument(signedXMLString);
            if (signedDoc == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
            }
            fragment.appendChild(document.importNode(signedDoc.getDocumentElement(), true));
            return fragment;
        }

        Element requestElement = XMLUtils.createRootElement(document, PROTOCOL_PREFIX, PROTOCOL_NAMESPACE,
                AUTHN_REQUEST, includeNSPrefix, declareNS);
        fragment.appendChild(requestElement);

        requestElement.setAttribute(ID, requestId);
        requestElement.setAttribute(VERSION, version);
        requestElement.setAttribute(ISSUE_INSTANT, DateUtils.toUTCDateFormat(issueInstant));

        if (isNotBlank(destinationURI)) {
            requestElement.setAttribute(DESTINATION, destinationURI);
        }

        if (isNotBlank(consent)) {
            requestElement.setAttribute(CONSENT, consent);
        }

        if (forceAuthn != null) {
            requestElement.setAttribute(FORCEAUTHN, forceAuthn.toString());
        }

        if (isPassive != null) {
            requestElement.setAttribute(ISPASSIVE, isPassive.toString());
        }

        // include assertionConsumerSvcIndex OR
        // AssertionConsumerServiceURL && ProtocolBinding
        if (assertionConsumerSvcIndex != null) {
            requestElement.setAttribute(ASSERTION_CONSUMER_SVC_INDEX, assertionConsumerSvcIndex.toString());
        } else {
            if (isNotBlank(protocolBinding))  {
                requestElement.setAttribute(PROTOBINDING, protocolBinding);
            }

            if (isNotBlank(assertionConsumerServiceURL)) {
                requestElement.setAttribute(ASSERTION_CONSUMER_SVC_URL, assertionConsumerServiceURL);
            }
        }

        if (attrConsumingSvcIndex != null) {
            requestElement.setAttribute(ATTR_CONSUMING_SVC_INDEX, attrConsumingSvcIndex.toString());
        }

        if (isNotBlank(providerName)) {
            requestElement.setAttribute(PROVIDER_NAME, providerName);
        }

        if (nameID != null) {
            requestElement.appendChild(nameID.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (isNotBlank(signatureString)) {
            Document parsedSignature = XMLUtils.toDOMDocument(signatureString);
            requestElement.appendChild(document.adoptNode(parsedSignature));
        }
        if (extensions != null) {
            requestElement.appendChild(extensions.toDocumentFragment(document, includeNSPrefix, declareNS));
        }
        if (subject != null) {
            requestElement.appendChild(subject.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        if (nameIDPolicy != null) {
            requestElement.appendChild(nameIDPolicy.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        if (conditions != null) {
            requestElement.appendChild(conditions.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        if (reqAuthnContext != null) {
            requestElement.appendChild(reqAuthnContext.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        if (scoping != null) {
            requestElement.appendChild(scoping.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        return fragment;
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable()	 {
	if (isMutable) {
	    super.makeImmutable();
	    if ((subject != null) && (subject.isMutable())) {
		subject.makeImmutable();
	    }
	    if ((nameIDPolicy != null) && (nameIDPolicy.isMutable())) {
		nameIDPolicy.makeImmutable();
	    }
	    if ((conditions != null) && (conditions.isMutable())) {
		conditions.makeImmutable();
	    }
	    if ((reqAuthnContext != null) && (reqAuthnContext.isMutable())) {
		reqAuthnContext.makeImmutable();
	    }
	    if ((scoping != null) && (scoping.isMutable())) {
		scoping.makeImmutable();
	    }
	    isMutable = false;
	}
    }

    /**
     * Parses the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    protected void parseDOMElement(Element element) throws SAML2Exception {
	AssertionFactory assertionFactory = AssertionFactory.getInstance();
	ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        requestId = element.getAttribute(SAML2Constants.ID);
	validateID(requestId);

        version = element.getAttribute(SAML2Constants.VERSION);
	validateVersion(version);

        String issueInstantStr = element.getAttribute(
				    SAML2Constants.ISSUE_INSTANT);
	validateIssueInstant(issueInstantStr);

        destinationURI = element.getAttribute(SAML2Constants.DESTINATION);
        consent = element.getAttribute(SAML2Constants.CONSENT);

	NodeList nList = element.getChildNodes();

	if ((nList !=null) && (nList.getLength() >0)) {
	    for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
		    if (cName.equals(SAML2Constants.ISSUER)) {
			validateIssuer();
	                nameID =
			assertionFactory.createIssuer((Element)childNode);
		    } else if (cName.equals(SAML2Constants.SIGNATURE)) {
			validateSignature();
		        signatureString = XMLUtils.print((Element) childNode);
                        isSigned = true;
		    } else if (cName.equals(SAML2Constants.EXTENSIONS)) {
			validateExtensions();
			extensions =
			    protoFactory.createExtensions((Element)childNode);
		    } else if (cName.equals(SAML2Constants.SUBJECT)) {
			validateSubject();
			subject =
			    assertionFactory.createSubject((Element)childNode);
		    } else if (cName.equals(SAML2Constants.NAMEIDPOLICY)) {
			validateNameIDPolicy();
			nameIDPolicy =
			    protoFactory.createNameIDPolicy((Element)childNode);
		    } else if (cName.equals(SAML2Constants.CONDITIONS)) {
			validateConditions();
			conditions =
			  assertionFactory.createConditions((Element)childNode);
		    } else if (cName.equals(SAML2Constants.REQ_AUTHN_CONTEXT)) {
			validateReqAuthnContext();
			reqAuthnContext=
			protoFactory.createRequestedAuthnContext(
						    (Element)childNode);
		    } else if (cName.equals(SAML2Constants.SCOPING)) {
			validateScoping();
			scoping=
			    protoFactory.createScoping((Element)childNode);
		    }
		}
	    }
	}

        // Get ForceAuthn Attribute
        String forceAuthnAttr = element.getAttribute(SAML2Constants.FORCEAUTHN);
        if ((forceAuthnAttr != null) && (forceAuthnAttr.length() > 0)) {
            forceAuthn = SAML2SDKUtils.booleanValueOf(forceAuthnAttr);
        }

        String isPassiveAttr = element.getAttribute(SAML2Constants.ISPASSIVE);
        if ((isPassiveAttr != null) && (isPassiveAttr.length() > 0)) {
            isPassive = SAML2SDKUtils.booleanValueOf(isPassiveAttr);
        }

        protocolBinding = element.getAttribute(SAML2Constants.PROTOBINDING);

        String index = element.getAttribute(
			    SAML2Constants.ASSERTION_CONSUMER_SVC_INDEX);

	if ( (index != null) && (index.length() > 0)) {
	    assertionConsumerSvcIndex = Integer.parseInt(index);
	    validateAssertionConsumerServiceIndex(assertionConsumerSvcIndex);
	}

        assertionConsumerServiceURL=XMLUtils.unescapeSpecialCharacters(
            element.getAttribute(SAML2Constants.ASSERTION_CONSUMER_SVC_URL));

        index = element.getAttribute(SAML2Constants.ATTR_CONSUMING_SVC_INDEX);
	if ( (index != null) && (index.length() > 0)) {
	    attrConsumingSvcIndex = Integer.parseInt(index);
	    validateAttributeConsumingServiceIndex(attrConsumingSvcIndex);
	}

        providerName = element.getAttribute(SAML2Constants.PROVIDER_NAME);
    }


    /* validate the value of AssertionConsumerServiceIndex attribute*/
    private void validateAssertionConsumerServiceIndex(
		Integer assertionConsumerSvcIndex) throws SAML2Exception {
	if ((assertionConsumerSvcIndex != null) &&
		((assertionConsumerSvcIndex.intValue() < 0) &&
		(assertionConsumerSvcIndex.intValue()
				> SAML2Constants.MAX_INT_VALUE)))  {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("invalidAssertionConsumerIndex"));
        }
    }


    /* validate the value of AttributeConsumingServiceIndex attribute*/
    private void validateAttributeConsumingServiceIndex(
		Integer attrConsumingSvcIndex) throws SAML2Exception {
	if ((attrConsumingSvcIndex != null) &&
		((attrConsumingSvcIndex.intValue() < 0) &&
		(attrConsumingSvcIndex.intValue()
				> SAML2Constants.MAX_INT_VALUE))) {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("invalidAttributeConsumingSvcIdx"));
        }
    }

    /* validate the sequence and occurence of Issuer Element*/
    private void validateIssuer() throws SAML2Exception {
	if (nameID != null) {
	    logger.debug("Request has too many Issuer Element");
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}

	if ((signatureString != null) || (extensions != null)
	   || (subject != null) || (nameIDPolicy != null)
	   || (conditions != null) || (reqAuthnContext != null)
	   || (scoping != null)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Issuer Element should be the " +
				    "first element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* validate the sequence and occurence of Signature Element*/
    private void validateSignature() throws SAML2Exception {
	if (signatureString != null) {
	    logger.debug("Request has too many Signature Elements");
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if ((extensions != null) || (subject != null) || (nameIDPolicy != null)
				 || (conditions != null)
				 || (reqAuthnContext != null)
				 || (scoping != null)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Signature should be the " +
				    "second element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* validate the sequence and occurence of Extensions Element*/
    private void validateExtensions() throws SAML2Exception {
	if (extensions != null) {
	    logger.debug("Request has too many Extension Elements");
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if ((subject != null) || (nameIDPolicy != null)
			      || (conditions != null)
			      || (reqAuthnContext != null)
			      || (scoping != null)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Extensions should be the " +
				    "third element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* validate the sequence and occurence of Subject Element*/
    private void validateSubject() throws SAML2Exception {
	if (subject != null) {
	    logger.debug("Request has too many Subject Elements");
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if ((nameIDPolicy != null)
		|| (conditions != null)
		|| (reqAuthnContext != null)
		|| (scoping != null)) {

	    if (logger.isDebugEnabled()) {
		logger.debug("Subject should be the " +
				    "fourth element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* validate the sequence and occurence of NameIDPolicy Element*/
    private void validateNameIDPolicy() throws SAML2Exception {
	if (nameIDPolicy != null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Request has too many " +
				     "NameIDPolicy Elements");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if ((conditions != null) || (reqAuthnContext != null)
				 || (scoping != null)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Subject should be the " +
				    "fourth element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }
    /* validate the sequence and occurence of Conditions Element*/
    private void validateConditions() throws SAML2Exception {
	if (conditions != null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Request has too many " +
				     "Conditions Elements");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if ((reqAuthnContext != null) || (scoping != null)) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Conditions should be the " +
				    "fifth element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* validate the sequence and occurence of RequestedAuthnContext Element*/
    private void validateReqAuthnContext () throws SAML2Exception {
	if (reqAuthnContext != null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("Request has too many " +
				     "RequestedAuthnContext Elements");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
	if (scoping != null) {
	    if (logger.isDebugEnabled()) {
		logger.debug("RequestedAuthnContext should " +
				    "be the sixth element in the Request");
	    }
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }

    /* Validates the sequence and occurence of Scoping Element*/
    private void validateScoping() throws SAML2Exception {
	if (scoping != null) {
	    logger.debug("Request has too many Scoping Elements");
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("schemaViolation"));
	}
    }
}
