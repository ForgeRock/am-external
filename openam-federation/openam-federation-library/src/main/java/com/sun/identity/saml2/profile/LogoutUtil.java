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
 * $Id: LogoutUtil.java,v 1.16 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2012-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import org.forgerock.openam.saml2.plugins.FedletAdapter;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.SessionIndex;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusDetail;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class constructs the <code>LogoutRequest</code> and executes
 * the required processing logic for sending <code>LogoutRequest</code>
 * from SP to IDP.
 */

public class LogoutUtil {
    private static final Logger logger = LoggerFactory.getLogger(LogoutUtil.class);
    static SAML2MetaManager metaManager = null;
    private static SOAPCommunicator soapCommunicator;

    static {
        metaManager= SAML2Utils.getSAML2MetaManager();
    }

    /**
     * Builds the <code>LogoutRequest</code> and executes
     * the required processing logic for sending <code>LogoutRequest</code>
     * from SP to IDP.
     *
     * @param metaAlias the requester's metaAlais.
     * @param recipientEntityID the recipient's entity ID.
     * @param recipientSLOList recipient's Single Logout Service location
     * URL list.
     * @param extensionsList Extension list for request.
     * @param binding binding used for this request.
     * @param relayState the target URL on successful Single Logout.
     * @param sessionIndex sessionIndex of the Assertion generated by the
     * Identity Provider or Service Provider.
     * @param nameID <code>NameID</code> of the Provider.
     * @param response the HttpServletResponse.
     * @param paramsMap Map of all other parameters.
     *       Following parameters names with their respective
     *       String values are allowed in this paramsMap.
     *       "realm" - MetaAlias for Service Provider. The format of
     *               this parameter is /realm_name/SP name.
     *       "RelayState" - the target URL on successful Single Logout
     *       "Destination" - A URI Reference indicating the address to
     *                       which the request has been sent.
     *       "Consent" - Specifies a URI a SAML defined identifier
     *                   known as Consent Identifiers.
     * @param config entity base config for basic auth.
     *
     * @return Logout request ID
     *
     * @throws SAML2Exception if error initiating request to IDP.
     * @throws SessionException if error initiating request to IDP.
     */
    public static StringBuffer doLogout(
            String metaAlias,
            String recipientEntityID,
            List<EndpointType> recipientSLOList,
            List extensionsList,
            String binding,
            String relayState,
            String sessionIndex,
            NameID nameID,
            HttpServletRequest request,
            HttpServletResponse response,
            Map paramsMap,
            JAXBElement<BaseConfigType> config) throws SAML2Exception, SessionException {
        EndpointType logoutEndpoint = null;
        for (EndpointType endpoint : recipientSLOList) {
            if (binding.equals(endpoint.getBinding())) {
                logoutEndpoint = endpoint;
                break;
            }
        }
        return doLogout(metaAlias, recipientEntityID, extensionsList, logoutEndpoint, relayState, sessionIndex, nameID,
                request, response, paramsMap, config);
    }

    public static StringBuffer doLogout(
            String metaAlias,
            String recipientEntityID,
            List extensionsList,
            EndpointType logoutEndpoint,
            String relayState,
            String sessionIndex,
            NameID nameID,
            HttpServletRequest request,
            HttpServletResponse response,
            Map paramsMap,
            JAXBElement<BaseConfigType> config) throws SAML2Exception, SessionException {
        StringBuffer logoutRequestID = new StringBuffer();

        String classMethod = "LogoutUtil.doLogout: ";
        String requesterEntityID = metaManager.getEntityByMetaAlias(metaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityRole = SAML2Utils.getHostEntityRole(paramsMap);
        String location = null;
        String binding = null;
        if (logoutEndpoint != null) {
            location = logoutEndpoint.getLocation();
            binding = logoutEndpoint.getBinding();
        } else {
            logger.error(classMethod + "Unable to find the recipient's single logout service with the binding "
                    + binding);
            throw new SAML2Exception(SAML2Utils.bundle.getString("sloServiceNotfound"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "Entering ..."
                    + "\nrequesterEntityID=" + requesterEntityID
                    + "\nrecipientEntityID=" + recipientEntityID
                    + "\nbinding=" + binding
                    + "\nrelayState=" + relayState
                    + "\nsessionIndex=" + sessionIndex);
        }

        // generate unique request ID
        String requestID = SAML2Utils.generateID();
        if ((requestID == null) || (requestID.length() == 0)) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("cannotGenerateID"));
        }

        // retrieve data from the params map
        // destinationURI required if message is signed.
        String destinationURI = SAML2Utils.getParameter(paramsMap, SAML2Constants.DESTINATION);
        String consent = SAML2Utils.getParameter(paramsMap, SAML2Constants.CONSENT);
        Extensions extensions = createExtensions(extensionsList);
        Issuer issuer = SAML2Utils.createIssuer(requesterEntityID);

        // construct LogoutRequest
        LogoutRequest logoutReq = null;
        try {
            logoutReq = ProtocolFactory.getInstance().createLogoutRequest();
        } catch (Exception e) {
            logger.error(classMethod + "Unable to create LogoutRequest : ", e);
            throw new SAML2Exception(SAML2Utils.bundle.getString("errorCreatingLogoutRequest"));
        }

        // set required attributes / elements
        logoutReq.setID(requestID);
        logoutReq.setVersion(SAML2Constants.VERSION_2_0);
        logoutReq.setIssueInstant(newDate());
        setNameIDForSLORequest(logoutReq, nameID, realm, requesterEntityID, hostEntityRole, recipientEntityID);

        // set optional attributes / elements
        logoutReq.setDestination(XMLUtils.escapeSpecialCharacters(destinationURI));
        logoutReq.setConsent(consent);
        logoutReq.setIssuer(issuer);
        if (hostEntityRole.equals(IDP_ROLE)) {
            // use the assertion effective time (in seconds)
            int effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
            String effectiveTimeStr = SAML2Utils.getAttributeValueFromSSOConfig(realm, requesterEntityID,
                    IDP_ROLE, SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE);
            if (effectiveTimeStr != null) {
                try {
                    effectiveTime = Integer.parseInt(effectiveTimeStr);
                    if (logger.isDebugEnabled()) {
                        logger.debug(classMethod + "got effective time from config:" + effectiveTime);
                    }
                } catch (NumberFormatException nfe) {
                    logger.error(classMethod + "Failed to get assertion effective time from "
                            + "IDP SSO config: ", nfe);
                    effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
                }
            }
            Date date = newDate();
            date.setTime(date.getTime() + effectiveTime * 1000);
            logoutReq.setNotOnOrAfter(date);
        }
        if (extensions != null) {
            logoutReq.setExtensions(extensions);
        }

        if (sessionIndex != null) {
            List list = new ArrayList();
            list.add(sessionIndex);
            logoutReq.setSessionIndex(list);
        }

        logger.debug(classMethod + "Recipient's single logout service location = " + location);
        if (destinationURI == null || destinationURI.isEmpty()) {
            logoutReq.setDestination(XMLUtils.escapeSpecialCharacters(location));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "SLO Request before signing : ");
            logger.debug(logoutReq.toXMLString(true, true));
        }

