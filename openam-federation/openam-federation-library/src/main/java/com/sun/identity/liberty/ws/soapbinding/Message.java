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
 * $Id: Message.java,v 1.3 2008/06/25 05:47:22 qcheng Exp $
 *
 * Portions Copyrighted 2017-2025 Ping Identity Corporation.
 */
package com.sun.identity.liberty.ws.soapbinding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.security.SecurityUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>Message</code> class is used by web service client and server to
 * construct request or response. It will be sent over the SOAP connection.
 * The <code>Message</code> contains SOAP headers and bodies. The SOAP binding
 * defines the following headers: <code>CorrelationHeader</code>,
 * <code>ProviderHeader</code>, <code>ConsentHeader</code>,
 * <code>UsageDirectiveHeader</code>, <code>ProcessingContextHeader</code>
 * and <code>ServiceInstanceUpdateHeader</code>.
 * The first 2 are required and the others are optional.
 * Signing is mandatory for <code>CorrelationHeader</code> and SOAP Body
 * element which is the parent of the bodies. Other headers are optional,
 * so each header needs to have a flag to specify whether it needs to be
 * signed or not. For each header that needs to be signed, it must have an
 * id attribute in the top element. The constuctor will take a SAML assertion
 * or cert alias in order to sign.
 * 
 */
@SupportedAll
public class Message {
    private static final Logger logger = LoggerFactory.getLogger(Message.class);
    /**
     * anonymous profile is specified.
     */
    public static final int ANONYMOUS  = 0;

    /**
     * X509 Token profile is specified.
     */
    public static final int X509_TOKEN = 1;

    /**
     * SAML Token profile is specified.
     */
    public static final int SAML_TOKEN = 2;

    /**
     * Bearer Token profile is specified.
     */
    public static final int BEARER_TOKEN = 3;

    private int securityProfileType = ANONYMOUS;
    private CorrelationHeader correlationHeader = null;
    private ConsentHeader consentHeader = null;
    private List usageDirectiveHeaders = null;
    private ServiceInstanceUpdateHeader serviceInstanceUpdateHeader = null;
    private List soapHeaders = null;
    private List soapBodies = null;
    private List securityHeaders = null;
    private List signingIds = null;
    private SOAPFault soapFault = null;
    private String protocol = "http";
    private SecurityAssertion assertion = null;
    private BinarySecurityToken binarySecurityToken = null;
    private X509Certificate certificate = null;
    private X509Certificate messageCertificate = null;
    private Object token = null;
    private String bodyId = null;
    private Document doc = null;
    private String wsfVersion = SOAPBindingConstants.WSF_11_VERSION;

    /**
     * Default Constructor.
     */
    public Message() {
        correlationHeader = new CorrelationHeader();
        securityProfileType = ANONYMOUS;
    }

    /**
     * This constructor is to create a SOAP fault message.
     *
     * @param soapFault <code>SOAPFault</code>
     */
    public Message( SOAPFault soapFault) {
        this.soapFault = soapFault;
        correlationHeader = new CorrelationHeader();
    }

    /**
     * This constructor takes an InputStream.
     *
     * @param inputStream an InputStream
     * @throws SOAPBindingException if an error occurs while parsing the input.
     */
    public Message(InputStream inputStream) throws SOAPBindingException {
        try {
            doc = XMLUtils.toDOMDocument(inputStream);
            parseDocument(doc);
        } catch (Exception ex) {
            logger.error("Message:Message", ex);
            throw new SOAPBindingException(ex.getMessage());
        }
    }

