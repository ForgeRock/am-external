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
 * $Id: IDPArtifactResolution.java,v 1.13 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2012-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.ArtifactResolve;
import com.sun.identity.saml2.protocol.ArtifactResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class handles the artifact resolution request 
 * from a service provider. It processes the artifact 
 * resolution request sent by the service provider and 
 * sends a proper SOAPMessage that contains an Assertion.
 */
public class IDPArtifactResolution {

    private static final Logger logger = LoggerFactory.getLogger(IDPArtifactResolution.class);
    static MessageFactory messageFactory = null;
    private static SOAPCommunicator soapCommunicator;
    static {
        try {
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException se) {
            logger.error("Unable to obtain SOAPFactory.", se);
        }
    }

    private IDPArtifactResolution() {
    }

    /**
     * This method processes the artifact resolution request coming from a service provider. It processes the artifact
     * resolution request sent by the service provider and sends back a proper SOAPMessage that contains an Assertion.
     *
     * @param request The <code>HttpServletRequest</code> object.
     * @param response The <code>HttpServletResponse</code> object.
     */
    public static void doArtifactResolution(HttpServletRequest request, HttpServletResponse response) {
        String classMethod = "IDPArtifactResolution.doArtifactResolution: ";
        String idpEntityID;
        String idpMetaAlias = request.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        if (StringUtils.isBlank(idpMetaAlias)) {
            idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
        }
        if (StringUtils.isBlank(idpMetaAlias)) {
            logger.debug("{}unable to get IDP meta alias from request.", classMethod);
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
            sendSoapFault(request, response, SAML2Constants.SERVER_FAULT, "nullIDPMetaAlias");
            return;
        }

        String realm;
        try {
            idpEntityID = IDPSSOUtil.metaManager.getEntityByMetaAlias(idpMetaAlias);
            if (StringUtils.isBlank(idpEntityID)) {
                logger.error("{}Unable to get IDP Entity ID from meta.", classMethod);
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, data, null);
                sendSoapFault(request, response, SAML2Constants.SERVER_FAULT, "nullIDPEntityID");
                return;
            }
            realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
        } catch (SAML2MetaException sme) {
            logger.error("{}Unable to get IDP Entity ID from meta.", classMethod);
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
            sendSoapFault(request, response, SAML2Constants.SERVER_FAULT, "metaDataError");
            return;
        }
        if (!SAML2Utils.isIDPProfileBindingSupported(realm, idpEntityID, SAML2Constants.ARTIFACT_RESOLUTION_SERVICE,
                SAML2Constants.SOAP)) {
            logger.error("{}Artifact Resolution Service binding: Redirect is not supported for {}",
                    classMethod, idpEntityID);
            String[] data = {idpEntityID, SAML2Constants.SOAP};
            LogUtil.error(Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            sendSoapFault(request, response, SAML2Constants.CLIENT_FAULT, "unsupportedBinding");
            return;
        }

        try {
            MimeHeaders headers = SAMLUtils.getMimeHeaders(request);
            InputStream is = request.getInputStream();
            SOAPMessage message = messageFactory.createMessage(headers, is);
            sendSoapResponse(request, response, onMessage(message, request, response, realm, idpEntityID));
        } catch (SAML2Exception se) {
            logger.error("{}SAML2 error", classMethod, se);
            sendSoapFault(request, response, SAML2Constants.SERVER_FAULT, "UnableToCreateArtifactResponse");
        } catch (SOAPException | IOException ex) {
            logger.error("{}SOAP error", classMethod, ex);
            String[] data = { idpEntityID };
            LogUtil.error(Level.INFO, LogUtil.INVALID_SOAP_MESSAGE, data, null);
            sendSoapFault(request, response, SAML2Constants.SERVER_FAULT, "invalidSOAPMessage");
        }
    }

    private static void sendSoapFault(HttpServletRequest request, HttpServletResponse response, String faultCode,
            String faultString) {
        sendSoapResponse(request, response, getSoapCommunicator().createSOAPFault(faultCode, faultString,
                null));
    }

    private static void sendSoapResponse(HttpServletRequest request, HttpServletResponse response, SOAPMessage reply) {
        try {
            if (reply != null) {
                // Need to call saveChanges because we're going to use the MimeHeaders to set HTTP response
                // information. These MimeHeaders are generated as part of the save.
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }

                response.setStatus(HttpServletResponse.SC_OK);
                putHeaders(reply.getMimeHeaders(), response);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                reply.writeTo(stream);
                response.getWriter().println(stream.toString("UTF-8"));
                response.getWriter().flush();
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (SOAPException | IOException ex) {
            logger.error("An error occurred while trying to return SOAP response", ex);
            SAMLUtils.sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "errorInSOAPResponse",
                    SAML2Utils.bundle.getString("errorInSOAPResponse") + " " + ex.getMessage());
        }
    }