        if (binding.equals(SAML2Constants.HTTP_REDIRECT)) {
            try {
                doSLOByHttpRedirect(logoutReq.toXMLString(true, true),
                        location,
                        relayState,
                        realm,
                        requesterEntityID,
                        hostEntityRole,
                        recipientEntityID,
                        response);
                logoutRequestID.append(requestID);
                String[] data = {location};
                LogUtil.access(Level.INFO, LogUtil.REDIRECT_TO_IDP, data, null);
            } catch (Exception e) {
                logger.error("Exception :", e);
                throw new SAML2Exception(SAML2Utils.bundle.getString("errorRedirectingLogoutRequest"));
            }
        } else if (binding.equals(SAML2Constants.SOAP)) {
            logoutRequestID.append(requestID);
            signSLORequest(logoutReq, realm, requesterEntityID, hostEntityRole, recipientEntityID);
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "SLO Request after signing : ");
                logger.debug(logoutReq.toXMLString(true, true));
            }
            location = SAML2Utils.fillInBasicAuthInfo(config, location, realm);

            doSLOBySOAP(requestID, logoutReq, location, realm, requesterEntityID, hostEntityRole, request, response);
        } else if (binding.equals(SAML2Constants.HTTP_POST)) {
            logoutRequestID.append(requestID);
            signSLORequest(logoutReq, realm, requesterEntityID, hostEntityRole, recipientEntityID);
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "SLO Request after signing : ");
                logger.debug(logoutReq.toXMLString(true, true));
            }
            doSLOByPOST(requestID, logoutReq.toXMLString(true, true), location, relayState, realm, requesterEntityID,
                    hostEntityRole, response, request);
        }
        SPCache.logoutRequestIDHash.put(logoutRequestID.toString(), logoutReq);
        return logoutRequestID;
    }

    static private void doSLOByHttpRedirect(
            String sloRequestXMLString,
            String sloURL,
            String relayState,
            String realm,
            String hostEntity,
            String hostEntityRole,
            String remoteEntity,
            HttpServletResponse response) throws SAML2Exception, IOException {
        String method = "doSLOByHttpRedirect: ";
        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(sloRequestXMLString);

        StringBuffer queryString =
                new StringBuffer().append(SAML2Constants.SAML_REQUEST)
                        .append(SAML2Constants.EQUAL)
                        .append(encodedXML);

        // spec states that the relay state MUST NOT exceed 80
        // chanracters, need to have some means to pass it when it
        // exceeds 80 chars
        if ((relayState != null) && (relayState.length() > 0)) {
            String tmp = SAML2Utils.generateID();
            if (hostEntityRole.equals(IDP_ROLE)) {
                IDPCache.relayStateCache.put(tmp, relayState);
            } else {
                SPCache.relayStateHash.put(tmp, new CacheObject(relayState));
            }
            queryString.append("&").append(SAML2Constants.RELAY_STATE)
                    .append("=").append(tmp);
        }

        String remoteEntityRole = hostEntityRole.equalsIgnoreCase(IDP_ROLE) ? SP_ROLE : IDP_ROLE;
        boolean needToSign = SAML2Utils.getWantLogoutRequestSigned(realm, remoteEntity, remoteEntityRole);

        String signedQueryString = queryString.toString();
        if (needToSign) {
            signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity, hostEntityRole,
                    remoteEntity, remoteEntityRole);
        }

        String redirectURL = sloURL + (sloURL.contains("?") ? "&" : "?") +
                signedQueryString;
        if (logger.isDebugEnabled()) {
            logger.debug(method + "LogoutRequestXMLString : "
                    + sloRequestXMLString);
            logger.debug(method + "LogoutRedirectURL : " + sloURL);
        }

        response.sendRedirect(redirectURL);
    }

    /**
     * Performs SOAP logout, this method will send LogoutResuest to IDP using
     * SOAP binding, and process LogoutResponse.
     * @param requestID Request id.
     * @param sloRequest  a string representation of LogoutRequest.
     * @param sloURL SOAP logout URL on IDP side.
     * @param realm  a string representation of LogoutRequest.
     * @param hostEntity  host entity is sending the request.
     * @param hostRole SOAP logout URL on IDP side.
     * @throws SAML2Exception if logout failed. 
     * @throws SessionException if logout failed. 
     */
    private static void doSLOBySOAP(
            String requestID,
            LogoutRequest sloRequest,
            String sloURL,
            String realm,
            String hostEntity,
            String hostRole,
            HttpServletRequest request,
            HttpServletResponse response) throws SAML2Exception, SessionException {

        String sloRequestXMLString = sloRequest.toXMLString(true, true);
        if (logger.isDebugEnabled()) {
            logger.debug("LogoutUtil.doSLOBySOAP : SLORequestXML: "
                    + sloRequestXMLString + "\nSOAPURL : " + sloURL);
        }

        SOAPMessage resMsg = null;
        try {
            resMsg = getSoapCommunicator().sendSOAPMessage(sloRequestXMLString, sloURL,
                    true);
        } catch (SOAPException se) {
            logger.error("Unable to send SOAPMessage to IDP ", se);
            throw new SAML2Exception(se.getMessage());
        }

        // get the LogoutResponse element from SOAP message
        Element respElem = getSoapCommunicator().getSamlpElement(resMsg, "LogoutResponse");
        LogoutResponse sloResponse = ProtocolFactory.getInstance()
                .createLogoutResponse(respElem);

        String userId = null;
        // invoke SPAdapter for preSingleLogoutProcess : SP initiated SOAP
        if ((hostRole != null) && hostRole.equals(SAML2Constants.SP_ROLE)) {
            userId = SPSingleLogout.preSingleLogoutProcess(hostEntity, realm,
                    request, response, null, sloRequest, sloResponse,
                    SAML2Constants.SOAP);
        }
        if (sloResponse == null) {
            logger.error("LogoutUtil.doSLOBySoap : null response");
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "nullLogoutResponse"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("LogoutUtil.doSLOBySOAP : " +
                    "LogoutResponse without SOAP envelope:\n" +
                    sloResponse.toXMLString());
        }

        Issuer resIssuer = sloResponse.getIssuer();
        String requestId = sloResponse.getInResponseTo();
        SAML2Utils.verifyResponseIssuer(
                realm, hostEntity, resIssuer, requestId);

        String remoteEntityID = sloResponse.getIssuer().getValue();
        verifySLOResponse(sloResponse, realm, remoteEntityID, hostEntity,
                hostRole);

        boolean success = checkSLOResponse(sloResponse, requestID);

        if (logger.isDebugEnabled()) {
            logger.debug("Request success : " + success);
        }

        if (success == false) {
            if (SPCache.isFedlet) {
                FedletAdapter adapter = SAML2Utils.getFedletAdapter(hostEntity, realm);
                if (adapter != null) {
                    adapter.onFedletSLOFailure(request, response, sloRequest, sloResponse,
                            hostEntity, remoteEntityID, SAML2Constants.SOAP);
                }
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString("sloFailed"));
        } else {
            // invoke SPAdapter for postSLOSuccess : SP inited SOAP 
            if ((hostRole != null) && hostRole.equals(SAML2Constants.SP_ROLE)) {
                if (SPCache.isFedlet) {
                    FedletAdapter adapter = SAML2Utils.getFedletAdapter(hostEntity, realm);
                    if (adapter != null) {
                        adapter.onFedletSLOSuccess(request, response, sloRequest, sloResponse,
                                hostEntity, remoteEntityID, SAML2Constants.SOAP);
                    }
                } else {
                    SPSingleLogout.postSingleLogoutSuccess(hostEntity, realm,
                            request, response, userId, sloRequest, sloResponse,
                            SAML2Constants.SOAP);
                }
            }
        }
    }

    private static boolean checkSLOResponse(LogoutResponse sloResponse,
            String requestID)
            throws SAML2Exception {
        boolean success = false;
        String retCode = sloResponse.getStatus().getStatusCode().getValue();

        if (retCode.equalsIgnoreCase(SAML2Constants.SUCCESS)) {
            String inResponseTo = sloResponse.getInResponseTo();
            if (inResponseTo.equals(requestID)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("LogoutUtil.doSLOBySOAP " +
                            "LogoutResponse inResponseTo matches LogoutRequest ID");
                }
            } else {
                logger.error("LogoutUtil.doSLOBySOAP " +
                        "LogoutResponse inResponseTo does not match Request ID.");
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "inResponseToNoMatch"));
            }
            success = true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("LogoutUtil.doSLOBySOAP : " +
                        "return code : " + retCode);
            }
            success = false;
        }

        return success;
    }

    static LogoutResponse forwardToRemoteServer(LogoutRequest logoutReq,
            String remoteLogoutURL) {

        if (logger.isDebugEnabled()) {
            logger.debug("LogoutUtil.forwardToRemoteServer: " +
                    "remoteLogoutURL = " + remoteLogoutURL);
        }

        try {
            SOAPMessage resMsg = getSoapCommunicator().sendSOAPMessage(
                    logoutReq.toXMLString(true, true), remoteLogoutURL, true);

            // get the LogoutResponse element from SOAP message
            Element respElem = getSoapCommunicator().getSamlpElement(resMsg,
                    "LogoutResponse");
            return ProtocolFactory.getInstance().createLogoutResponse(respElem);
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("LogoutUtil.forwardToRemoteServer:", ex);
            }
        }
        return null;
    }

    static List getSessionIndex(LogoutResponse logoutRes) {
        StatusDetail statusDetail = logoutRes.getStatus().getStatusDetail();
        if (statusDetail == null) {
            return null;
        }
        List details = statusDetail.getAny();
        if (details == null || details.isEmpty()) {
            return null;
        }

        List sessionIndexList = new ArrayList();
        for(Iterator iter = details.iterator(); iter.hasNext();) {
            String detail = (String)iter.next();
            Document doc = XMLUtils.toDOMDocument(detail);
            Element elem = doc.getDocumentElement();
            String localName = elem.getLocalName();
            if (SAML2Constants.SESSION_INDEX.equals(localName)) {
                sessionIndexList.add(XMLUtils.getElementString(elem));
            }
        }

        return sessionIndexList;
    }

    static void setSessionIndex(Status status, List sessionIndex) {
        try {
            StatusDetail sd=ProtocolFactory.getInstance().createStatusDetail();
            status.setStatusDetail(sd);
            if (sessionIndex != null && !sessionIndex.isEmpty()) {
                List details = new ArrayList();
                for(Iterator iter = sessionIndex.iterator(); iter.hasNext();) {
                    String si = (String)iter.next();
                    SessionIndex sIndex = ProtocolFactory.getInstance()
                            .createSessionIndex(si);
                    details.add(sIndex.toXMLString(true, true));
                }
                sd.setAny(details);
            }
        } catch (SAML2Exception e) {
            logger.error("LogoutUtil.setSessionIndex: ", e);
        }

    }

    /**
     * Based on the preferred SAML binding this method tries to choose the most appropriate
     * {@link EndpointType} that can be used to send the logout request to. The algorithm itself is
     * simple:
     * <ul>
     *  <li>When asynchronous binding was used with the initial logout request, it is preferred to use asynchronous
     *      bindings, but if they are not available, a synchronous binding should be used.</li>
     *  <li>When synchronous binding is used with the initial request, only synchronous bindings can be used for the
     *      rest of the entities.</li>
     * </ul>
     *
     * @param sloList The list of SLO endpoints for a given entity.
     * @param preferredBinding The binding that was used to initiate the logout request.
     * @return The most appropriate SLO service location that can be used for sending the logout request. If there is
     * no appropriate logout endpoint, null is returned.
     */
    public static EndpointType getMostAppropriateSLOServiceLocation(
            List<EndpointType> sloList, String preferredBinding) {
        //shortcut for the case when SLO isn't supported at all
        if (sloList.isEmpty()) {
            return null;
        }

        Map<String, EndpointType> sloBindings =
                new HashMap<String, EndpointType>(sloList.size());
        for (EndpointType sloEndpoint : sloList) {
            sloBindings.put(sloEndpoint.getBinding(), sloEndpoint);
        }

        EndpointType endpoint = sloBindings.get(preferredBinding);
        if (endpoint == null) {
            //if the requested binding isn't supported let's try to find the most appropriate SLO endpoint
            if (preferredBinding.equals(SAML2Constants.HTTP_POST)) {
                endpoint = sloBindings.get(SAML2Constants.HTTP_REDIRECT);
            } else if (preferredBinding.equals(SAML2Constants.HTTP_REDIRECT)) {
                endpoint = sloBindings.get(SAML2Constants.HTTP_POST);
            }

            if (endpoint == null) {
                //we ran out of asynchronous bindings, so our only chance is to try to use SOAP binding
                //in case the preferred binding was SOAP from the beginning, then this code will just return null again
                endpoint = sloBindings.get(SAML2Constants.SOAP);
            }
        }

        return endpoint;
    }

    /**
     * Gets Single Logout Service location URL.
     *
     * @param sloList list of configured <code>SingleLogoutElement</code>.
     * @param desiredBinding desired binding of SingleLogout.
     * @return url of desiredBinding.
     */
    public static String getSLOServiceLocation(
            List sloList,
            String desiredBinding) {
        String classMethod =
                "LogoutUtil.getSLOserviceLocation: ";
        int n = sloList.size();
        if (logger.isDebugEnabled()) {
            logger.debug(
                    classMethod +
                            "Number of single logout services = " +
                            n);
        }
        EndpointType slos = null;
        String location = null;
        String binding = null;
        for (int i=0; i<n; i++) {
            slos = (EndpointType)sloList.get(i);
            if (slos != null) {
                binding = slos.getBinding();
            }
            if (logger.isDebugEnabled()) {
                logger.debug(
                        classMethod +
                                "Single logout service binding = " +
                                binding);
            }
            if ((binding != null) && (binding.equals(desiredBinding))) {
                location = slos.getLocation();
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            classMethod +
                                    "Found the single logout service "+
                                    "with the desired binding");
                }
                break;
            }
        }
        return location;
    }

    /**
     * Gets Single Logout Response Service location URL.
     *
     * @param sloList list of configured <code>SingleLogoutElement</code>.
     * @param desiredBinding desired binding of SingleLogout.
     * @return url of desiredBinding.
     */
    public static String getSLOResponseServiceLocation(
            List sloList,
            String desiredBinding) {
        String classMethod =
                "LogoutUtil.getSLOResponseServiceLocation: ";
        int n = sloList.size();
        if (logger.isDebugEnabled()) {
            logger.debug(
                    classMethod +
                            "Number of single logout services = " +
                            n);
        }
        EndpointType slos = null;
        String resLocation = null;
        String binding = null;
        for (int i=0; i<n; i++) {
            slos = (EndpointType)sloList.get(i);
            if (slos != null) {
                binding = slos.getBinding();
            }
            if (logger.isDebugEnabled()) {
                logger.debug(
                        classMethod +
                                "Single logout service binding = " +
                                binding);
            }
            if ((binding != null) && (binding.equals(desiredBinding))) {
                resLocation = slos.getResponseLocation();
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            classMethod +
                                    "Found the single logout service "+
                                    "with the desired binding");
                }
                break;
            }
        }
        return resLocation;
    }

    /* Creates Extensions */
    private static com.sun.identity.saml2.protocol.Extensions
    createExtensions(List extensionsList) throws SAML2Exception {
        Extensions extensions = null;
        if (extensionsList != null && !extensionsList.isEmpty()) {
            extensions = ProtocolFactory.getInstance().createExtensions();
            extensions.setAny(extensionsList);
        }
        return extensions;
    }

    /**
     * Builds the <code>LogoutResponse</code> to be sent to IDP.
     *
     * @param status status of the response.
     * @param inResponseTo inResponseTo.
     * @param issuer issuer of the response, which is SP.
     * @param realm inResponseTo.
     * @param hostRole issuer of the response, which is SP.
     * @param remoteEntity will get this response.
     *
     * @return <code>LogoutResponse</code>
     *
     */
    public static LogoutResponse generateResponse(
            Status status,
            String inResponseTo,
            Issuer issuer,
            String realm,
            String hostRole,
            String remoteEntity) {

        if (status == null) {
            status = SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
                    SAML2Utils.bundle.getString("requestSuccess"));
        }
        LogoutResponse logoutResponse = ProtocolFactory.getInstance()
                .createLogoutResponse();
        String responseID = SAMLUtils.generateID();

        try {
            logoutResponse.setStatus(status);
            logoutResponse.setID(responseID);
            logoutResponse.setInResponseTo(inResponseTo);
            logoutResponse.setVersion(SAML2Constants.VERSION_2_0);
            logoutResponse.setIssueInstant(newDate());
            logoutResponse.setIssuer(issuer);
        } catch (SAML2Exception e) {
            logger.error("Error in generating LogoutResponse.", e);
        }

        return logoutResponse;
    }

    /**
     * Sign LogoutRequest.
     *
     * @param sloRequest SLO request will be signed.
     * @param realm realm of host entity.
     * @param hostEntity entity ID of host entity.
     * @param hostEntityRole role of host entity.
     * @param remoteEntityId entity ID of remote host entity.
     * @throws SAML2Exception if error in signing the request.
     */
    public static void signSLORequest(LogoutRequest sloRequest,
            String realm, String hostEntity,
            String hostEntityRole, String remoteEntityId)
            throws SAML2Exception {
        String method = "signSLORequest : ";
        boolean needRequestSign = false;

        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needRequestSign = SAML2Utils.getWantLogoutRequestSigned(realm,
                    remoteEntityId, SAML2Constants.SP_ROLE);
        } else {
            needRequestSign = SAML2Utils.getWantLogoutRequestSigned(realm,
                    remoteEntityId, IDP_ROLE);
        }

        if (!needRequestSign) {
            if (logger.isDebugEnabled()) {
                logger.debug(method + "SLORequest doesn't need to be signed.");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
            logger.debug(method + "SLO Request before sign : "
                    + sloRequest.toXMLString(true, true));
        }

        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, hostEntity, Saml2EntityRole.fromString(hostEntityRole));
        Key signingKey = credentials.getSigningKey();

        if (signingKey == null) {
            logger.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        X509Certificate signingCert = credentials.getSigningCertificate();
        sloRequest.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, remoteEntityId),
                        IDP_ROLE.equals(hostEntityRole) ? Saml2EntityRole.SP : Saml2EntityRole.IDP));

        if (logger.isDebugEnabled()) {
            logger.debug(method + "SLO Request after sign : "
                    + sloRequest.toXMLString(true, true));
        }
    }

    /**
     * Verify the signature in LogoutRequest.
     *
     * @param sloRequest SLO request will be verified.
     * @param realm realm of host entity.
     * @param remoteEntity entity ID of remote host entity.
     * @param hostEntity entity ID of host entity.
     * @param hostEntityRole role of host entity.
     * @return returns true if signature is valid.
     * @throws SAML2Exception if error in verifying the signature.
     * @throws SessionException if error in verifying the signature.
     */
    public static boolean verifySLORequest(LogoutRequest sloRequest,
            String realm, String remoteEntity,
            String hostEntity, String hostEntityRole)
            throws SAML2Exception, SessionException {
        String method = "verifySLORequest : ";
        boolean needVerifySignature =
                SAML2Utils.getWantLogoutRequestSigned(realm, hostEntity,
                        hostEntityRole);

        if (needVerifySignature == false) {
            if (logger.isDebugEnabled()) {
                logger.debug(method+"SLORequest doesn't need to be verified.");
            }
            return true;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "remoteEntity is : " + remoteEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
        }

        boolean valid = false;
        Set<X509Certificate> signingCerts;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc = metaManager.getSPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(spSSODesc, remoteEntity, SAML2Constants.SP_ROLE, realm);
        } else {
            IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, remoteEntity, IDP_ROLE, realm);
        }

        if (!signingCerts.isEmpty()) {
            valid = sloRequest.isSignatureValid(signingCerts);
            if (logger.isDebugEnabled()) {
                logger.debug(method + "Signature is : " + valid);
            }
        } else {
            logger.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        return valid;
    }

    /**
     * Sign LogoutResponse.
     *
     * @param sloResponse SLO response will be signed.
     * @param realm realm of host entity.
     * @param hostEntity entity ID of host entity.
     * @param hostEntityRole role of host entity.
     * @param remoteEntityId entity ID of remote host entity.
     * @throws SAML2Exception if error in signing the request.
     */
    public static void signSLOResponse(LogoutResponse sloResponse,
            String realm, String hostEntity,
            String hostEntityRole, String remoteEntityId)
            throws SAML2Exception {
        String method = "signSLOResponse : ";
        boolean needSignResponse = false;

        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needSignResponse = SAML2Utils.getWantLogoutResponseSigned(realm,
                    remoteEntityId, SAML2Constants.SP_ROLE);
        } else {
            needSignResponse = SAML2Utils.getWantLogoutResponseSigned(realm,
                    remoteEntityId, IDP_ROLE);
        }

        if (!needSignResponse) {
            if (logger.isDebugEnabled()) {
                logger.debug(method +
                        "SLOResponse doesn't need to be signed.");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
        }

        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, hostEntity, Saml2EntityRole.fromString(hostEntityRole));
        Key signingKey = credentials.getSigningKey();

        if (signingKey == null) {
            logger.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        X509Certificate signingCert = credentials.getSigningCertificate();
        sloResponse.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, remoteEntityId),
                        IDP_ROLE.equals(hostEntityRole) ? Saml2EntityRole.SP : Saml2EntityRole.IDP));
    }

    /**
     * Verify the signature in LogoutResponse.
     *
     * @param sloResponse SLO response will be verified.
     * @param realm realm of host entity.
     * @param remoteEntity entity ID of remote host entity.
     * @param hostEntity entity ID of host entity.
     * @param hostEntityRole role of host entity.
     * @return returns true if signature is valid.
     * @throws SAML2Exception if error in verifying the signature.
     * @throws SessionException if error in verifying the signature.
     */
    public static boolean verifySLOResponse(LogoutResponse sloResponse,
            String realm, String remoteEntity,
            String hostEntity, String hostEntityRole)
            throws SAML2Exception, SessionException {
        String method = "verifySLOResponse : ";
        boolean needVerifySignature =
                SAML2Utils.getWantLogoutResponseSigned(realm, hostEntity,
                        hostEntityRole);

        if (needVerifySignature == false) {
            if (logger.isDebugEnabled()) {
                logger.debug(method +
                        "SLOResponse doesn't need to be verified.");
            }
            return true;
        }

        Set<X509Certificate> signingCerts;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc = metaManager.getSPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(spSSODesc, remoteEntity, SAML2Constants.SP_ROLE, realm);
        } else {
            IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, remoteEntity, IDP_ROLE, realm);
        }

        if (!signingCerts.isEmpty()) {
            boolean valid = sloResponse.isSignatureValid(signingCerts);
            if (logger.isDebugEnabled()) {
                logger.debug(method + "Signature is : " + valid);
            }
            return valid;
        } else {
            logger.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    public static void setNameIDForSLORequest(LogoutRequest request,
            NameID nameID, String realm, String hostEntity,
            String hostEntityRole, String remoteEntity)
            throws SAML2Exception, SessionException {
        String method = "setNameIDForSLORequest: ";
        boolean needEncryptIt = false;

        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needEncryptIt =
                    SAML2Utils.getWantNameIDEncrypted(realm, remoteEntity,
                            SAML2Constants.SP_ROLE);
        } else {
            needEncryptIt =
                    SAML2Utils.getWantNameIDEncrypted(realm, remoteEntity,
                            IDP_ROLE);
        }

        if (needEncryptIt == false) {
            if (logger.isDebugEnabled()) {
                logger.debug(method + "NamID doesn't need to be encrypted.");
            }
            request.setNameID(nameID);
            return;
        }

        EncryptionConfig encryptionConfig;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc =
                    metaManager.getSPSSODescriptor(realm, remoteEntity);
            encryptionConfig = KeyUtil.getEncryptionConfig(spSSODesc, remoteEntity, SAML2Constants.SP_ROLE, realm);
        } else {
            IDPSSODescriptorType idpSSODesc =
                    metaManager.getIDPSSODescriptor(realm, remoteEntity);
            encryptionConfig = KeyUtil.getEncryptionConfig(idpSSODesc, remoteEntity, IDP_ROLE, realm);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
            logger.debug(method + "remoteEntity is : " + remoteEntity);
        }

        if (encryptionConfig == null) {
            logger.error("NO meta data for encrypt Info.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        EncryptedID encryptedID = nameID.encrypt(encryptionConfig, remoteEntity);
        request.setEncryptedID(encryptedID);
    }

    static NameID getNameIDFromSLORequest(LogoutRequest request,
            String realm, String hostEntity,
            String hostEntityRole)
            throws SAML2Exception {
        String method = "getNameIDFromSLORequest: ";

        boolean needDecryptIt =
                SAML2Utils.getWantNameIDEncrypted(realm,hostEntity,hostEntityRole);

        if (needDecryptIt == false) {
            if (logger.isDebugEnabled()) {
                logger.debug(method + "NamID doesn't need to be decrypted.");
            }
            return request.getNameID();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
        }

        EncryptedID encryptedID = request.getEncryptedID();

        final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveValidDecryptionCredentials(realm, hostEntity, Saml2EntityRole.fromString(hostEntityRole));
        return encryptedID.decrypt(decryptionKeys);
    }

    public static void sendSLOResponse(HttpServletResponse response,
            LogoutResponse sloResponse, String sloURL, String relayState,
            String realm, String hostEntity, String hostEntityRole,
            String remoteEntity) throws SAML2Exception {

        sendSLOResponseRedirect(response, sloResponse, sloURL, relayState,
                realm, hostEntity, hostEntityRole, remoteEntity);
    }

    public static void sendSLOResponse(HttpServletResponse response, HttpServletRequest request,
            LogoutResponse sloResponse, String sloURL, String relayState,
            String realm, String hostEntity, String hostEntityRole,
            String remoteEntity, String binding) throws SAML2Exception {

        if (SAML2Constants.HTTP_POST.equals(binding)) {
            sendSLOResponsePost(response, request, sloResponse, sloURL, relayState,
                    realm, hostEntity, hostEntityRole, remoteEntity);
        } else {
            sendSLOResponseRedirect(response, sloResponse, sloURL, relayState,
                    realm, hostEntity, hostEntityRole, remoteEntity);
        }
    }

    public static void sendSLOResponsePost(HttpServletResponse response, HttpServletRequest request,
            LogoutResponse sloResponse, String sloURL, String relayState,
            String realm, String hostEntity, String hostEntityRole,
            String remoteEntity) throws SAML2Exception {

        signSLOResponse(sloResponse, realm, hostEntity, hostEntityRole,
                remoteEntity);

        String logoutResponseStr = sloResponse.toXMLString(true,true);
        String encMsg = SAML2Utils.encodeForPOST(logoutResponseStr);

        SAML2Utils.postToTarget(request, response, "SAMLResponse", encMsg, "RelayState", relayState, sloURL);
    }

    public static void sendSLOResponseRedirect(HttpServletResponse response,
            LogoutResponse sloResponse,
            String sloURL,
            String relayState,
            String realm,
            String hostEntity,
            String hostEntityRole,
            String remoteEntity)
            throws SAML2Exception {

        try {
            String logoutResXMLString = sloResponse.toXMLString(true,true);

            // encode the xml string
            String encodedXML =
                    SAML2Utils.encodeForRedirect(logoutResXMLString);

            StringBuilder queryString =
                    new StringBuilder().append(SAML2Constants.SAML_RESPONSE)
                            .append(SAML2Constants.EQUAL)
                            .append(encodedXML);

            // http://docs.oasis-open.org/security/saml/v2.0/sstc-saml-approved-errata-2.0.html
            // Spec states RelayState data MUST NOT exceed 80 bytes in length
            if (StringUtils.isNotEmpty(relayState)) {
                if (relayState.getBytes(UTF_8).length <= 80) {
                    queryString.append("&").append(SAML2Constants.RELAY_STATE)
                            .append("=").append(urlEncodeQueryParameterNameOrValue(relayState));
                } else {
                    logger.warn("RelayState MUST NOT exceed 80 bytes. Dropping relayState : '{}'", relayState);
                }
            }

            String remoteEntityRole = hostEntityRole.equalsIgnoreCase(IDP_ROLE) ? SP_ROLE : IDP_ROLE;
            boolean needToSign = SAML2Utils.getWantLogoutResponseSigned(realm, remoteEntity, remoteEntityRole);

            String signedQueryString = queryString.toString();
            if (needToSign) {
                signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity, hostEntityRole,
                        remoteEntity, remoteEntityRole);
            }

            String redirectURL = sloURL+ (sloURL.contains("?") ? "&" : "?") +
                    signedQueryString;

            if (logger.isDebugEnabled()) {
                logger.debug("redirectURL :" + redirectURL);
            }
            String[] data = {sloURL};
            LogUtil.access(Level.INFO,LogUtil.REDIRECT_TO_SP,data,
                    null);

            response.sendRedirect(redirectURL);
        } catch (Exception e) {
            logger.error("Exception :",e);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "errorRedirectingLogoutResponse"));
        }
    }

    /**
     * Returns binding information of SLO Service for remote entity 
     * from request or meta configuration.
     *
     * @param request the HttpServletRequest.
     * @param metaAlias entityID of hosted entity.
     * @param hostEntityRole Role of hosted entity.
     * @param remoteEntityID entityID of remote entity.
     * @return return true if the processing is successful.
     * @throws SAML2Exception if no binding information is configured.
     */
    public static String getSLOBindingInfo(HttpServletRequest request,
            String metaAlias,
            String hostEntityRole,
            String remoteEntityID)
            throws SAML2Exception {
        String binding = request.getParameter(SAML2Constants.BINDING);

        try {
            if (binding == null) {
                String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
                EndpointType sloService =
                        getSLOServiceElement(realm, remoteEntityID,
                                hostEntityRole, null);
                if (sloService != null) {
                    binding = sloService.getBinding();
                }
            }
        } catch (SessionException e) {
            logger.error("Invalid SSOToken", e);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        if (binding == null) {
            logger.error("Incorrect configuration for SingleLogout Service.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
        return binding;
    }

    private static EndpointType getSLOServiceElement(
            String realm, String entityID,
            String hostEntityRole, String binding)
            throws SAML2MetaException, SessionException, SAML2Exception {
        EndpointType sloService = null;
        String method = "getSLOServiceElement: ";

        if (logger.isDebugEnabled()) {
            logger.debug(method + "Realm : " + realm);
            logger.debug(method + "Entity ID : " + entityID);
            logger.debug(method + "Host Entity Role : " + hostEntityRole);
        }

        if (hostEntityRole.equalsIgnoreCase(SAML2Constants.SP_ROLE)) {
            sloService = getIDPSLOConfig(realm, entityID, binding);
        } else if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)){
            sloService = getSPSLOConfig(realm, entityID, binding);
        } else {
            logger.error("Hosted Entity role is missing .");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullIDPEntityID"));
        }

        return sloService;
    }

    /**
     * Returns first SingleLogout configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>SingleLogoutServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     * @throws SessionException invalid or expired single-sign-on session
     */
    static public EndpointType getIDPSLOConfig(
            String realm,
            String entityId,
            String binding)
            throws SAML2MetaException, SessionException {
        EndpointType slo = null;

        IDPSSODescriptorType idpSSODesc =
                metaManager.getIDPSSODescriptor(realm, entityId);
        if (idpSSODesc == null) {
            logger.error("Identity Provider SSO config is missing.");
            return null;
        }

        List<EndpointType> list = idpSSODesc.getSingleLogoutService();

        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return list.get(0);
            }
            for (EndpointType aList : list) {
                slo = aList;
                if (binding.equalsIgnoreCase(slo.getBinding())) {
                    break;
                }
            }
        }

        return slo;
    }

    /**
     * Returns first SingleLogout configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>SingleLogoutServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     * @throws SessionException invalid or expired single-sign-on session
     */
    static public EndpointType getSPSLOConfig(
            String realm, String entityId,
            String binding)
            throws SAML2MetaException, SessionException {
        EndpointType slo = null;

        SPSSODescriptorType spSSODesc =
                metaManager.getSPSSODescriptor(realm, entityId);
        if (spSSODesc == null) {
            return null;
        }

        List<EndpointType> list = spSSODesc.getSingleLogoutService();

        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return list.get(0);
            }
            for (EndpointType aList : list) {
                slo = aList;
                if (binding.equalsIgnoreCase(slo.getBinding())) {
                    break;
                }
            }
        }

        return slo;
    }

    /**
     * Returns the extensions list
     * @param paramsMap request paramsMap has extensions
     * @return <code>List</code> for extensions params
     */
    public static List getExtensionsList(Map paramsMap) {
        List extensionsList = null;
        // TODO Get the Extensions list from request parameter
        return extensionsList;
    }

    private static void doSLOByPOST(String requestID,
            String sloRequestXMLString, String sloURL, String relayState,
            String realm, String hostEntity, String hostRole,
            HttpServletResponse response, HttpServletRequest request) throws SAML2Exception, SessionException {

        if (logger.isDebugEnabled()) {
            logger.debug("LogoutUtil.doSLOByPOST : SLORequestXML: "
                    + sloRequestXMLString + "\nPOSTURL : " + sloURL);
            logger.debug("LogoutUtil.doSLOByPOST : relayState : "
                    + sloRequestXMLString + "\nPOSTURL : " + relayState);
        }

        String encMsg = SAML2Utils.encodeForPOST(sloRequestXMLString);
        SAML2Utils.postToTarget(request, response, "SAMLRequest", encMsg,
                "RelayState", relayState, sloURL);
    }

    static LogoutResponse getLogoutResponseFromPost(String samlResponse,
            HttpServletResponse response) throws SAML2Exception {

        if (samlResponse == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "missingLogoutResponse"));
        }

        LogoutResponse resp = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlResponse);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis);
                if (doc != null) {
                    resp = ProtocolFactory.getInstance().
                            createLogoutResponse(doc.getDocumentElement());
                }
            }
        } catch (Exception e) {
            logger.error("LogoutUtil.getLogoutResponseFromPost:", e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("LogoutUtil.getLogoutResponseFromPost:",
                                ie);
                    }
                }
            }
        }

        if (resp == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "errorGettingLogoutResponse"));
        }
        return resp;
    }

    static LogoutRequest getLogoutRequestFromPost(String samlRequest,
            HttpServletResponse response) throws SAML2Exception {

        if (samlRequest == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "missingLogoutRequest"));
        }

        LogoutRequest req = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlRequest);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis);
                if (doc != null) {
                    req = ProtocolFactory.getInstance().
                            createLogoutRequest(doc.getDocumentElement());
                }
            }
        } catch (Exception e) {
            logger.error("LogoutUtil.getLogoutRequestFromPost:", e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("LogoutUtil.getLogoutRequestFromPost:",
                                ie);
                    }
                }
            }
        }
        if (req == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "errorGettingLogoutRequest"));
        }
        return req;
    }

    private static SOAPCommunicator getSoapCommunicator() {
        if (soapCommunicator == null) {
            soapCommunicator = InjectorHolder.getInstance(SOAPCommunicator.class);
        }
        return soapCommunicator;
    }
}