    /**
     * This constructor takes a SOAP message which is received from a SOAP
     * connection.
     *
     * @param  soapMessage a SOAP message
     * @throws SOAPBindingException if an error occurs while parsing the
     *         SOAP message
     */
    public Message(SOAPMessage soapMessage)
           throws SOAPBindingException,SOAPFaultException {
        try {
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            soapMessage.writeTo(bop);
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bop.toByteArray());
            doc = XMLUtils.toDOMDocument(bin);
            parseDocument(doc);
        } catch (Exception ex) {
            logger.error("Message:Message", ex);
            throw new SOAPBindingException(ex.getMessage());
        }
    }

    /**
     * Gets security profile type. Possible values are ANONYMOUS, X509_TOKEN
     * and SAML_TOKEN.
     *
     * @return the Security Profile type
     */
    public int getSecurityProfileType() {
        return securityProfileType;
    }

    /**
     * Returns the <code>CorrelationHeader</code>.
     *
     * @return the <code>CorrelationHeader</code>.
     */
    public CorrelationHeader getCorrelationHeader() {
        return correlationHeader;
    }

    /**
     * Returns the <code>ConsentHeader</code>.
     *
     * @return the <code>ConsentHeader</code>.
     */
    public ConsentHeader getConsentHeader() {
        return consentHeader;
    }

    /**
     * Returns a list of <code>UsageDirectiveHeader</code>.
     *
     * @return a list of <code>UsageDirectiveHeader</code>.
     */
    public List getUsageDirectiveHeaders() {
        return usageDirectiveHeaders;
    }

    /**
     * Returns a list of SOAP headers except  <code>CorrelationHeader</code>,
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code> and
     * <code>Security</code> header. Each entry will be a 
     * <code>org.w3c.dom.Element</code>.
     *
     * @return a list of SOAP headers
     */
    public List getOtherSOAPHeaders() {
        return soapHeaders;
    }

    /**
     * Returns the <code>SOAPFault</code>.
     *
     * @return the <code>SOAPFault</code>.
     */
    public SOAPFault getSOAPFault() {
        return soapFault;
    }

    /**
     * Returns a list of SOAP bodies.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @return a list of SOAP bodies
     */
    public List getBodies() {
        return soapBodies;
    }

    /**
     * Returns the SAML assertion used for signing.
     *
     * @return the SAML assertion.
     */
    public SecurityAssertion getAssertion() {
        return assertion;
    }

    /**
     * Returns the X509 certificate used in client authentication.
     *
     * @return a X509 certificate
     */
    public X509Certificate getPeerCertificate() {
        return certificate;
    }

    /**
     * Returns the X509 certificate used in message level authentication.
     *
     * @return a X509 certificate.
     */
    public X509Certificate getMessageCertificate() {
        return messageCertificate;
    }

    /**
     * Returns a token for the sender of this Message.
     *
     * @return a token Object.
     */
    public Object getToken() {
        return token;
    }

    /**
     * Returns a list of id's for signing.
     *
     * @return a list of id's for signing.
     */
    public List getSigningIds() {
        List ids = new  ArrayList();
        ids.add(correlationHeader.getId());
        if (consentHeader != null) {
            String id = consentHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (usageDirectiveHeaders != null &&
            !usageDirectiveHeaders.isEmpty()) {
            Iterator iter = usageDirectiveHeaders.iterator();
            while(iter.hasNext()) {
                String id = ((UsageDirectiveHeader)iter.next()).getId();
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        if (serviceInstanceUpdateHeader != null) {
            String id = serviceInstanceUpdateHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (signingIds != null && !signingIds.isEmpty()) {
            ids.addAll(signingIds);
        }
        if (bodyId == null) {
            bodyId = SAMLUtils.generateID();
        }
        ids.add(bodyId);
        return ids;
    }

    /**
     * Sets a SOAP body. To send a SOAP Fault, please use method
     * <code>setSOAPFault</code>.
     *
     * @param body a <code>org.w3c.dom.Element</code>
     */
    public void setSOAPBody(Element body) {
        soapBodies = new ArrayList(1);
        soapBodies.add(body);
    }

    /**
     * Sets the protocol value . The expected
     * value is either http or https.
     *
     * @param protocol the protocol value.
     */
    void setProtocol( String protocol) {
        if (protocol == null) {
            this.protocol = "http";
        } else {
            this.protocol = protocol;
        }
    }

    /**
     * Sets a token for the sender of this Message. The accual type
     * will be the same as the type of the Object retured from
     * <code>WebServiceAuthenticator.authenticate</code>.
     *
     * @param token a token Object
     */
    void setToken( Object token) {
        this.token = token;
    }

    /**
     * Returns the SOAP message in String format.
     *
     * @return the SOAP message in String format.
     */
    public String toString() {
        try {
            return XMLUtils.print(toDocument(true).getDocumentElement());
        } catch (Exception ex) {
            logger.error("Message.toString", ex);
            return "";
        }
    }

    /**
     * Returns the SOAP message in <code>org.w3c.dom.Document</code> format.
     *
     * @param refresh true to reconstruct a document, false to reuse a
     *                previous document. If previous document doesn't exist,
     *                it will construct a new document.
     * @return the SOAP message in <code>org.w3c.dom.Document</code> format.
     * @throws SOAPBindingException if an error occurs while constructing
     *                                 the <code>org.w3c.dom.Document</code>.
     */
    public Document toDocument( boolean refresh) throws SOAPBindingException {
        if (!refresh && doc != null) {
            return doc;
        }

        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            logger.error("Message:toDocument", ex);
            throw new SOAPBindingException(ex.getMessage());
        }

        String wsseNS = WSSEConstants.NS_WSSE_WSF11;
        String wsuNS = WSSEConstants.NS_WSU_WSF11;
        if(SOAPBindingConstants.WSF_10_VERSION.equals(wsfVersion)) {
           wsseNS = WSSEConstants.NS_WSSE;
           wsuNS = WSSEConstants.NS_WSU;
        }

        Element envelopeE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                           SOAPBindingConstants.PTAG_ENVELOPE);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP,
                                 SOAPBindingConstants.NS_SOAP);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP_BINDING,
                                 SOAPBindingConstants.NS_SOAP_BINDING);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP_BINDING_11,
                                 SOAPBindingConstants.NS_SOAP_BINDING_11);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 WSSEConstants.TAG_XML_WSU,
                                 wsuNS);
        doc.appendChild(envelopeE);
        Element headerE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                             SOAPBindingConstants.PTAG_HEADER);
        envelopeE.appendChild(headerE);
        if (correlationHeader != null) {
            correlationHeader.addToParent(headerE);
        }
        if (consentHeader != null) {
            consentHeader.addToParent(headerE);
        }
        if (usageDirectiveHeaders != null &&
            !usageDirectiveHeaders.isEmpty()) {
            Iterator iter = usageDirectiveHeaders.iterator();
            while(iter.hasNext()) {
                ((UsageDirectiveHeader)iter.next()).addToParent(headerE);
            }
        }

        if (serviceInstanceUpdateHeader != null) {
            serviceInstanceUpdateHeader.addToParent(headerE);
        }

        if (soapHeaders != null && !soapHeaders.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message.toDocument: adding headers ");
            }
            Iterator iter = soapHeaders.iterator();
            while(iter.hasNext()) {
                Element soapHeaderE = (Element)iter.next();
                headerE.appendChild(doc.importNode(soapHeaderE, true));
            }
        }

        boolean hasSecurityHeaders = 
                (securityHeaders != null && !securityHeaders.isEmpty());
        if (securityProfileType != ANONYMOUS || hasSecurityHeaders) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Message.toDocument: adding security headers ");
            }

            Element securityE = doc.createElementNS(wsseNS,
                WSSEConstants.TAG_WSSE + ":" + WSSEConstants.TAG_SECURITYT);
            securityE.setAttributeNS(SOAPBindingConstants.NS_XML,
                WSSEConstants.TAG_XML_WSSE, wsseNS);
            headerE.appendChild(securityE);

            if (assertion != null) {
                Document assertionDoc =
                        XMLUtils.toDOMDocument(assertion.toString(true, true)
                        );
                if (assertionDoc == null) {
                    String msg =
                        Utils.bundle.getString("cannotProcessSAMLAssertion");
                    logger.error("Message.Message: " + msg);
                    throw new SOAPBindingException(msg);
                }
                Element assertionE = assertionDoc.getDocumentElement();
                securityE.appendChild(doc.importNode(assertionE, true));
            } else if (binarySecurityToken != null) {
                Document bstDoc =
                        XMLUtils.toDOMDocument(binarySecurityToken.toString()
                        );
                if (bstDoc == null) {
                    String msg = Utils.bundle.getString(
                                     "cannotProcessBinarySecurityToken");
                    logger.error("Message.Message: " + msg);
                    throw new SOAPBindingException(msg);
                }
                Element binarySecurityTokenE = bstDoc.getDocumentElement();
                securityE.appendChild(doc.importNode(binarySecurityTokenE,
                                                     true));
            }

            if (hasSecurityHeaders) {
                Iterator iter = securityHeaders.iterator();
                while(iter.hasNext()) {
                    securityE.appendChild(doc.importNode((Node)iter.next(),
                                                         true));
                }
            }
        }

        Element bodyE = null;
        if (soapFault != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message.toDocument: adding soapFault ");
            }

            bodyE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                        SOAPBindingConstants.PTAG_BODY);
            envelopeE.appendChild(bodyE);
            soapFault.addToParent(bodyE);
        }

        if (soapBodies != null && !soapBodies.isEmpty()){

            if (logger.isDebugEnabled()) {
                logger.debug("Message.toDocument: adding bodies ");
            }

            if (bodyE == null) {
                bodyE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                            SOAPBindingConstants.PTAG_BODY);
                bodyE.setAttributeNS(SOAPBindingConstants.NS_XML,
                    SOAPBindingConstants.XMLNS_SOAP,
                    SOAPBindingConstants.NS_SOAP);
                envelopeE.appendChild(bodyE);
            }

            Iterator iter = soapBodies.iterator();
            while(iter.hasNext()) {
                Element soapBodyE = (Element)iter.next();
                bodyE.appendChild(doc.importNode(soapBodyE, true));
            }

            if (bodyId == null) {
                bodyId = SAMLUtils.generateID();
            }
            if (SOAPBindingConstants.WSF_10_VERSION.equals(wsfVersion)) {
                bodyE.setAttributeNS(null, SOAPBindingConstants.ATTR_id,
                    bodyId);
            } else {
                bodyE.setAttributeNS(wsuNS, WSSEConstants.WSU_ID, bodyId);
            }
        }

        return doc;
    }

    /**
     * Parses a <code>org.w3c.dom.Document</code> to construct this object.
     *
     * @param doc a <code>org.w3c.dom.Document</code>.
     * @throws SOAPBindingException if an error occurs while parsing
     *                              the document
     */
    private void parseDocument( Document doc) throws SOAPBindingException {
        Element envelopeE = doc.getDocumentElement();

        if (logger.isDebugEnabled()) {
            logger.debug("Message.parseDocument: doc = " +
                                XMLUtils.print(envelopeE));
        }

        NodeList nl = envelopeE.getChildNodes();
        int length = nl.getLength();

        if (length == 0) {
            String msg = Utils.bundle.getString("soapEnvelopeMissingChildren");
            logger.error("Message.parseDocument: " + msg);
            throw new SOAPBindingException(msg);
        }

        Element headerE = null;
        Element bodyE = null;
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                String localName = element.getLocalName();
                String namespaceURI = element.getNamespaceURI();

                if (SOAPBindingConstants.NS_SOAP.equals(namespaceURI)) {
                    if (SOAPBindingConstants.TAG_HEADER.equals(localName)) {
                        headerE = element;
                    } else if(SOAPBindingConstants.TAG_BODY.equals(localName)){
                        bodyE = element;
                    }
                }
            }
        }

        Element securityE = null;
        soapHeaders = new ArrayList();
        // parsing Header element
        if (headerE != null) {
            nl = headerE.getChildNodes();
            length = nl.getLength();
            for (int i = 0; i < length; i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)child;
		    String localName = element.getLocalName();
		    String ns = element.getNamespaceURI();

                    if (SOAPBindingConstants.NS_SOAP_BINDING.equals(ns)) {
                        if (SOAPBindingConstants.TAG_CORRELATION
                                                .equals(localName)) {
                            correlationHeader = new CorrelationHeader(element);
                        } else if (SOAPBindingConstants.TAG_CONSENT
                                                       .equals(localName)) {
                            consentHeader = new ConsentHeader(element);
                        } else if(SOAPBindingConstants.TAG_USAGE_DIRECTIVE
                                                      .equals(localName)){
                            if (usageDirectiveHeaders == null) {
                                usageDirectiveHeaders = new ArrayList();
                            }
                            usageDirectiveHeaders.add(
                                    new UsageDirectiveHeader(element));
                        } else {
                            soapHeaders.add(element);
                        }
                    } else if (SOAPBindingConstants.NS_SOAP_BINDING_11
                                                   .equals(ns) &&
                               SOAPBindingConstants
                                                 .TAG_SERVICE_INSTANCE_UPDATE
                                                 .equals(localName)) {

                        serviceInstanceUpdateHeader =
                                new ServiceInstanceUpdateHeader(element);
                    } else if (WSSEConstants.NS_WSSE.equals(ns) ||
                        WSSEConstants.NS_WSSE_WSF11.equals(ns)) {
                        if (WSSEConstants.TAG_SECURITYT.equals(localName)) {
                            securityE = element;
                        } else {
                            soapHeaders.add(element);
                        }
                    } else {
                        soapHeaders.add(element);
                    }
                }
            }
            parseSecurityElement(securityE);
        }

        if (soapHeaders.isEmpty()) {
            soapHeaders = null;
        }

        // parsing Body element

        if (bodyE != null) {
            nl = bodyE.getChildNodes();
            length = nl.getLength();
            for(int i = 0; i < length; i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childE = (Element)child;
                    String localName = childE.getLocalName();
                    String ns = childE.getNamespaceURI();
                    if (soapFault == null &&
                        SOAPBindingConstants.NS_SOAP.equals(ns) &&
                        SOAPBindingConstants.TAG_FAULT.equals(localName)) {
                        soapFault = new SOAPFault(childE);
                    } else {
                        if (soapBodies == null) {
                            soapBodies = new ArrayList();
                        }
                        soapBodies.add(child);
                    }
                }
            }
        }

    }

    /**
     * Sets security profile type by parsing a security element.
     *
     * @param securityE a security element
     * @throws SOAPBindingException if an error occurs while parsing
     *                              the security element
     */
    private void parseSecurityElement(Element securityE)
    throws SOAPBindingException {
        if (securityE == null) {
            securityProfileType = ANONYMOUS;
            return;
        }

        String wsseNS = securityE.getNamespaceURI();
        if (wsseNS == null) {
            securityProfileType = ANONYMOUS;
            return;
        }
        String wsuNS = null;
        if (wsseNS.equals(WSSEConstants.NS_WSSE_WSF11)) {
            wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
            wsuNS = WSSEConstants.NS_WSU_WSF11;

        } else if(wsseNS.equals(WSSEConstants.NS_WSSE)) {
            wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
            wsuNS = WSSEConstants.NS_WSU;

        } else {
            securityProfileType = ANONYMOUS;
            return;
        }

        NodeList nl = securityE.getElementsByTagNameNS(wsseNS,
            SAMLConstants.TAG_SECURITYTOKENREFERENCE);

        Element securityTokenRefE = null;
        String uri = null;
        if (nl != null && nl.getLength() > 0) {
            securityTokenRefE = (Element)nl.item(0);
            List list = XMLUtils.getElementsByTagNameNS1(securityTokenRefE,
                wsseNS, SAMLConstants.TAG_REFERENCE);
            if (!list.isEmpty()) {
                Element referenceE = (Element)list.get(0);
                uri = XMLUtils.getNodeAttributeValue(referenceE,
                        SAMLConstants.TAG_URI);
                if (uri != null && uri.length() > 1 && uri.startsWith("#")) {
                    uri = uri.substring(1);
                } else {
                    String msg = Utils.bundle.getString("invalidReferenceURI");
                    logger.error("Message.parseSecurityElement: " + msg);
                    throw new SOAPBindingException(msg);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Message.parseSecurityElement: " +
                            "SecurityTokenReference Reference URI = " + uri);
                }
            }
        }
        
        securityProfileType = ANONYMOUS;
        securityHeaders = new ArrayList();
        nl = securityE.getChildNodes();
        int length = nl.getLength();
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String localName = child.getLocalName();
                String ns = child.getNamespaceURI();
                
                if (securityProfileType != ANONYMOUS) {
                    securityHeaders.add(child);
                    continue;
                }
                
                if (SAMLConstants.BINARYSECURITYTOKEN.equals(localName) &&
                    wsseNS.equals(ns)) {
                    
                    Element binarySecurityTokenE = (Element)child;
                    String valuetype = XMLUtils.getNodeAttributeValue(
                            binarySecurityTokenE,
                            "ValueType");
                    logger.debug("ValueType: "+valuetype);
                    if ((valuetype != null) &&
                            valuetype.endsWith("ServiceSessionContext")) {
                        securityHeaders.add(child);
                        continue;
                    }
                    if (uri != null) {
                        String id = XMLUtils.getNodeAttributeValueNS(
                            binarySecurityTokenE, wsuNS, SAMLConstants.TAG_ID);
                        if (!uri.equals(id)) {
                            securityHeaders.add(child);
                            continue;
                        }
                    }
                    
                    try {
                        binarySecurityToken =
                                new BinarySecurityToken(binarySecurityTokenE);
                        messageCertificate =
                                (X509Certificate)SecurityUtils.getCertificate(
                                binarySecurityToken);
                    } catch (Exception ex) {
                        String msg = Utils.bundle.getString(
                                "cannotProcessBinarySecurityToken");
                        logger.error("Message.parseSecurityElement: "+
                                msg);
                        throw new SOAPBindingException(msg);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message.parseSecurityElement:" +
                                " found binary security token");
                    }
                    securityProfileType = X509_TOKEN;
                } else if (SAMLConstants.TAG_ASSERTION.equals(localName) &&
                        SAMLConstants.assertionSAMLNameSpaceURI.equals(ns)){
                    
                    Element assertionE = (Element)child;
                    
                    if (uri != null) {
                        String assertionID = XMLUtils.getNodeAttributeValue(
                                assertionE,
                                SAMLConstants.TAG_ASSERTION_ID);
                        if (!uri.equals(assertionID)) {
                            securityHeaders.add(child);
                            continue;
                        }
                    }
                    
                    try {
                        assertion = new SecurityAssertion(assertionE);
                    } catch (SAMLException ex) {
                        String msg = Utils.bundle.getString(
                                "cannotProcessSAMLAssertion");
                        logger.error("Message.parseSecurityElement: " +
                                msg);
                        throw new SOAPBindingException(msg);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message.parseSecurityElement:" +
                                " found security assertion, " +
                                "isBearer = " +
                                assertion.isBearer());
                    }
                    
                    if (assertion.isBearer()) {
                        securityProfileType = BEARER_TOKEN;
                    } else {
                        securityProfileType = SAML_TOKEN;
                        messageCertificate =
                                (X509Certificate)SecurityUtils.getCertificate(
                                assertion);
                    }
                } else {
                    securityHeaders.add(child);
                }
            }
        }
        if (securityHeaders.isEmpty()) {
            securityHeaders = null;
        }
    }

    /**
     * Returns the web services version of the message.
     *
     * @return the web services version.
     */
    public String
    getWSFVersion()
    {
        return wsfVersion;
    }
}