    /**
     * This method generates a <code>SOAPMessage</code> containing the
     * <code>ArtifactResponse</code> that is corresponding to the
     * <code>ArtifactResolve</code> contained in the 
     * <code>SOAPMessage</code> passed in.
     *
     * @param message <code>SOAPMessage</code> contains a
     *             <code>ArtifactResolve</code> 
     * @param request the <code>HttpServletRequest</code> object
     * @param realm the realm to where the identity provider belongs
     * @param idpEntityID the entity id of the identity provider 
     * 
     * @return <code>SOAPMessage</code> contains the 
     *             <code>ArtifactResponse</code>
     * @exception SAML2Exception if the operation is not successful
     */

    public static SOAPMessage onMessage(SOAPMessage message, 
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        String realm,
                                        String idpEntityID) 
        throws SAML2Exception {
    
        String classMethod = "IDPArtifactResolution.onMessage: ";
    
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "Entering onMessage().");
        }
    
        Element reqElem = getSoapCommunicator().getSamlpElement(message,
                "ArtifactResolve");
        ArtifactResolve artResolve = 
            ProtocolFactory.getInstance().createArtifactResolve(reqElem);

        if (artResolve == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                    "no valid ArtifactResolve node found in SOAP body.");
            }
            return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "noArtifactResolve", null);
        }

        String spEntityID = artResolve.getIssuer().getValue();
        if (!SAML2Utils.isSourceSiteValid(
            artResolve.getIssuer(), realm, idpEntityID)) 
        {
            logger.error(classMethod + spEntityID +
                " is not trusted issuer.");
            String[] data = { idpEntityID, realm, artResolve.getID() };
            LogUtil.error(
                Level.INFO, LogUtil.INVALID_ISSUER_REQUEST, data, null);
            return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "invalidIssuerInRequest", null);
        }
        SPSSODescriptorType spSSODescriptor = null;
        try {
            spSSODescriptor = IDPSSOUtil.metaManager.
                      getSPSSODescriptor(realm, spEntityID);
        } catch (SAML2MetaException sme) {
            logger.error(classMethod, sme);
            spSSODescriptor = null;
        }
        if (spSSODescriptor == null) {
            logger.error(classMethod +
                "Unable to get SP SSO Descriptor from meta.");
            return getSoapCommunicator().createSOAPFault(SAML2Constants.SERVER_FAULT,
                    "metaDataError", null);
        }
        OrderedSet acsSet = SPSSOFederate.getACSUrl(spSSODescriptor,
            SAML2Constants.HTTP_ARTIFACT);
        String acsURL = (String) acsSet.get(0);
        //String protocolBinding = (String) acsSet.get(1);

        String isArtifactResolveSigned = 
           SAML2Utils.getAttributeValueFromSSOConfig(
               realm, idpEntityID, SAML2Constants.IDP_ROLE,
               SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED);
        if ((isArtifactResolveSigned != null) 
            && (isArtifactResolveSigned.equals(SAML2Constants.TRUE))) {
            if (!artResolve.isSigned()) {
                logger.error(classMethod +
                    "The artifact resolve is not signed " +
                    "when it is expected to be signed.");
                return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                        "ArtifactResolveNotSigned", null);
            }
            Set<X509Certificate> verificationCerts = KeyUtil.getVerificationCerts(spSSODescriptor, spEntityID,
                    SAML2Constants.SP_ROLE, realm);
            if (!artResolve.isSignatureValid(verificationCerts)) {
                logger.error(classMethod +
                    "artifact resolve verification failed.");
                return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                        "invalidArtifact", null);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                    "artifact resolve signature verification is successful.");
            }
        }
                
        Artifact art = artResolve.getArtifact();
        if (art == null) {
            logger.error(classMethod +
                "Unable to get an artifact from ArtifactResolve.");
            return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "invalidArtifactSignature", null);
        }

        String artStr = art.getArtifactValue();
        Response res = 
            (Response)IDPCache.responsesByArtifacts.remove(artStr);

        if (res == null) {
            // Check the SAML2 Token Repository
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Artifact=" + artStr);
                }
                res = (Response) SAML2FailoverUtils.retrieveSAML2Token(artStr);
            } catch (SAML2TokenRepositoryException se) {
                logger.error(classMethod + " There was a problem reading the response "
                        + "from the SAML2 Token Repository using artStr:" + artStr, se);
                return getSoapCommunicator().createSOAPFault(
                        SAML2Constants.CLIENT_FAULT,
                        "UnableToFindResponseInRepo", null);
            }
        }

        if (res == null) {
            logger.error("Unable to find response for artifact {}", artStr);
            return getSoapCommunicator().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "UnableToFindResponse", null);
        }

        // Remove Response from SAML2 Token Repository
        try {
            SAML2FailoverUtils.deleteSAML2Token(artStr);
        } catch (SAML2TokenRepositoryException e) {
            logger.error(classMethod + 
                    " Error deleting the response from the SAML2 Token Repository using artStr:" + artStr, e);
        }

        Map props = new HashMap();
        String nameIDString = SAML2Utils.getNameIDStringFromResponse(res);
        if (nameIDString != null) {
            props.put(LogUtil.NAME_ID, nameIDString);
        }
        
        // check if need to sign the assertion
        boolean signAssertion = spSSODescriptor.isWantAssertionsSigned();
        if (signAssertion) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                    "signing the assertion.");
            }
        }
        // encrypt the assertion or its NameID and/or Attribute based
        // on SP config setting and sign the assertion.
        IDPSSOUtil.signAndEncryptResponseComponents(realm, spEntityID, idpEntityID, res, signAssertion);

        ArtifactResponse artResponse = ProtocolFactory.getInstance().createArtifactResponse();

        Status status = ProtocolFactory.getInstance().createStatus();

        StatusCode statusCode = ProtocolFactory.getInstance().createStatusCode();
        statusCode.setValue(SAML2Constants.SUCCESS);
        status.setStatusCode(statusCode);
        
        // set the idp entity id as the response issuer
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);
 
        artResponse.setStatus(status);
        artResponse.setID(SAML2Utils.generateID());
        artResponse.setInResponseTo(artResolve.getID());
        artResponse.setVersion(SAML2Constants.VERSION_2_0);
        artResponse.setIssueInstant(newDate());
        artResponse.setAny(res.toXMLString(true,true));
        artResponse.setIssuer(issuer);
        artResponse.setDestination(XMLUtils.escapeSpecialCharacters(acsURL)); 
        
        String wantArtifactResponseSigned = 
           SAML2Utils.getAttributeValueFromSSOConfig(realm, spEntityID, SAML2Constants.SP_ROLE,
               SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED);
        if ((wantArtifactResponseSigned != null) && (wantArtifactResponseSigned.equals(SAML2Constants.TRUE))) {
            Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                    .resolveActiveSigningCredential(realm, idpEntityID, Saml2EntityRole.IDP);
            Key signingKey = credentials.getSigningKey();
            X509Certificate signingCert = credentials.getSigningCertificate();
            artResponse.sign(SigningConfigFactory.getInstance()
                    .createXmlSigningConfig(signingKey, signingCert,
                            SAML2Utils.getSAML2MetaManager().getEntityDescriptor(realm, spEntityID), SP));
        }

        String str = artResponse.toXMLString(true,true);
        String[] logdata = {idpEntityID, artStr, str};
        LogUtil.access(Level.INFO, LogUtil.ARTIFACT_RESPONSE, logdata, null, props);
        if (str != null) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "ArtifactResponse message:\n"+ str);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "Unable to print ArtifactResponse message.");
            }
        }        

        SOAPMessage msg = null;
        try {
            msg = getSoapCommunicator().createSOAPMessage(str, false);
        } catch (SOAPException se) {
            logger.error(classMethod + "Unable to create a SOAPMessage and add a document ", se);
            return getSoapCommunicator().createSOAPFault(SAML2Constants.SERVER_FAULT, "unableToCreateSOAPMessage", null);
        }

        return msg;
    }

    // puts MIME headers into a HTTPResponse
    private static void putHeaders(MimeHeaders headers, 
                                  HttpServletResponse res) {
        Iterator it = headers.getAllHeaders();
        while (it.hasNext()) {
            MimeHeader header = (MimeHeader)it.next();
            String[] values = headers.getHeader(header.getName());
            if (values.length == 1) {
            res.setHeader(header.getName(), header.getValue());
            } else {
            StringBuilder concat = new StringBuilder();
            int i = 0;
            while (i < values.length) {
                if (i != 0) {
                concat.append(',');
                }
                concat.append(values[i++]);
            }
            res.setHeader(header.getName(), concat.toString());
            }
        }
    }


    private static SOAPCommunicator getSoapCommunicator() {
        if(soapCommunicator == null) {
            soapCommunicator = InjectorHolder.getInstance(SOAPCommunicator.class);
        }
        return soapCommunicator;
    }
} 
