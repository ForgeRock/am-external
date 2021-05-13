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
 * $Id: SPSSOFederate.java,v 1.29 2009/11/24 21:53:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2011-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static com.sun.identity.saml2.common.SAML2FailoverUtils.isFailoverEnabled;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.liberty.ws.paos.PAOSConstants;
import com.sun.identity.liberty.ws.paos.PAOSException;
import com.sun.identity.liberty.ws.paos.PAOSHeader;
import com.sun.identity.liberty.ws.paos.PAOSRequest;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.saml2.ecp.ECPRequest;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2IDPFinder;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAuthnContextMapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.GetComplete;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class reads the query parameters and performs the required
 * processing logic for sending Authentication Request
 * from SP to IDP.
 *
 */

public class SPSSOFederate {

    private static final Logger logger = LoggerFactory.getLogger(SPSSOFederate.class);
    static SAML2MetaManager sm = null;
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            logger.error("SPSSOFederate: Error retreiving metadata"
                    ,sme);
        }
    }

    /**
     * Parses the request parameters and builds the Authentication
     * Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param metaAlias metaAlias to locate the service providers.
     * @param idpEntityID entityID of Identity Provider.
     * @param paramsMap Map of all other parameters.The key in the
     *              map are of the type String. The values in the paramsMap
     *              are of the type List.
     *              Some of the possible keys are:RelayState,NameIDFormat,
     *              reqBinding, binding, AssertionConsumerServiceIndex,
     *              AttributeConsumingServiceIndex (currently not supported),
     *              isPassive, ForceAuthN, AllowCreate, Destination,
     *              AuthnContextDeclRef, AuthnContextClassRef,
     *              AuthComparison, Consent (currently not supported),
     *              AuthLevel, and sunamcompositeadvice.
     * @param auditor the SAML2EventLogger to use to log the saml request - may be null
     * @throws SAML2Exception if error initiating request to IDP.
     */
    public static void initiateAuthnRequest(final HttpServletRequest request,
            final HttpServletResponse response,
            final String metaAlias,
            final String idpEntityID,
            final Map paramsMap,
            final SAML2EventLogger auditor) throws SAML2Exception {

        try {
            // get the sp entity ID from the metaAlias
            String spEntityID = getSPEntityId(metaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

            if (logger.isDebugEnabled()) {
                logger.debug("SPSSOFederate : spEntityID is :" + spEntityID);
                logger.debug("SPSSOFederate realm is :" + realm);
            }

            initiateAuthnRequest(request, response, spEntityID,  idpEntityID, realm, paramsMap, auditor);
        } catch (SAML2MetaException sme) {
            logger.error("SPSSOFederate: Error retreiving spEntityID from MetaAlias",sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaAliasError"));
        }
    }

    /**
     * Gets the SP Entity ID from the metaAlias.
     *
     * @param metaAlias the metaAlias String
     * @return the EntityId of the SP from the meta Alias
     * @throws SAML2MetaException if there was a problem extracting
     */
    public static String getSPEntityId(String metaAlias) throws SAML2MetaException {
        return sm.getEntityByMetaAlias(metaAlias);
    }

    /**
     * Parses the request parameters and builds the Authentication
     * Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param spEntityID entityID of Service Provider.
     * @param idpEntityID entityID of Identity Provider.
     * @param paramsMap Map of all other parameters.The key in the
     *              map are the parameter names of the type String. 
     *              The values in the paramsMap are of the type List.
     *              Some of the possible keys are:RelayState,NameIDFormat,
     *              reqBinding, binding, AssertionConsumerServiceIndex,
     *              AttributeConsumingServiceIndex (currently not supported),
     *              isPassive, ForceAuthN, AllowCreate, Destination,
     *              AuthnContextDeclRef, AuthnContextClassRef,
     *              AuthComparison, Consent (currently not supported),
     *              AuthLevel, and sunamcompositeadvice.
     * @param auditor the auditor for logging SAML2 Events - may be null
     * @throws SAML2Exception if error initiating request to IDP.
     */
    private static void initiateAuthnRequest(
            final HttpServletRequest request, final HttpServletResponse response, final String spEntityID,
            final String idpEntityID, final String realmName, final Map paramsMap, final SAML2EventLogger auditor)
            throws SAML2Exception {

        FSUtils.setLbCookieIfNecessary(request, response);

        if (spEntityID == null) {
            logger.error("SPSSOFederate:Service Provider ID  is missing.");
            String[] data = { spEntityID };
            LogUtil.error(Level.INFO, LogUtil.INVALID_SP, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullSPEntityID"));
        }

        if (idpEntityID == null)  {
            logger.error("SPSSOFederate: Identity Provider ID is missing .");
            String[] data = { idpEntityID };
            LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullIDPEntityID"));
        }


        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate: in initiateSSOFed");
            logger.debug("SPSSOFederate: spEntityID is : " + spEntityID);
            logger.debug("SPSSOFederate: idpEntityID : "  + idpEntityID);
        }

        String realm = getRealm(realmName);

        try {
            // Retrieve MetaData
            if (sm == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString("errorMetaManager"));
            }

            Map spConfigAttrsMap = getAttrsMapForAuthnReq(realm, spEntityID);

            // get SPSSODescriptor
            SPSSODescriptorType spsso = getSPSSOForAuthnReq(realm, spEntityID);

            if (spsso == null) {
                String[] data = { spEntityID };
                LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }

            List extensionsList = getExtensionsList(spEntityID, realm);

            // get IDP Descriptor
            IDPSSODescriptorType idpsso = getIDPSSOForAuthnReq(realm, idpEntityID);

            if (idpsso == null) {
                String[] data = { idpEntityID };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }

            String binding = getParameter(paramsMap, SAML2Constants.REQ_BINDING);
            List<EndpointType> ssoServiceList = idpsso.getSingleSignOnService();
            final EndpointType endPoint = getSingleSignOnServiceEndpoint(ssoServiceList, binding);

            if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                String[] data = { idpEntityID };
                LogUtil.error(Level.INFO, LogUtil.SSO_NOT_FOUND, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("ssoServiceNotfound"));
            }

            String ssoURL = endPoint.getLocation();
            logger.debug("SPSSOFederate: SingleSignOnService URL : {}", ssoURL);
            if (binding == null) {
                logger.debug("SPSSOFederate: reqBinding is null using endpoint binding: {} ",
                        endPoint.getBinding());
                binding = endPoint.getBinding();
                if (binding == null) {
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO, LogUtil.NO_RETURN_BINDING, data, null);
                    throw new SAML2Exception(SAML2Utils.bundle.getString("UnableTofindBinding"));
                }
            }

            // create AuthnRequest 
            AuthnRequest authnRequest = createAuthnRequest(request, response, realm, spEntityID, idpEntityID,
                    paramsMap, spConfigAttrsMap, extensionsList, spsso, idpsso, ssoURL, false);
            if (null != auditor && null != authnRequest) {
                auditor.setRequestId(authnRequest.getID());
            }


            String authReqXMLString = authnRequest.toXMLString(true, true);

            if (logger.isDebugEnabled()) {
                logger.debug("SPSSOFederate: AuthnRequest:" + authReqXMLString);
            }

            // Default URL if relayState not present? in providerConfig?
            // TODO get Default URL from metadata 
            String relayState = getParameter(paramsMap, SAML2Constants.RELAY_STATE);

            String requestUrl = request.getRequestURL().toString();
            // Validate the RelayState URL.
            SAML2Utils.validateRelayStateURL(realm, spEntityID, relayState, SP_ROLE, requestUrl);

            // check if relayState is present and get the unique
            // id which will be appended to the SSO URL before
            // redirecting.
            String relayStateID = null;
            if (relayState != null && relayState.length() > 0) {
                relayStateID = getRelayStateID(relayState, authnRequest.getID());
            }

            if (binding.equals(SAML2Constants.HTTP_POST)) {
                String encodedReqMsg = getPostBindingMsg(realm, idpEntityID,idpsso, spsso, spEntityID,
                        authnRequest);
                SAML2Utils.postToTarget(request, response, "SAMLRequest", encodedReqMsg, "RelayState", relayStateID, ssoURL);
            } else {
                String redirect = getRedirect(realm, idpEntityID, authReqXMLString, relayStateID, ssoURL, idpsso, spsso,
                        spEntityID);
                response.sendRedirect(redirect);
            }

            String[] data = { ssoURL };
            LogUtil.access(Level.INFO, LogUtil.REDIRECT_TO_IDP, data, null);
            AuthnRequestInfo reqInfo = new AuthnRequestInfo(authnRequest, relayState);

            synchronized(SPCache.requestHash) {
                SPCache.requestHash.put(authnRequest.getID(),reqInfo);
            }

            if (isFailoverEnabled()) {
                // sessionExpireTime is counted in seconds
                long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
                String key = authnRequest.getID();
                try {
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, new AuthnRequestInfoCopy(reqInfo), sessionExpireTime);
                    if (logger.isDebugEnabled()) {
                        logger.debug("SPSSOFederate.initiateAuthnRequest:"
                                + " SAVE AuthnRequestInfoCopy for requestID " + key);
                    }
                } catch (SAML2TokenRepositoryException e) {
                    logger.error("SPSSOFederate.initiateAuthnRequest: There was a problem saving the " +
                            "AuthnRequestInfoCopy in the SAML2 Token Repository for requestID " + key, e);
                    throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
                }
            }
        } catch (IOException ioe) {
            logger.error("SPSSOFederate: Exception :",ioe);
            throw new SAML2Exception(SAML2Utils.bundle.getString("errorCreatingAuthnRequest"));
        } catch (SAML2MetaException sme) {
            logger.error("SPSSOFederate:Error retrieving metadata", sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    /**
     * Gets the redirect String.
     *
     * @param authReqXMLString Auth Request XML.
     * @param relayStateId the id of the relay state
     * @param ssoURL the url for the reidrect
     * @param idpsso the idp descriptor to use
     * @param spsso the sp descriptor to use
     * @param spEntityId The sp entity ID.
     * @return a String to use for the redirect request.
     * @throws SAML2Exception if there is a problem creating the redirect string
     */
    public static String getRedirect(String realm, String idpEntityId, String authReqXMLString, String relayStateId,
            String ssoURL, IDPSSODescriptorType idpsso, SPSSODescriptorType spsso, String spEntityId)
            throws SAML2Exception {
        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(authReqXMLString);

        StringBuilder queryString = new StringBuilder();
        queryString.append(SAML2Constants.SAML_REQUEST).append(SAML2Constants.EQUAL).append(encodedXML);

        if ((relayStateId != null) && (relayStateId.length() > 0)) {
            queryString.append("&").append(SAML2Constants.RELAY_STATE)
                    .append("=")
                    .append(urlEncodeQueryParameterNameOrValue(relayStateId));
        }

        StringBuilder redirectURL =
                new StringBuilder().append(ssoURL).append(ssoURL.contains("?") ? "&" : "?");
        // sign the query string
        if (idpsso.isWantAuthnRequestsSigned() || spsso.isAuthnRequestsSigned()) {
            String signedQueryStr = signQueryString(queryString.toString(), spEntityId, realm, idpEntityId);
            redirectURL.append(signedQueryStr);
        } else {
            redirectURL.append(queryString);
        }

        return redirectURL.toString();
    }

    /**
     * Gets the SP SSO Descriptor for the given sp entity id in the given realm.
     *
     * @param realm the realm the sp is configured in
     * @param spEntityID the entity id of the sp to get the Descriptor for
     * @return the SPSSODescriptorElement for the requested sp entity
     * @throws SAML2MetaException if there is a problem looking up the SPSSODescriptorElement.
     */
    public static SPSSODescriptorType getSPSSOForAuthnReq(String realm, String spEntityID)
            throws SAML2MetaException {
        return sm.getSPSSODescriptor(realm, spEntityID);
    }

    /**
     * Gets the Configuration attributes for the given sp entity id in the given realm.
     * @param realm the realm the sp is configured in
     * @param spEntityID the entity id of the sp to get the attributes map for
     * @return a map of SAML2 Attributes with String keys mapped to a collection of values
     * @throws SAML2MetaException
     */
    public static Map<String, Collection<String>> getAttrsMapForAuthnReq(String realm, String spEntityID)
            throws SAML2MetaException {

        SPSSOConfigElement spEntityCfg = sm.getSPSSOConfig(realm, spEntityID);
        Map spConfigAttrsMap = null;

        if (spEntityCfg != null) {
            spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
        }

        return spConfigAttrsMap;
    }

    /**
     * Gets the IDP SSO Descriptor for the given sp entity id in the given realm.
     *
     * @param realm the realm the idp is configured in
     * @param idpEntityID the entity id of the idp[ to get the Descriptor for
     * @return the SPSSODescriptorElement for the requested idp entity
     * @throws SAML2MetaException if there is a problem looking up the IDPSSODescriptorElement.
     */
    public static IDPSSODescriptorType getIDPSSOForAuthnReq(String realm, String idpEntityID)
            throws SAML2MetaException {
        return sm.getIDPSSODescriptor(realm, idpEntityID);
    }

    /**
     * Gets the Post Binding message
     *
     * @param realm The realm where the hosted SP is configured.
     * @param idpEntityId The entity ID of the remote IDP.
     * @param idpsso The remote IDP's standard metadata.
     * @param spsso The hosted SP's standard metadata.
     * @param spEntityId The sp entity ID.
     * @param authnRequest The SAML authentication request.
     * @return The optionally signed, serialised, then encoded SAML authentication request.
     * @throws SAML2Exception If there was an error while signing or serialising the authentication request.
     */
    public static String getPostBindingMsg(String realm, String idpEntityId, IDPSSODescriptorType idpsso,
            SPSSODescriptorType spsso, String spEntityId, AuthnRequest authnRequest) throws SAML2Exception {
        if (idpsso.isWantAuthnRequestsSigned() || spsso.isAuthnRequestsSigned()) {
            signAuthnRequest(realm, idpEntityId, spEntityId, authnRequest);
        }
        String authXMLString = authnRequest.toXMLString(true, true);

        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate.initiateAuthnRequest: SAML Response content :\n" + authXMLString);
        }

        return SAML2Utils.encodeForPOST(authXMLString);
    }

    /**
     * Parses the request parameters and builds ECP Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     *
     * @throws SAML2Exception if error creating AuthnRequest.
     * @throws IOException if error sending AuthnRequest to ECP.
     */
    public static void initiateECPRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws SAML2Exception, IOException {

        if (!isFromECP(request)) {
            logger.error("SPSSOFederate.initiateECPRequest: " +
                    "invalid HTTP request from ECP.");
            SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalidHttpRequestFromECP",
                    SAML2Utils.bundle.getString("invalidHttpRequestFromECP"));
            return;
        }

        String metaAlias = request.getParameter("metaAlias");
        Map paramsMap = SAML2Utils.getParamsMap(request);

        // get the sp entity ID from the metaAlias
        String spEntityID = sm.getEntityByMetaAlias(metaAlias);
        String realm = getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate.initiateECPRequest: " +
                    "spEntityID is " + spEntityID + ", realm is " + realm);
        }

        try {
            // Retreive MetaData
            if (sm == null) {
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorMetaManager"));
            }

            SPSSOConfigElement spEntityCfg =
                    sm.getSPSSOConfig(realm,spEntityID);
            Map spConfigAttrsMap=null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }
            // get SPSSODescriptor
            SPSSODescriptorType spsso =
                    sm.getSPSSODescriptor(realm,spEntityID);

            if (spsso == null) {
                String[] data = { spEntityID };
                LogUtil.error(Level.INFO,LogUtil.SP_METADATA_ERROR,data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }

            String[] data = { spEntityID, realm };
            LogUtil.access(Level.INFO, LogUtil.RECEIVED_HTTP_REQUEST_ECP, data,
                    null);

            List extensionsList = getExtensionsList(spEntityID, realm);

            // create AuthnRequest
            AuthnRequest authnRequest = createAuthnRequest(request, response, realm, spEntityID, null,
                    paramsMap, spConfigAttrsMap, extensionsList, spsso, null, null, true);

            Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                    .resolveActiveSigningCredential(realm, spEntityID, Saml2EntityRole.SP);
            Key signingKey = credentials.getSigningKey();

            if (signingKey == null) {
                logger.error("SPSSOFederate.initiateECPRequest: Unable to find signing key.");
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }
            X509Certificate signingCert = credentials.getSigningCertificate();
            authnRequest.sign(SigningConfigFactory.getInstance().createXmlSigningConfig(signingKey, signingCert));

            ECPFactory ecpFactory = ECPFactory.getInstance();

            // Default URL if relayState not present? in providerConfig?
            // TODO get Default URL from metadata
            String relayState = getParameter(paramsMap,
                    SAML2Constants.RELAY_STATE);

            String ecpRelayStateXmlStr = "";
            if (relayState != null && relayState.length()> 0) {
                String relayStateID = getRelayStateID(relayState,
                        authnRequest.getID());
                ECPRelayState ecpRelayState = ecpFactory.createECPRelayState();
                ecpRelayState.setValue(relayStateID);
                ecpRelayState.setMustUnderstand(Boolean.TRUE);
                ecpRelayState.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
                ecpRelayStateXmlStr = ecpRelayState.toXMLString(true, true);
            }

            ECPRequest ecpRequest = ecpFactory.createECPRequest();
            ecpRequest.setIssuer(createIssuer(spEntityID));
            ecpRequest.setMustUnderstand(Boolean.TRUE);
            ecpRequest.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
            ecpRequest.setIsPassive(authnRequest.isPassive());
            SAML2IDPFinder ecpIDPFinder =
                    SAML2Utils.getECPIDPFinder(realm, spEntityID);
            if (ecpIDPFinder != null) {
                List idps = ecpIDPFinder.getPreferredIDP(authnRequest,
                        spEntityID, realm, request, response);
                if ((idps != null) && (!idps.isEmpty())) {
                    SAML2MetaManager saml2MetaManager =
                            SAML2Utils.getSAML2MetaManager();
                    List idpEntries = null;
                    for(Iterator iter = idps.iterator(); iter.hasNext();) {
                        String idpEntityID = (String)iter.next();
                        IDPSSODescriptorType idpDesc = saml2MetaManager
                                .getIDPSSODescriptor(realm, idpEntityID);
                        if (idpDesc != null) {
                            IDPEntry idpEntry = ProtocolFactory.getInstance()
                                    .createIDPEntry();
                            idpEntry.setProviderID(idpEntityID);
                            String description =
                                    SAML2Utils.getAttributeValueFromSSOConfig(
                                            realm, idpEntityID, SAML2Constants.IDP_ROLE,
                                            SAML2Constants.ENTITY_DESCRIPTION);
                            idpEntry.setName(description);
                            List<EndpointType> ssoServiceList = idpDesc.getSingleSignOnService();
                            EndpointType endPoint = getSingleSignOnServiceEndpoint(ssoServiceList, SAML2Constants.SOAP);
                            if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                                throw new SAML2Exception(SAML2Utils.bundle.getString("ssoServiceNotfound"));
                            }
                            String ssoURL = endPoint.getLocation();
                            logger.debug("SPSSOFederate.initiateECPRequest URL : {}", ssoURL);
                            idpEntry.setLoc(ssoURL);
                            if (idpEntries == null) {
                                idpEntries = new ArrayList();
                            }
                            idpEntries.add(idpEntry);
                        }
                    }
                    if (idpEntries != null) {
                        IDPList idpList = ProtocolFactory.getInstance()
                                .createIDPList();
                        idpList.setIDPEntries(idpEntries);
                        ecpRequest.setIDPList(idpList);
                        Map attrs = SAML2MetaUtils.getAttributes(spEntityCfg);
                        List values = (List)attrs.get(
                                SAML2Constants.ECP_REQUEST_IDP_LIST_GET_COMPLETE);
                        if ((values != null) && (!values.isEmpty())) {
                            GetComplete getComplete =
                                    ProtocolFactory.getInstance()
                                            .createGetComplete();
                            getComplete.setValue((String)values.get(0));
                            idpList.setGetComplete(getComplete);
                        }
                    }
                }
            }
            String paosRequestXmlStr = "";
            try {
                PAOSRequest paosRequest = new PAOSRequest(
                        authnRequest.getAssertionConsumerServiceURL(),
                        SAML2Constants.PAOS_ECP_SERVICE, null, Boolean.TRUE,
                        SAML2Constants.SOAP_ACTOR_NEXT);
                paosRequestXmlStr =  paosRequest.toXMLString(true, true);
            } catch (PAOSException paosex) {
                logger.error("SPSSOFederate.initiateECPRequest:",
                        paosex);
                throw new SAML2Exception(paosex.getMessage());
            }
            String header = paosRequestXmlStr +
                    ecpRequest.toXMLString(true, true) + ecpRelayStateXmlStr;

            String body = authnRequest.toXMLString(true, true);
            try {
                SOAPMessage reply = SOAPCommunicator.getInstance().createSOAPMessage(header, body,
                        false);

                String[] data2 = { spEntityID, realm, "" };
                if (LogUtil.isAccessLoggable(Level.FINE)) {
                    data2[2] = SOAPCommunicator.getInstance().soapMessageToString(reply);
                }
                LogUtil.access(Level.INFO, LogUtil.SEND_ECP_PAOS_REQUEST, data2,
                        null);

                // Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }

                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(reply.getMimeHeaders(), response);
                response.setContentType(PAOSConstants.PAOS_MIME_TYPE);
                // Write out the message on the response stream
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                reply.writeTo(stream);
                response.getWriter().println(stream.toString("UTF-8"));
                response.getWriter().flush();
            } catch (SOAPException soapex) {
                logger.error("SPSSOFederate.initiateECPRequest",
                        soapex);
                String[] data3 = { spEntityID, realm };
                LogUtil.error(Level.INFO, LogUtil.SEND_ECP_PAOS_REQUEST_FAILED,
                        data3, null);
                SAMLUtils.sendError(request, response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "soapError", soapex.getMessage());
                return;
            }

            AuthnRequestInfo reqInfo = new AuthnRequestInfo(authnRequest, relayState);
            synchronized(SPCache.requestHash) {
                SPCache.requestHash.put(authnRequest.getID(),reqInfo);
            }
            if (isFailoverEnabled()) {
                // sessionExpireTime is counted in seconds
                long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
                String key = authnRequest.getID();
                try {
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, new AuthnRequestInfoCopy(reqInfo), sessionExpireTime);
                    if (logger.isDebugEnabled()) {
                        logger.debug("SPSSOFederate.initiateECPRequest:"
                                + " SAVE AuthnRequestInfoCopy for requestID " + key);
                    }
                } catch (SAML2TokenRepositoryException e) {
                    logger.error("SPSSOFederate.initiateECPRequest: There was a problem saving the " +
                            "AuthnRequestInfoCopy in the SAML2 Token Repository for requestID " + key, e);
                }
            }
        } catch (SAML2MetaException sme) {
            logger.error("SPSSOFederate:Error retrieving metadata" ,sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    /**
     * Checks if the request is from ECP.
     *
     * @param request the HttpServletRequest.
     * @return true if the request is from ECP.
     */
    public static boolean isFromECP(HttpServletRequest request) {
        PAOSHeader paosHeader = null;
        try {
            paosHeader = new PAOSHeader(request);
        } catch (PAOSException pex) {
            if (logger.isDebugEnabled()) {
                logger.debug("SPSSOFederate.initiateECPRequest:" +
                        "no PAOS header");
            }
            return false;
        }

        Map svcOpts = paosHeader.getServicesAndOptions();
        if ((svcOpts == null) ||
                (!svcOpts.containsKey(SAML2Constants.PAOS_ECP_SERVICE))) {
            if (logger.isDebugEnabled()) {
                logger.debug("SPSSOFederate.initiateECPRequest:" +
                        "PAOS header doesn't contain ECP service");
            }
            return false;
        }

        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null) {
            return false;
        }

        return (acceptHeader.indexOf(PAOSConstants.PAOS_MIME_TYPE) != -1);
    }

    /* Create NameIDPolicy Element */
    private static NameIDPolicy createNameIDPolicy(String spEntityID,
            String format, boolean allowCreate, SPSSODescriptorType spsso,
            IDPSSODescriptorType idpsso, String realm, Map paramsMap)
            throws SAML2Exception {

        format = SAML2Utils.verifyNameIDFormat(format, spsso, idpsso);

        NameIDPolicy nameIDPolicy =
                ProtocolFactory.getInstance().createNameIDPolicy();


        String affiliationID = getParameter(paramsMap,
                SAML2Constants.AFFILIATION_ID);
        if (affiliationID != null) {
            AffiliationDescriptorType affiDesc =
                    sm.getAffiliationDescriptor(realm, affiliationID);
            if (affiDesc == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "affiliationNotFound"));
            }
            if (!affiDesc.getAffiliateMember().contains(spEntityID)) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "spNotAffiliationMember"));
            }
            nameIDPolicy.setSPNameQualifier(affiliationID);
        } else {
            nameIDPolicy.setSPNameQualifier(spEntityID);
        }

        nameIDPolicy.setAllowCreate(allowCreate);
        nameIDPolicy.setFormat(format);
        return nameIDPolicy;
    }

    /* Create Issuer */
    private static Issuer createIssuer(String spEntityID)
            throws SAML2Exception {
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(spEntityID);
        return issuer;
    }

    /**
     * Create an AuthnRequest.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param realmName the authentication realm for this request
     * @param spEntityID the entity id for the service provider
     * @param idpEntityID entityID of Identity Provider.
     * @param paramsMap the map of parameters for the authentication request
     * @param spConfigMap the configuration map for the service provider
     * @param extensionsList a list of extendsions for the authentication request
     * @param spsso the SPSSODescriptorElement for theservcie provider
     * @param idpsso the IDPSSODescriptorElement for the identity provider
     * @param ssourl the url for the single sign on request
     * @param isForECP boolean to indicatge if the request originated from an ECP
     * @return a new AuthnRequest object
     * @throws SAML2Exception
     */
    public static AuthnRequest createAuthnRequest(final HttpServletRequest request,
            final HttpServletResponse response,
            final String realmName,
            final String spEntityID,
            final String idpEntityID,
            final Map paramsMap,
            final Map spConfigMap,
            final List extensionsList,
            final SPSSODescriptorType spsso,
            final IDPSSODescriptorType idpsso,
            final String ssourl,
            final boolean isForECP) throws SAML2Exception {
        // generate unique request ID
        String requestID = SAML2Utils.generateID();
        if ((requestID == null) || (requestID.length() == 0)) {
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("cannotGenerateID"));
        }

        // retrieve data from the params map and if not found get
        // default values from the SPConfig Attributes
        // destinationURI required if message is signed.
        String destinationURI= getParameter(paramsMap,
                SAML2Constants.DESTINATION);
        Boolean isPassive = doPassive(paramsMap, spConfigMap);
        Boolean isforceAuthn = isForceAuthN(paramsMap, spConfigMap);
        boolean allowCreate = isAllowCreate(paramsMap, spConfigMap);
        boolean includeRequestedAuthnContextFlag = includeRequestedAuthnContext(paramsMap, spConfigMap);

        String consent = getParameter(paramsMap,SAML2Constants.CONSENT);
        Extensions extensions = createExtensions(extensionsList);
        String nameIDPolicyFormat = getParameter(paramsMap,
                SAML2Constants.NAMEID_POLICY_FORMAT);
        // get NameIDPolicy Element
        NameIDPolicy nameIDPolicy = createNameIDPolicy(spEntityID,
                nameIDPolicyFormat, allowCreate, spsso, idpsso, realmName,
                paramsMap);
        Issuer issuer = createIssuer(spEntityID);
        Integer acsIndex = getIndex(paramsMap,SAML2Constants.ACS_URL_INDEX);
        Integer attrIndex = getIndex(paramsMap,SAML2Constants.ATTR_INDEX);

        String protocolBinding = isForECP ? SAML2Constants.PAOS :
                getParameter(paramsMap, "binding");
        OrderedSet acsSet = getACSUrl(spsso,protocolBinding);
        String acsURL = (String) acsSet.get(0);
        protocolBinding = (String)acsSet.get(1);
        if (!SAML2Utils.isSPProfileBindingSupported(
                realmName, spEntityID, SAML2Constants.ACS_SERVICE,
                protocolBinding))
        {
            logger.error("SPSSOFederate.createAuthnRequest:" +
                    protocolBinding +
                    "is not supported for " + spEntityID);
            String[] data = { spEntityID, protocolBinding };
            LogUtil.error(
                    Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        AuthnRequest authnReq =
                ProtocolFactory.getInstance().createAuthnRequest();
        if (!isForECP) {
            if ((destinationURI == null) || (destinationURI.length() == 0)) {
                authnReq.setDestination(XMLUtils.escapeSpecialCharacters(
                        ssourl));
            } else {
                authnReq.setDestination(XMLUtils.escapeSpecialCharacters(
                        destinationURI));
            }
        }
        authnReq.setConsent(consent);
        authnReq.setIsPassive(isPassive);
        authnReq.setForceAuthn(isforceAuthn);
        authnReq.setAttributeConsumingServiceIndex(attrIndex);
        authnReq.setAssertionConsumerServiceIndex(acsIndex);
        authnReq.setAssertionConsumerServiceURL(
                XMLUtils.escapeSpecialCharacters(acsURL));
        authnReq.setProtocolBinding(protocolBinding);
        authnReq.setIssuer(issuer);
        authnReq.setNameIDPolicy(nameIDPolicy);
        if (includeRequestedAuthnContextFlag) {
            authnReq.setRequestedAuthnContext(createReqAuthnContext(realmName, spEntityID, paramsMap, spConfigMap));
        }
        if (extensions != null) {
            authnReq.setExtensions(extensions);
        }

        // Required attributes in authn request
        authnReq.setID(requestID);
        authnReq.setVersion(SAML2Constants.VERSION_2_0);
        authnReq.setIssueInstant(newDate());
        //IDP Proxy 
        Boolean enableIDPProxy =
                getAttrValueFromMap(spConfigMap,
                        SAML2Constants.ENABLE_IDP_PROXY);
        if ((enableIDPProxy != null) && enableIDPProxy.booleanValue())
        {
            Scoping scoping =
                    ProtocolFactory.getInstance().createScoping();
            String proxyCountParam = getParameter(spConfigMap,
                    SAML2Constants.IDP_PROXY_COUNT);
            if (proxyCountParam != null && (!proxyCountParam.equals(""))) {
                scoping.setProxyCount(new Integer(proxyCountParam));
            }
            List proxyIDPs = (List) spConfigMap.get(
                    SAML2Constants.IDP_PROXY_LIST);
            if (proxyIDPs != null && !proxyIDPs.isEmpty()) {
                Iterator iter = proxyIDPs.iterator();
                ArrayList list = new ArrayList();
                while(iter.hasNext()) {
                    IDPEntry entry = ProtocolFactory.getInstance().
                            createIDPEntry();
                    entry.setProviderID((String)iter.next());
                    list.add(entry);
                }
                IDPList idpList = ProtocolFactory.getInstance().
                        createIDPList();
                idpList.setIDPEntries(list);
                scoping.setIDPList(idpList);
            }
            authnReq.setScoping(scoping);
        }
        // invoke SP Adapter class if registered
        SAML2ServiceProviderAdapter spAdapter = SAML2Utils.getSPAdapterClass(spEntityID, realmName);
        if (spAdapter != null) {
            spAdapter.preSingleSignOnRequest(spEntityID, idpEntityID, realmName, request, response, authnReq);
        }

        return authnReq;
    }

    /**
     * Returns value of an boolean parameter in the SP SSO Config.
     * @param attrMap the map of attributes for the sso config
     * @param attrName the key to get the boolean value for
     * @return the value of the parameter in the sso config or null if the attribute was not found or was
     * not a boolean parameter
     */
    public static Boolean getAttrValueFromMap(final Map attrMap, final String attrName) {
        Boolean boolVal = null;
        if (attrMap!=null && attrMap.size()> 0) {
            String attrVal = getParameter(attrMap,attrName);
            if ((attrVal != null)
                    && ( (attrVal.equals(SAML2Constants.TRUE))
                    || (attrVal.equals(SAML2Constants.FALSE)))) {
                boolVal = new Boolean(attrVal);
            }
        }
        return boolVal;
    }

    /**
     * Returns the SingleSignOnService service. If no binding is specified
     * it will return the first endpoint in the list matching either HTTP-Redirect or HTTP-Post.
     * If the binding is specified it will attempt to return a match.
     * If either of the above is not found it will return null.
     *
     * @param ssoServiceList list of sso services
     * @param binding        binding of the sso service to get the url for
     * @return a SingleSignOnServiceElement or null if no match found.
     */
    public static EndpointType getSingleSignOnServiceEndpoint(
            List<EndpointType> ssoServiceList, String binding) {
        EndpointType preferredEndpoint = null;
        boolean noPreferredBinding = StringUtils.isEmpty(binding);
        for (EndpointType endpoint : ssoServiceList) {
            if (noPreferredBinding && (SAML2Constants.HTTP_REDIRECT.equals(endpoint.getBinding())
                    || SAML2Constants.HTTP_POST.equals(endpoint.getBinding()))) {
                preferredEndpoint = endpoint;
                break;
            } else if (binding.equals(endpoint.getBinding())) {
                preferredEndpoint = endpoint;
                break;
            }
        }
        return preferredEndpoint;
    }

    /**
     * Returns an Ordered Set containing the AssertionConsumerServiceURL
     * and AssertionConsumerServiceIndex.
     */
    static OrderedSet getACSUrl(SPSSODescriptorType spsso,
            String binding) {
        String responseBinding = binding;
        if ((binding != null) && (binding.length() > 0) &&
                (!binding.contains(SAML2Constants.BINDING_PREFIX))) {
            responseBinding =
                    new StringBuffer().append(SAML2Constants.BINDING_PREFIX)
                            .append(binding).toString();
        }
        List<IndexedEndpointType> acsList = spsso.getAssertionConsumerService();
        String acsURL=null;
        if (acsList != null && !acsList.isEmpty()) {
            Iterator<IndexedEndpointType> ac = acsList.iterator();
            while (ac.hasNext()) {
                IndexedEndpointType ace = ac.next();

                if ((ace != null && ace.isIsDefault()) &&
                        (responseBinding == null || responseBinding.length() ==0 )) {
                    acsURL = ace.getLocation();
                    responseBinding = ace.getBinding();
                    break;
                } else if ((ace != null) &&
                        (ace.getBinding().equals(responseBinding))) {
                    acsURL = ace.getLocation();
                    break;
                }
            }
        }
        OrderedSet ol = new OrderedSet();
        ol.add(acsURL);
        ol.add(responseBinding);
        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate: AssertionConsumerService :"
                    + " URL :" + acsURL);
            logger.debug("SPSSOFederate: AssertionConsumerService :"
                    + " Binding Passed in Query: " + binding);
            logger.debug("SPSSOFederate: AssertionConsumerService :"
                    + " Binding : " + responseBinding);
        }
        return ol;
    }

    /**
     * Fills in the realm with the default top level realm if it does not contain a more specific subrealm.
     * i.e. if it is null or empty it becomes "/"
     * @param realm the current realm
     * @return the realm to use
     */
    public static String getRealm(final String realm) {
        return ((realm == null) || (realm.length() == 0)) ? "/" : realm;
    }

    /**
     * Gets isPassive attribute from the config map and parameters map.
     *
     * @param paramsMap the map of the parameters
     * @param spConfigAttrsMap the map of the configuration
     * @return boolean to indicate if the request should be passive
     */
    private static Boolean doPassive(Map paramsMap,Map spConfigAttrsMap){
        // get isPassive
        Boolean isPassive=Boolean.FALSE;
        String isPassiveStr =
                getParameter(paramsMap,SAML2Constants.ISPASSIVE);

        if ((isPassiveStr != null) &&
                ((isPassiveStr.equals(SAML2Constants.TRUE) ||
                        (isPassiveStr.equals(SAML2Constants.FALSE))))) {
            isPassive = new Boolean(isPassiveStr);
        } else {
            isPassive = getAttrValueFromMap(spConfigAttrsMap,
                    SAML2Constants.ISPASSIVE);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate: isPassive : " + isPassive);
        }
        return (isPassive == null) ? Boolean.FALSE : isPassive;
    }

    /* Returns value of ForceAuthn */
    private static Boolean isForceAuthN(Map paramsMap,Map spConfigAttrsMap) {
        Boolean isforceAuthn;
        String forceAuthn = getParameter(paramsMap,SAML2Constants.FORCEAUTHN);
        if ((forceAuthn != null) &&
                ((forceAuthn.equals(SAML2Constants.TRUE) ||
                        (forceAuthn.equals(SAML2Constants.FALSE))))) {
            isforceAuthn = new Boolean(forceAuthn);
        } else {
            isforceAuthn = getAttrValueFromMap(spConfigAttrsMap,
                    SAML2Constants.FORCEAUTHN);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate:ForceAuthn: " + forceAuthn);
        }
        return (isforceAuthn == null) ? Boolean.FALSE : isforceAuthn;
    }

    /* get value of AllowCreate */
    private static boolean isAllowCreate(Map paramsMap,Map spConfigAttrsMap) {
        //assuming default true? 
        boolean allowCreate=true;
        String allowCreateStr=getParameter(paramsMap,
                SAML2Constants.ALLOWCREATE);
        if ((allowCreateStr != null) &&
                ((allowCreateStr.equals(SAML2Constants.TRUE) ||
                        (allowCreateStr.equals(SAML2Constants.FALSE))))
        ) {
            allowCreate = new Boolean(allowCreateStr).booleanValue();
        } else {
            Boolean val = getAttrValueFromMap(spConfigAttrsMap,
                    SAML2Constants.ALLOWCREATE);
            if (val != null) {
                allowCreate = val.booleanValue();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate:AllowCreate:"+ allowCreate);
        }
        return allowCreate;
    }

    private static boolean includeRequestedAuthnContext(Map paramsMap, Map spConfigAttrsMap) {

        // Default to true if this flag is not found to be backwards compatible.
        boolean result = true;

        // Check the parameters first in case the request wants to override the metadata value.
        Boolean val = getAttrValueFromMap(paramsMap, SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT);
        if (val != null) {
            result = val;
        } else {
            val = getAttrValueFromMap(spConfigAttrsMap, SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT);
            if (val != null) {
                result = val;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("SPSSOFederate:includeRequestedAuthnContext:" + result);
        }

        return result;
    }

    /* Returns the AssertionConsumerServiceURL Index */
    private static Integer getIndex(Map paramsMap,String attrName) {
        Integer attrIndex = null;
        String index = getParameter(paramsMap,attrName);
        if ((index != null) && (index.length() > 0)) {
            attrIndex = new Integer(index);
        }
        return attrIndex;
    }

    /**
     * Gets the query parameter value for the param specified.
     * @param paramsMap the map of parameters
     * @param attrName the parameter name to get the value for
     * @return the string value for the given parameter
     */
    public static String getParameter(Map paramsMap,String attrName) {
        String attrVal = null;
        if ((paramsMap != null) && (!paramsMap.isEmpty())) {
            List attrValList = (List)paramsMap.get(attrName);
            if (attrValList != null && !attrValList.isEmpty()) {
                attrVal = (String) attrValList.iterator().next();
            }
        }
        return attrVal;
    }

    /**
     * Gets the extensions list for the sp entity.
     *
     * @param entityID the entity of the id for get the extensions list for
     * @param realm the realm that the entity is configured in
     * @return a List ofd the extensions for the sso request
     */
    public static List getExtensionsList(String entityID,String realm) {
        List extensionsList = null;
        try {
            EntityDescriptorElement ed = sm.getEntityDescriptor(realm,entityID);
            if (ed != null) {
                com.sun.identity.saml2.jaxb.metadata.ExtensionsType ext =
                        ed.getValue().getExtensions();
                if (ext != null) {
                    extensionsList = ext.getAny();
                }
            }
        } catch (SAML2Exception e) {
            logger.error("SPSSOFederate:Error retrieving " +
                    "EntityDescriptor");
        }
        return extensionsList;
    }

    private static com.sun.identity.saml2.protocol.Extensions
    createExtensions(List extensionsList) throws SAML2Exception {
        com.sun.identity.saml2.protocol.Extensions extensions=null;
        if (extensionsList != null && !extensionsList.isEmpty()) {
            extensions =
                    ProtocolFactory.getInstance().createExtensions();
            extensions.setAny(extensionsList);
        }
        return extensions;
    }


    /**
     * Gets the Relay State ID for the request.
     *
     * @param relayState the relay state
     * @param requestID the request id
     * @return the relay state id
     */
    public static String getRelayStateID(String relayState, String requestID) {

        SPCache.relayStateHash.put(requestID, new CacheObject(relayState));

        if (isFailoverEnabled()) {
            // sessionExpireTime is counted in seconds
            long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
            // Need to make the key unique due to the requestID also being used to
            // store a copy of the AuthnRequestInfo
            String key = requestID + requestID;
            try {
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, relayState, sessionExpireTime);
                if (logger.isDebugEnabled()) {
                    logger.debug("SPSSOFederate.getRelayStateID: SAVE relayState for requestID "
                            + key);
                }
            } catch (SAML2TokenRepositoryException se) {
                logger.error("SPSSOFederate.getRelayStateID: Unable to SAVE relayState for requestID "
                        + key, se);
            }
        }

        return requestID;
    }

    /* Creates RequestedAuthnContext Object */
    private static RequestedAuthnContext createReqAuthnContext(String realmName,
            String spEntityID,Map paramsMap,
            Map spConfigMap) {
        RequestedAuthnContext reqCtx = null;
        String className = null;
        if ((spConfigMap != null) && (!spConfigMap.isEmpty())) {
            List listVal =
                    (List) spConfigMap.get(
                            SAML2Constants.SP_AUTHCONTEXT_MAPPER);
            if (listVal != null && listVal.size() != 0) {
                className = ((String) listVal.iterator().next()).trim();
            }
        }

        SPAuthnContextMapper spAuthnContextMapper =
                SAML2Utils.getSPAuthnContextMapper(realmName,spEntityID,className);

        try {
            reqCtx =
                    spAuthnContextMapper.getRequestedAuthnContext(
                            realmName,spEntityID,paramsMap);

        } catch (SAML2Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("SPSSOFederate:Error creating " +
                        "RequestedAuthnContext",e);
            }
        }

        return reqCtx;
    }

    /**
     * Signs the query string.
     *
     * @param queryString the query string
     * @param spEntityId The sp entity ID.
     * @return the signed query string
     * @throws SAML2Exception
     */
    static String signQueryString(String queryString, String spEntityId, String realm, String idpEntityId)
            throws SAML2Exception {
        logger.debug("SPSSOFederate:queryString: {}, spEntityId: {}", queryString, spEntityId);
        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, spEntityId, Saml2EntityRole.SP);
        Key signingKey = credentials.getSigningKey();
        return QuerySignatureUtil.sign(queryString, SigningConfigFactory.getInstance()
                .createQuerySigningConfig(signingKey,
                        SAML2Utils.getSAML2MetaManager().getEntityDescriptor(realm, idpEntityId),
                        Saml2EntityRole.IDP));
    }

    /**
     * Sign an authentication request.
     *
     * @param realm The realm where the hosted SP is configured.
     * @param idpEntityId The entity ID of the remote IDP.
     * @param spEntityId The sp entity ID.
     * @param authnRequest the authentication request to sign
     * @throws SAML2Exception the signed authentication request
     */
    static void signAuthnRequest(String realm, String idpEntityId, String spEntityId, AuthnRequest authnRequest)
            throws SAML2Exception {
        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, spEntityId, Saml2EntityRole.SP);
        Key signingKey = credentials.getSigningKey();
        X509Certificate signingCert = credentials.getSigningCertificate();
        authnRequest.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey, signingCert,
                        SAML2Utils.getSAML2MetaManager().getEntityDescriptor(realm, idpEntityId), Saml2EntityRole.IDP));
    }
}
