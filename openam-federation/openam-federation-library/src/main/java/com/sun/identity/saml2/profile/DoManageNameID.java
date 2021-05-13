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
 * $Id: DoManageNameID.java,v 1.26 2009/11/24 21:53:27 madan_ranganath Exp $
 *
 * Portions copyright 2013-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnectionFactory;
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

import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAccountMapper;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.protocol.NewID;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class reads the query parameters and the required
 * processing logic for sending ManageNameIDRequest
 * from SP to IDP.
 */

public class DoManageNameID {

    private static final Logger logger = LoggerFactory.getLogger(DoManageNameID.class);
    final static String className = "DoManageNameID:";
    static ProtocolFactory pf = ProtocolFactory.getInstance();
    static SOAPConnectionFactory scf = null;
    static MessageFactory mf = null;
    static SAML2MetaManager metaManager = null;
    static SessionProvider sessionProvider = null;
    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
    
    static {
        try {
            scf = SOAPConnectionFactory.newInstance();
            mf = MessageFactory.newInstance();
            metaManager= new SAML2MetaManager();
            sessionProvider = SessionManager.getProvider();
        } catch (SOAPException se) {
            logger.error(SAML2Utils.bundle.getString("errorSOAPFactory"), se);
        } catch (SAML2MetaException se) {
            logger.error(SAML2Utils.bundle.getString("errorMetaManager"), se);
        } catch (SessionException sessE) {
            logger.error("Error retrieving session provider.", sessE);
        }
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }
    
    private static void logError(String msgID, String key, String value) {
        logger.error(SAML2Utils.bundle.getString(msgID));
        String[] data = {value};
        LogUtil.error(Level.INFO, key, data, null);
    }

    private static void logAccess(String msgID, String key, String value) {
        logger.debug(SAML2Utils.bundle.getString(msgID));
        String[] data = {value};
        LogUtil.access(Level.INFO, key, data, null);
    }
    
    /**
     * Parses the request parameters and builds the ManageNameID
     * Request to sent to remote Entity.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param metaAlias entityID of hosted entity.
     * @param remoteEntityID entityID of remote entity.
     * @param paramsMap Map of all other parameters.
     * @throws SAML2Exception if error initiating request to remote entity.
     */
    public static void initiateManageNameIDRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        String metaAlias,
        String remoteEntityID,
        Map paramsMap) throws SAML2Exception {
            
        String method = "DoManageNameID.initiateManageNameIDRequest: ";

        if (metaManager == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("errorMetaManager"));
        }
        
        if (metaAlias == null) {
            logError("MetaAliasNotFound", 
                            LogUtil.MISSING_META_ALIAS, metaAlias);
            throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullEntityID"));
        }
                
        if (remoteEntityID == null)  {
            logError("nullRemoteEntityID", 
                            LogUtil.MISSING_ENTITY, remoteEntityID);
            throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullRemoteEntityID"));
        }

        Object session = null;
        try {
            session = SessionManager.getProvider().getSession(request);
        } catch (SessionException se) {
            if (logger.isDebugEnabled()) {
                logger.debug(method, se);
            }
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
        String hostEntityRole = SAML2Utils.getHostEntityRole(paramsMap);
        if (session == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(method + "Session is missing." +
                            "redirect to the authentication service");
            }
            // the user has not logged in yet, 
            // redirect to the authentication service
            try {       
                SAML2Utils.redirectAuthentication(request, response, 
                                realm, hostEntityID, hostEntityRole);
            } catch (IOException ioe) {
                logError("UnableToRedirectToAuth", 
                                LogUtil.REDIRECT_TO_AUTH, null);
                throw new SAML2Exception(ioe.toString());
            }
            return;
        } 
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "Meta Alias is : "+ metaAlias);
            logger.debug(method + "Remote EntityID is : " + remoteEntityID);
            logger.debug(method + "Host EntityID is : " + hostEntityID);
        }
        
        try {
            String binding = 
                SAML2Utils.getParameter(paramsMap, SAML2Constants.BINDING);

            EndpointType mniService =
                getMNIServiceElement(realm, remoteEntityID, 
                hostEntityRole, binding);
            if (binding == null) {
                binding = mniService.getBinding();
            }

            if (binding == null) {
                logError("UnableTofindBinding", LogUtil.METADATA_ERROR, null);
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("UnableTofindBinding"));
                }

            String mniURL = null;
            if (mniService != null) {
                mniURL = mniService.getLocation();
            }
            
            if (mniURL == null) {
                logError("mniServiceNotFound", LogUtil.METADATA_ERROR, null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("mniServiceNotFound"));
            }

            String requestType = (String)paramsMap.get("requestType");
            boolean changeID = "NewID".equals(requestType);

            String affiliationID = SAML2Utils.getParameter(paramsMap,
                SAML2Constants.AFFILIATION_ID); 
            ManageNameIDRequest mniRequest = createManageNameIDRequest(
                session, realm, hostEntityID, hostEntityRole, remoteEntityID,
                mniURL, changeID, affiliationID);

            String relayState = SAML2Utils.getParameter(paramsMap,
                             SAML2Constants.RELAY_STATE);
            if ((relayState == null) || (relayState.equals(""))) {
                relayState = SAML2Utils.getAttributeValueFromSSOConfig(
                    realm, hostEntityID, hostEntityRole,
                    SAML2Constants.DEFAULT_RELAY_STATE);
            }

            String requestUrl = request.getRequestURL().toString();

            // Validate the RelayState URL.
            SAML2Utils.validateRelayStateURL(realm,
                                             hostEntityID,
                                             relayState,
                                             hostEntityRole,
                                             requestUrl);

            mniRequest.setDestination(XMLUtils.escapeSpecialCharacters(mniURL));
            saveMNIRequestInfo(request, response, paramsMap, mniRequest,
                relayState, hostEntityRole, session);

            String mniRequestXMLString = null;

            if (binding.equalsIgnoreCase(SAML2Constants.HTTP_REDIRECT)) {
                mniRequestXMLString = mniRequest.toXMLString(true,true);
                doMNIByHttpRedirect(mniRequestXMLString, mniURL, relayState,
                    realm, hostEntityID, hostEntityRole, remoteEntityID,
                    response);
            } else if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
                signMNIRequest(mniRequest, realm, hostEntityID, hostEntityRole,
                    remoteEntityID);

                JAXBElement<BaseConfigType> config = null;
                if (hostEntityRole.equalsIgnoreCase(SP_ROLE)) {
                    config = metaManager.getIDPSSOConfig(realm, remoteEntityID);
                } else {
                    config = metaManager.getSPSSOConfig(realm, remoteEntityID);
                }
                mniURL = SAML2Utils.fillInBasicAuthInfo(config, mniURL);
                if (!doMNIBySOAP(mniRequest, mniURL, metaAlias, hostEntityRole,
                    request, response)) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("mniFailed"));
                }
            } else if (binding.equalsIgnoreCase(SAML2Constants.HTTP_POST)) {
                signMNIRequest(mniRequest, realm, hostEntityID, hostEntityRole,
                    remoteEntityID);
                mniRequestXMLString= mniRequest.toXMLString(true,true);
                doMNIByPOST(mniRequestXMLString, mniURL, relayState, realm,
                    hostEntityID, hostEntityRole, remoteEntityID, response, request);
            }
        } catch (IOException ioe) {
            logError("errorCreatingMNIRequest", 
                            LogUtil.CANNOT_INSTANTIATE_MNI_REQUEST, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorCreatingMNIRequest"));
        } catch (SAML2MetaException sme) {
            logError("metaDataError", LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));            
        } catch (SessionException ssoe) {
             logError("invalidSSOToken", LogUtil.INVALID_SSOTOKEN, null);
             throw new SAML2Exception(
                     SAML2Utils.bundle.getString("invalidSSOToken"));
        }
    }

    private static void postTerminationSuccess(String hostEntityId,
        String realm, HttpServletRequest request, HttpServletResponse response,
        String userId,  ManageNameIDRequest idRequest, 
        ManageNameIDResponse idResponse, String binding) {

        SAML2ServiceProviderAdapter spAdapter = null;
        try {
            spAdapter = SAML2Utils.getSPAdapterClass(hostEntityId, realm);
        } catch (SAML2Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("DoManageNameID.postTerminationSuccess:", e);
            }
        }
        if (spAdapter != null) {
            spAdapter.postTerminateNameIDSuccess(hostEntityId, realm,
                request, response, userId, idRequest, idResponse, binding);
        }
    }

    /**
     * Returns binding information of MNI Service for remote entity 
     * from request or meta configuration.
     *
     * @param request the HttpServletRequest.
     * @param metaAlias entityID of hosted entity.
     * @param hostEntityRole Role of hosted entity.
     * @param remoteEntityID entityID of remote entity.
     * @return return true if the processing is successful.
     * @throws SAML2Exception if no binding information is configured.
     */
    public static String getMNIBindingInfo(HttpServletRequest request,
                                 String metaAlias,
                                 String hostEntityRole,
                                 String remoteEntityID)
                                 throws SAML2Exception {
        String binding = request.getParameter(SAML2Constants.BINDING);

        try {
            if (binding == null) {
                String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
                EndpointType mniService =
                    getMNIServiceElement(realm, remoteEntityID,
                                       hostEntityRole, null);
                if (mniService != null) {
                    binding = mniService.getBinding();
                }
            }
        } catch (SessionException e) {
            logError("invalidSSOToken", LogUtil.INVALID_SSOTOKEN, null);
            throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSSOToken"));       
        }
        
        if (binding == null) {
            logError("UnableTofindBinding", 
                             LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("UnableTofindBinding"));
        }
        return binding;
    }
    
    private static void signMNIRequest(ManageNameIDRequest mniRequest, 
                                   String realm, String hostEntity, 
                                   String hostEntityRole, String remoteEntity) 
        throws SAML2Exception {
        signMNIRequest(mniRequest, realm, hostEntity, 
                       hostEntityRole, remoteEntity, false);
    }
    
    private static void signMNIRequest(ManageNameIDRequest mniRequest, 
                                  String realm, String hostEntity,
                                  String hostEntityRole, String remoteEntityId,
                                  boolean includeCert) 
        throws SAML2Exception {
        String method = "signMNIRequest : ";
        boolean needRequestSign = false;
        
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needRequestSign = SAML2Utils.getWantMNIRequestSigned(realm, remoteEntityId, SP_ROLE);
        } else {
            needRequestSign = SAML2Utils.getWantMNIRequestSigned(realm, remoteEntityId, IDP_ROLE);
        }
        
        if (!needRequestSign) {
            if (logger.isDebugEnabled()) {
                logger.debug(method + "MNIRequest doesn't need to be signed.");
            }
            return;
        }
        
        Saml2SigningCredentials signingDetails = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, hostEntity, Saml2EntityRole.fromString(hostEntityRole));
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
            logger.debug(method + "remoteEntityId is : " + remoteEntityId);
            logger.debug(method + "MNI Request before sign : "
                            + mniRequest.toXMLString(true, true));
        }

        Key signingKey = signingDetails.getSigningKey();

        if (signingKey == null) {
            logError("secretNotConfigured", LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("secretNotConfigured"));
        }

        X509Certificate signingCert = includeCert ? signingDetails.getSigningCertificate() : null;
        mniRequest.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, remoteEntityId),
                        IDP_ROLE.equals(hostEntityRole) ? SP : IDP));
        if (logger.isDebugEnabled()) {
            logger.debug(method + "MNI Request after sign : "
                                + mniRequest.toXMLString(true, true));
        }
    }

    private static boolean verifyMNIRequest(ManageNameIDRequest mniRequest, 
        String realm, String remoteEntity, String hostEntity,
        String hostEntityRole, String destination) throws SAML2Exception {

        String method = "verifyMNIRequest : ";
        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "remoteEntity is : " + remoteEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
        }
        
        boolean needVerifySignature = 
            SAML2Utils.getWantMNIRequestSigned(realm, hostEntity, 
            hostEntityRole);
        
        if (!needVerifySignature) {
            if (logger.isDebugEnabled()) {
                logger.debug(method+"MNIRequest doesn't need to be verified.");
            }
            return true;
        }
                
        boolean valid;
        Set<X509Certificate> signingCerts;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc = metaManager.getSPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(spSSODesc, remoteEntity, SP_ROLE, realm);
        } else {
            IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, remoteEntity, IDP_ROLE, realm);
        }

        if (!signingCerts.isEmpty()) {
            valid = mniRequest.isSignatureValid(signingCerts);
                if (logger.isDebugEnabled()) {
                logger.debug(method + "Signature is : " + valid);
            }
        } else {
            logError("missingSigningCertAlias.", 
                             LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
        
        return valid;
    }
    
    private static void signMNIResponse(ManageNameIDResponse mniResponse, 
                                         String realm, String hostEntity, 
                               String hostEntityRole, String remoteEntity) 
        throws SAML2Exception {
        signMNIResponse(mniResponse, realm, hostEntity, 
                        hostEntityRole, remoteEntity, false); 
    }
    
    private static void signMNIResponse(ManageNameIDResponse mniResponse, 
                                         String realm, String hostEntity, 
                               String hostEntityRole, String remoteEntityId,
                                                      boolean includeCert) 
        throws SAML2Exception {
        String method = "signMNIResponse : ";
        boolean needResponseSign = false;
        
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needResponseSign = SAML2Utils.getWantMNIResponseSigned(realm, remoteEntityId, SP_ROLE);
        } else {
            needResponseSign = SAML2Utils.getWantMNIResponseSigned(realm, remoteEntityId, IDP_ROLE);
        }
        
        if (!needResponseSign) {
            if (logger.isDebugEnabled()) {
                logger.debug(method+"MNIResponse doesn't need to be signed.");
            }
            return;
        }
        

        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, hostEntity, Saml2EntityRole.fromString(hostEntityRole));
        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "hostEntity is : " + hostEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
            logger.debug(method + "MNI Response before sign : "
                            + mniResponse.toXMLString(true, true));
        }
        Key signingKey = credentials.getSigningKey();

        if (signingKey == null) {
            logError("secretNotConfigured", LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("secretNotConfigured"));
        }

        X509Certificate signingCert = includeCert ? credentials.getSigningCertificate() : null;
        mniResponse.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, remoteEntityId),
                        IDP_ROLE.equals(hostEntityRole) ? SP : IDP));
        if (logger.isDebugEnabled()) {
                logger.debug(method + "MNI Response after sign : "
                                    + mniResponse.toXMLString(true, true));
        }
    }

    private static boolean verifyMNIResponse(ManageNameIDResponse mniResponse, 
        String realm, String remoteEntity, 
        String hostEntity, String hostEntityRole,
        String destination) 
        throws SAML2Exception, SessionException {
        String method = "verifyMNIResponse : ";
        if (logger.isDebugEnabled()) {
            logger.debug(method + "realm is : "+ realm);
            logger.debug(method + "remoteEntity is : " + remoteEntity);
            logger.debug(method + "Host Entity role is : " + hostEntityRole);
        }
        
        boolean needVerifySignature = 
                SAML2Utils.getWantMNIResponseSigned(realm, hostEntity, 
                                hostEntityRole);
        
        if (!needVerifySignature) {
            if (logger.isDebugEnabled()) {
                logger.debug(method +
                        "MNIResponse doesn't need to be verified.");
            }
            return true;
        }
                
        boolean valid;
        Set<X509Certificate> signingCerts;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc = metaManager.getSPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(spSSODesc, remoteEntity, SP_ROLE, realm);
        } else {
            IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, remoteEntity, IDP_ROLE, realm);
        }
        
        if (!signingCerts.isEmpty()) {
            valid = mniResponse.isSignatureValid(signingCerts);
                if (logger.isDebugEnabled()) {
                logger.debug(method + "Signature is : " + valid);
                }
        } else {
            logError("missingSigningCertAlias", 
                             LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
        
        return valid;
    }
    
    private static void saveMNIRequestInfo(HttpServletRequest request, 
        HttpServletResponse response, Map paramsMap,
        ManageNameIDRequest mniRequest, String relayState,
        String hostEntityRole, Object session) throws SAML2Exception {

        String method = "saveMNIRequestInfo: ";
        if (logger.isDebugEnabled()) {
            logger.debug(method + "hostEntityRole : " + hostEntityRole);
        }
        
        ManageNameIDRequest reqForSave = mniRequest;
        NameID nameID = mniRequest.getNameID();

        EncryptedID encryptedID = mniRequest.getEncryptedID();

        if (encryptedID != null) {
            NewEncryptedID newEncryptedID = mniRequest.getNewEncryptedID();
            mniRequest.setEncryptedID(null);
            mniRequest.setNewEncryptedID(null);
            reqForSave = (ManageNameIDRequest)pf.createManageNameIDRequest(
                mniRequest.toXMLString(true, true));

            mniRequest.setNameID(null);
            mniRequest.setNewID(null);
            mniRequest.setEncryptedID(encryptedID);
            mniRequest.setNewEncryptedID(newEncryptedID);
        }
        
        paramsMap.put(SAML2Constants.SESSION, session);
        ManageNameIDRequestInfo reqInfo = new ManageNameIDRequestInfo(request,
            response, reqForSave, relayState, paramsMap, session);

        reqInfo.setNameID(nameID);
        
        if (hostEntityRole.equalsIgnoreCase(SP_ROLE)) {
            SPCache.mniRequestHash.put(mniRequest.getID(), reqInfo);
        } else {
            IDPCache.mniRequestHash.put(mniRequest.getID(), reqInfo);
        }
    }
    
    /**
     * Parses the request parameters and process the ManageNameID
     * Request from the remote entity.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param paramsMap Map of all other parameters.
     * @throws SAML2Exception if error occurred while processing the request.
     * @throws SessionException if error processing the request from remote entity.
     * @throws ServletException if request length is invalid.
     */
    public static void processHttpRequest(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Map paramsMap) 
        throws SAML2Exception, SessionException, ServletException {
        String method = "processHttpRequest: ";
        String metaAlias = null;
        String remoteEntityID = null;
        String queryString = null;
        
        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(request);
        String requestURL = request.getRequestURI();
        metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        if (metaAlias == null) {
            logError("MetaAliasNotFound", 
                             LogUtil.MISSING_META_ALIAS, metaAlias);
            throw new SAML2Exception(
                 SAML2Utils.bundle.getString("MetaAliasNotFound"));
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntity = metaManager.getEntityByMetaAlias(metaAlias);
        String hostRole = SAML2Utils.getHostEntityRole(paramsMap);
        boolean isSupported = false;
        if (IDP_ROLE.equals(hostRole)) {
            isSupported = SAML2Utils.isIDPProfileBindingSupported(
                realm, hostEntity,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_REDIRECT);
        } else {
            isSupported = SAML2Utils.isSPProfileBindingSupported(
                realm, hostEntity,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_REDIRECT);
        }
        if (!isSupported) {
            logger.error(method +
                "MNI binding: Redirect is not supported for " + hostEntity);
            String[] data = { hostEntity, SAML2Constants.HTTP_REDIRECT };
            LogUtil.error(
                Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "unsupportedBinding"));
        }


        // Retrieve ManageNameIDRequest 
        ManageNameIDRequest mniRequest = getMNIRequest(request);
        remoteEntityID = mniRequest.getIssuer().getValue();
        if (remoteEntityID == null)  {
            logError("nullRemoteEntityID", 
                            LogUtil.MISSING_ENTITY, remoteEntityID);
            throw new SAML2Exception(
                 SAML2Utils.bundle.getString("nullRemoteEntityID"));
        }

        boolean needToVerify = 
            SAML2Utils.getWantMNIRequestSigned(realm, hostEntity, hostRole);
        if (needToVerify) {
            queryString = request.getQueryString();
            boolean valid = 
                    SAML2Utils.verifyQueryString(queryString, realm,
                            hostRole, remoteEntityID);
            if (!valid) {
                logError("invalidSignInRequest", 
                                LogUtil.MNI_REQUEST_INVALID_SIGNATURE, null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidSignInRequest"));
            }
        }
        
        String relayState =
                request.getParameter(SAML2Constants.RELAY_STATE);

        if (logger.isDebugEnabled()) {
            logger.debug(method + "Meta Alias is : "+ metaAlias);
            logger.debug(method + "Remote EntityID is : " + remoteEntityID);
            logger.debug(method + "Host Entity role is : " + hostRole);
            logger.debug(method + "Relay state is : " + relayState);
        }
        
        try {
            EndpointType mniService =
                getMNIServiceElement(realm, remoteEntityID, 
                hostRole, SAML2Constants.HTTP_REDIRECT);
            String mniURL = mniService.getResponseLocation();
            if (mniURL == null){
                mniURL = mniService.getLocation();
            }
            ManageNameIDResponse mniResponse = processManageNameIDRequest(
                mniRequest, metaAlias, remoteEntityID, paramsMap, mniURL, 
                SAML2Constants.HTTP_REDIRECT, request, response);
            sendMNIResponse(response, mniResponse, mniURL, relayState, realm, 
                hostEntity, hostRole, remoteEntityID);        
        } catch (SAML2MetaException e) {
            logError("metaDataError", LogUtil.METADATA_ERROR, null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));            
        }
    }

    /**
     * Parses the request parameters and process the ManageNameID
     * Request from the remote entity.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param paramsMap Map of all other parameters.
     * @throws SAML2Exception if error occurred while processing the request.
     * @throws IOException if error generation DOM from input stream.
     * @throws SOAPException if error generating soap message.
     * @throws ServletException if request length is invalid.
     */
    public static void processSOAPRequest(HttpServletRequest request,
        HttpServletResponse response, Map paramsMap) 
        throws SAML2Exception, IOException, SOAPException, ServletException {

        String method = "processSOAPRequest: ";
        String metaAlias = null;
        String remoteEntityID = null;
        String requestURL = request.getRequestURI();
        String hostEntityRole = SAML2Utils.getHostEntityRole(paramsMap);

        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(request);
        metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        if (metaAlias == null) {
            logError("MetaAliasNotFound", 
                                 LogUtil.MISSING_META_ALIAS, metaAlias);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("MetaAliasNotFound"));
        }
        
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntity = metaManager.getEntityByMetaAlias(metaAlias);
        boolean isSupported = false;
        if (IDP_ROLE.equals(hostEntityRole)) {
            isSupported = SAML2Utils.isIDPProfileBindingSupported(
                realm, hostEntity,
                SAML2Constants.MNI_SERVICE, SAML2Constants.SOAP);
        } else {
            isSupported = SAML2Utils.isSPProfileBindingSupported(
                realm, hostEntity,
                SAML2Constants.MNI_SERVICE, SAML2Constants.SOAP);
        }
        if (!isSupported) {
            logger.error(method +
                "MNI binding: SOAP is not supported for " + hostEntity);
            String[] data = { hostEntity, SAML2Constants.SOAP };
            LogUtil.error(
                Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "unsupportedBinding"));
        }

        // Retrieve a SOAPMessage
        SOAPMessage message = SOAPCommunicator.getInstance().getSOAPMessage(request);

        ManageNameIDRequest mniRequest = getMNIRequest(message);
        remoteEntityID = mniRequest.getIssuer().getValue();
        if (remoteEntityID == null)  {
            logError("nullRemoteEntityID", 
                                 LogUtil.MISSING_ENTITY, metaAlias);
            throw new SAML2Exception(
                 SAML2Utils.bundle.getString("nullRemoteEntityID"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "Meta Alias is : "+ metaAlias);
            logger.debug(method + "Host EntityID is : " + hostEntity);
            logger.debug(method + "Remote EntityID is : " + remoteEntityID);
        }
            
        String dest = mniRequest.getDestination();     
        boolean valid = verifyMNIRequest(mniRequest, realm, remoteEntityID, 
            hostEntity, hostEntityRole, dest);
        if (!valid)  {
            logError("invalidSignInRequest", 
                         LogUtil.MNI_REQUEST_INVALID_SIGNATURE, metaAlias);
            throw new SAML2Exception(
                      SAML2Utils.bundle.getString("invalidSignInRequest"));
        }

        ManageNameIDResponse mniResponse = processManageNameIDRequest(
            mniRequest, metaAlias, remoteEntityID, paramsMap, null,
            SAML2Constants.SOAP, request, response);
        
        signMNIResponse(mniResponse, realm, hostEntity, 
            hostEntityRole, remoteEntityID);

        SOAPMessage reply = SOAPCommunicator.getInstance().createSOAPMessage(
                mniResponse.toXMLString(true, true), false);
        if (reply != null) {
            /*  Need to call saveChanges because we're
             * going to use the MimeHeaders to set HTTP
             * response information. These MimeHeaders
             * are generated as part of the save. */
            if (reply.saveRequired()) {
                reply.saveChanges();
            }
        
            response.setStatus(HttpServletResponse.SC_OK);
            SAML2Utils.putHeaders(reply.getMimeHeaders(), response);
            // Write out the message on the response stream
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            reply.writeTo(stream);
            response.getWriter().println(stream.toString("UTF-8"));
            response.getWriter().flush();
        } else {
            logError("errorObtainResponse", 
                                 LogUtil.CANNOT_INSTANTIATE_MNI_RESPONSE, null);
            throw new SAML2Exception(
                           SAML2Utils.bundle.getString("errorObtainResponse"));
        }
    }

    /**
     * Parses the request parameters and builds the Authentication
     * Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param paramsMap Map of all other parameters.
     * @return return true if the processing is successful.
     * @throws SAML2Exception if error initiating request to IDP.
     */
    public static boolean processManageNameIDResponse(
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Map paramsMap)
                               throws SAML2Exception {
        String method = "processManageNameIDResponse: ";
        boolean success = false;
        String requestURL = request.getRequestURI();
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        if (metaAlias == null) {
            logError("MetaAliasNotFound", LogUtil.MISSING_META_ALIAS, null);
            throw new SAML2Exception(
                 SAML2Utils.bundle.getString("MetaAliasNotFound"));
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
        String hostRole = SAML2Utils.getHostEntityRole(paramsMap);
        boolean isSupported = false;
        if (IDP_ROLE.equals(hostRole)) {
            isSupported = SAML2Utils.isIDPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_REDIRECT);
        } else {
            isSupported = SAML2Utils.isSPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_REDIRECT);
        }
        if (!isSupported) {
            logger.error(method +
                "MNI binding: Redirect is not supported for " + hostEntityID);
            String[] data = { hostEntityID, SAML2Constants.HTTP_REDIRECT };
            LogUtil.error(
                Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "unsupportedBinding"));
        }

        
        String relayState =
                    request.getParameter(SAML2Constants.RELAY_STATE);
        String mniRes =
                    request.getParameter(SAML2Constants.SAML_RESPONSE);
        
        String mniResStr = SAML2Utils.decodeFromRedirect(mniRes);
        if (mniResStr == null) {
            logError("nullDecodedStrFromSamlResponse", 
                                 LogUtil.CANNOT_DECODE_RESPONSE, null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullDecodedStrFromSamlResponse"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "Meta Alias is : "+ metaAlias);
            logger.debug(method + "Host role is : " + hostRole);
            logger.debug(method + "Relay state is : " + relayState);
            logger.debug(method + "MNI Response : " + mniResStr);
        }

        String requestUrl = request.getRequestURL().toString();

        // Validate the RelayState URL.
        SAML2Utils.validateRelayStateURL(realm,
                                         hostEntityID,
                                         relayState,
                                         hostRole,
                                         requestUrl);
                    
        ManageNameIDResponse mniResponse = null;
        try {
            mniResponse = pf.createManageNameIDResponse(mniResStr);
            String remoteEntityID = mniResponse.getIssuer().getValue();
            Issuer resIssuer = mniResponse.getIssuer();
            String requestId = mniResponse.getInResponseTo();
            SAML2Utils.verifyResponseIssuer(realm, hostEntityID, resIssuer,
                requestId);
                            
            boolean needToVerify = SAML2Utils.getWantMNIResponseSigned(realm,
                hostEntityID, hostRole);
            if (needToVerify) {
                String queryString = request.getQueryString();
                boolean valid = SAML2Utils.verifyQueryString(queryString, realm,
                                hostRole, remoteEntityID);
                if (!valid) {
                    logError("invalidSignInResponse", 
                            LogUtil.MNI_RESPONSE_INVALID_SIGNATURE, null);
                        throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "invalidSignInResponse"));
                }
            }
            
            StringBuffer mniUserId = new StringBuffer(); 
            success = checkMNIResponse(mniResponse, realm, hostEntityID,
                hostRole, mniUserId);

            if (success && (hostRole != null) && 
                hostRole.equals(SP_ROLE)) {
                // invoke SPAdapter for termination success
                postTerminationSuccess(hostEntityID, realm, request, response,
                    mniUserId.toString(), null, mniResponse,
                    SAML2Constants.HTTP_REDIRECT);
            }
        } catch (SessionException e) {
            logError("invalidSSOToken", 
                                 LogUtil.INVALID_SSOTOKEN, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidSSOToken"));
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "Request success : " + success);
        }
        
        return success;
    }

    private static Status processManageNameIDRequest(
        ManageNameIDRequest mniRequest, String realm, String hostEntityID,
        String remoteEntityID, String hostRole, String userID)
        throws Exception {

        String method = "processManageNameIDRequest: ";
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "Host EntityID is : "+ hostEntityID);
            logger.debug(method + "Host role is : " + hostRole);
            logger.debug(method + "Realm  is : " + realm);
        }

        NameID nameID = getNameIDFromMNIRequest(mniRequest, realm, 
            hostEntityID, hostRole);
        if (nameID == null) {
            logger.debug("Null NameID");
            logError("unknownPrincipal", LogUtil.UNKNOWN_PRINCIPAL,
                    mniRequest.toXMLString(true, true));
            return SAML2Utils.generateStatus(SAML2Constants.REQUESTER,
                    SAML2Constants.UNKNOWN_PRINCIPAL, null);
        }
        NameIDInfo oldNameIDInfo = getNameIDInfo(userID, hostEntityID,
            remoteEntityID, hostRole, realm, nameID.getSPNameQualifier(),
            true);

        NameID oldNameID = null;
        if (oldNameIDInfo != null) {
            oldNameID = oldNameIDInfo.getNameID();
        }

        if (oldNameID == null) {
            // log manage name id failure
            logger.debug("Null oldNameID");
            logError("unknownPrincipal", LogUtil.UNKNOWN_PRINCIPAL,
                mniRequest.toXMLString(true, true));
            return SAML2Utils.generateStatus(SAML2Constants.REQUESTER,
                SAML2Constants.UNKNOWN_PRINCIPAL, null);
        }

        List spFedSessions = null;

        IDPSession idpSession = null;
        // Terminate
        if (hostRole.equalsIgnoreCase(IDP_ROLE)) {
            idpSession = 
                removeIDPFedSession(remoteEntityID, oldNameID.getValue());
        } else {
            spFedSessions = (List)SPCache.fedSessionListsByNameIDInfoKey.remove(
                oldNameIDInfo.getNameIDInfoKey().toValueString());
            if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                saml2Svc.setFedSessionCount(
		    (long)SPCache.fedSessionListsByNameIDInfoKey.size());
            }
        }
                
        if (!AccountUtils.removeAccountFederation(oldNameIDInfo, userID)) {
            // log termination failure
            logError("unableToTerminate", LogUtil.UNABLE_TO_TERMINATE, userID);
            return SAML2Utils.generateStatus(SAML2Constants.RESPONDER,
                SAML2Utils.bundle.getString("unableToTerminate"));
        }

        if (mniRequest.getTerminate()) {
            // log termination success
            logAccess("requestSuccess", LogUtil.SUCCESS_FED_TERMINATION, 
                userID);
            return SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
                SAML2Utils.bundle.getString("requestSuccess"));
        }

        // newID case
        NewID newID = getNewIDFromMNIRequest(mniRequest, realm, hostEntityID,
            hostRole);

        boolean isAffiliation = oldNameIDInfo.isAffiliation();
        String spNameQualifier = oldNameID.getSPNameQualifier();
        if (hostRole.equalsIgnoreCase(IDP_ROLE)){
            NameID newNameID = AssertionFactory.getInstance().createNameID();
            newNameID.setValue(oldNameID.getValue());
            newNameID.setNameQualifier(oldNameID.getNameQualifier());
            newNameID.setSPNameQualifier(spNameQualifier);
            newNameID.setFormat(oldNameID.getFormat());
            newNameID.setSPProvidedID(newID.getValue());

            NameIDInfo newNameIDinfo = new NameIDInfo(hostEntityID,
                (isAffiliation ? spNameQualifier : remoteEntityID), newNameID,
                IDP_ROLE, isAffiliation);

            AccountUtils.setAccountFederation(newNameIDinfo, userID);
            if (idpSession != null) {
                // there are active session using this Name id
                NameIDandSPpair pair = new NameIDandSPpair(newNameID,
                    remoteEntityID);
                synchronized(IDPCache.idpSessionsByIndices) {
                    List list = (List) idpSession.getNameIDandSPpairs();
                    list.add(pair);
                }
            }
            // log new name id success
            logAccess("requestSuccess", LogUtil.SUCCESS_NEW_NAMEID, userID);
            return SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
                SAML2Utils.bundle.getString("requestSuccess"));
        }

        // SP ROLE
        NameID newNameID = AssertionFactory.getInstance().createNameID();
        newNameID.setValue(newID.getValue());
        newNameID.setNameQualifier(oldNameID.getNameQualifier());
        newNameID.setSPProvidedID(oldNameID.getSPProvidedID());
        newNameID.setSPNameQualifier(spNameQualifier);
        newNameID.setFormat(oldNameID.getFormat());

        NameIDInfo newNameIDInfo = new NameIDInfo(
            (isAffiliation ? spNameQualifier : hostEntityID), remoteEntityID,
            newNameID, hostRole, isAffiliation);

        AccountUtils.setAccountFederation(newNameIDInfo, userID);

        if (spFedSessions != null) {
            String newInfoKeyStr =
                newNameIDInfo.getNameIDInfoKey().toValueString();

            String infoKeyAttribute = AccountUtils.getNameIDInfoKeyAttribute();

            synchronized (spFedSessions) {
                for(Iterator iter = spFedSessions.iterator(); iter.hasNext();){
                    SPFedSession spFedSession = (SPFedSession)iter.next();
                    spFedSession.info = newNameIDInfo;
                    String tokenID = spFedSession.spTokenID;
                    try {
                        Object session = sessionProvider.getSession(tokenID);
                        String[] fromToken = sessionProvider.getProperty(
                            session, infoKeyAttribute);
                        if ((fromToken == null) || (fromToken.length == 0) ||
                            (fromToken[0] == null) ||
                            (fromToken[0].length() == 0)) {

                            String[] values = { newInfoKeyStr };
                            sessionProvider.setProperty(session,
                                infoKeyAttribute, values);
                        } else {
                            if (fromToken[0].indexOf(newInfoKeyStr) == -1) {
                                String[] values = { fromToken[0] +
                                    SAML2Constants.SECOND_DELIM +
                                    newInfoKeyStr };
                                sessionProvider.setProperty(session,
                                    infoKeyAttribute, values);
                            }
                        }
                    } catch (SessionException ex) {
                        logger.error("DoManageNameID." +
                            "processManageNameIDRequest:", ex);
                    }
                }
            }
            SPCache.fedSessionListsByNameIDInfoKey.put(newInfoKeyStr,
                spFedSessions);
            if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                saml2Svc.setFedSessionCount(
		    (long)SPCache.fedSessionListsByNameIDInfoKey.size());
            }
        }
        // log new name id success
        logAccess("requestSuccess", LogUtil.SUCCESS_NEW_NAMEID, userID);
        return SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
            SAML2Utils.bundle.getString("requestSuccess"));
    }

    private static ManageNameIDResponse processManageNameIDRequest(
        ManageNameIDRequest mniRequest,
        String metaAlias,
        String remoteEntityID,
        Map paramsMap,
        String destination,
        String binding,
        HttpServletRequest request,
        HttpServletResponse response) {

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityID = null;
        String hostRole = null;
        Status status = null;

        String userID = null;
        try {
            hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
            hostRole = SAML2Utils.getHostEntityRole(paramsMap);
            SAML2Utils.verifyRequestIssuer(realm, hostEntityID, 
                mniRequest.getIssuer(), mniRequest.getID());

            if (hostRole.equalsIgnoreCase(IDP_ROLE)) {
                IDPAccountMapper idpAcctMapper = SAML2Utils.getIDPAccountMapper(
                    realm, hostEntityID);
                userID = idpAcctMapper.getIdentity(mniRequest, hostEntityID,
                    realm);
            } else if (hostRole.equalsIgnoreCase(SP_ROLE)) {
                SPAccountMapper spAcctMapper = SAML2Utils.getSPAccountMapper(
                    realm, hostEntityID);
                userID = spAcctMapper.getIdentity(mniRequest, hostEntityID,
                    realm);
            }

            if (userID == null) {
                status =  SAML2Utils.generateStatus(SAML2Constants.REQUESTER,
                    SAML2Constants.UNKNOWN_PRINCIPAL, null);
            } else {
                status = processManageNameIDRequest(mniRequest, realm,
                    hostEntityID, remoteEntityID, hostRole, userID);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("DoManageNameID.processManageNameIDRequest:", e);
            }
            status = SAML2Utils.generateStatus(SAML2Constants.RESPONDER,
                e.toString());
        }

        ManageNameIDResponse mniResponse = null;
        try {
            String responseID = SAML2Utils.generateID();
            if (responseID == null) {
                logger.error(
                        SAML2Utils.bundle.getString("failedToGenResponseID"));
            }
            mniResponse = pf.createManageNameIDResponse();
            mniResponse.setStatus(status);
            mniResponse.setID(responseID);
            mniResponse.setInResponseTo(mniRequest.getID());
            mniResponse.setVersion(SAML2Constants.VERSION_2_0);
            mniResponse.setIssueInstant(newDate());
            mniResponse.setIssuer(SAML2Utils.createIssuer(hostEntityID)); 
            if (destination != null && (destination.length() != 0)) {
                mniResponse.setDestination(
                    XMLUtils.escapeSpecialCharacters(destination));
            }
        } catch (SAML2Exception e) {
            logger.error("Error : ", e);
        }
        
        if (hostRole.equalsIgnoreCase(SP_ROLE) &&
            mniResponse.getStatus().getStatusCode().getValue().equals(
            SAML2Constants.SUCCESS)) {
            // invoke SPAdapter for post temination success
            postTerminationSuccess(hostEntityID, realm, request, response,
                userID, mniRequest, mniResponse, binding);
        } 

        return mniResponse;
    }
    
    private static void sendMNIResponse(HttpServletResponse response,
                                           ManageNameIDResponse mniResponse, 
                                           String mniURL,
                                           String relayState,
                                           String realm, 
                                           String hostEntity,
                                           String hostEntityRole, 
                                           String remoteEntity)        
        throws SAML2Exception {
        String method = "sendMNIResponse: ";
            
        try {
            String mniResXMLString = mniResponse.toXMLString(true, true);
            // encode the xml string
            String encodedXML = SAML2Utils.encodeForRedirect(mniResXMLString);
                
            StringBuffer queryString = 
                        new StringBuffer().append(SAML2Constants.SAML_RESPONSE)
                                          .append(SAML2Constants.EQUAL)
                                          .append(encodedXML);
                
            if (relayState != null && relayState.length() > 0 
                && relayState.getBytes("UTF-8").length <= 80) {
                queryString.append("&").append(SAML2Constants.RELAY_STATE)
                           .append("=").append(urlEncodeQueryParameterNameOrValue(relayState));
            }
            if (logger.isDebugEnabled()) {
                logger.debug(method + "MNI Response is : " +
                    mniResXMLString);
                logger.debug(method + "Relay State is : " + relayState);
            }
            mniResponse.setDestination(XMLUtils.escapeSpecialCharacters(
                mniURL));    

            String remoteEntityRole = hostEntityRole.equalsIgnoreCase(IDP_ROLE) ? SP_ROLE : IDP_ROLE;
            boolean needToSign = SAML2Utils.getWantMNIResponseSigned(realm, remoteEntity, SP_ROLE);

            String signedQueryString = queryString.toString();
            if (needToSign) {
                logger.debug("QueryString has need to be signed.");
                signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity, hostEntityRole,
                        remoteEntity, remoteEntityRole);
            }

            String redirectURL = mniURL + (mniURL.contains("?") ? "&" : "?") +
                signedQueryString;

            if (logger.isDebugEnabled()) {
                logger.debug(method + "redirectURL is : " + redirectURL);
            }
                
            response.sendRedirect(redirectURL);
        } catch (java.io.IOException ioe) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception when redirecting to " +
                            relayState, ioe);
            }
        }
    }

    static private ManageNameIDRequest createManageNameIDRequest(
        Object session, String realm, String hostEntityID,
        String hostEntityRole, String remoteEntityID, String destination,
        boolean changeID, String affiliationID) throws SAML2Exception {

        String method = "DoManageNameID.createManageNameIDRequest: ";

        NameID nameID = null;
        String userID = null;
        
        try {
            userID = sessionProvider.getPrincipalName(session);
            nameID = getNameID(userID, hostEntityID, remoteEntityID,
                hostEntityRole, affiliationID, realm);
             
        } catch (SessionException e) {
            logError("invalidSSOToken", LogUtil.INVALID_SSOTOKEN, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSSOToken"));       
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method + "User ID : " + userID);
            logger.debug(method + "NameID : " + nameID.toXMLString());
        }
        
        ManageNameIDRequest mniRequest = pf.createManageNameIDRequest();
        
        mniRequest.setID(SAML2Utils.generateID());
        mniRequest.setVersion(SAML2Constants.VERSION_2_0);
        mniRequest.setDestination(XMLUtils.escapeSpecialCharacters(
            destination));
        mniRequest.setIssuer(SAML2Utils.createIssuer(hostEntityID));
        mniRequest.setIssueInstant(newDate());
        setNameIDForMNIRequest(mniRequest, nameID, changeID, realm,
            hostEntityID, hostEntityRole, remoteEntityID);

        if (!changeID) {
            mniRequest.setTerminate(true);
        }
        return mniRequest;
    }
    
    static private ManageNameIDRequest getMNIRequest(HttpServletRequest request)
        throws SAML2Exception {

        String binding = request.getParameter("binding");
        String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);

        if (logger.isDebugEnabled()) {
            logger.debug("DoManageNameID.getMNIRequest: SAMLRequest = " +
                samlRequest);
        }

        if (samlRequest == null) {
            logError("nullManageIDRequest", 
                LogUtil.CANNOT_INSTANTIATE_MNI_REQUEST , samlRequest);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullManageIDRequest"));
        }

        if ((binding != null) && binding.equals(SAML2Constants.HTTP_POST)) {
            return getMNIRequestFromPost(samlRequest);
        } else {
            String decodedStr = SAML2Utils.decodeFromRedirect(samlRequest);
                
            if (decodedStr == null) {
                logError("nullDecodedStrFromSamlRequest", 
                    LogUtil.CANNOT_DECODE_REQUEST, samlRequest);
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "nullDecodedStrFromSamlRequest"));
            }   
            return pf.createManageNameIDRequest(decodedStr);
        }
    }
    
    // This is the application code for handling the message.
    static private ManageNameIDRequest getMNIRequest(SOAPMessage message)
                throws SAML2Exception {
        Element reqElem = SOAPCommunicator.getInstance().getSamlpElement(message,
                "ManageNameIDRequest");
        ManageNameIDRequest manageRequest = 
            pf.createManageNameIDRequest(reqElem);
        return manageRequest;
    }
    
    static private void doMNIByHttpRedirect(
        String mniRequestXMLString,
        String mniURL,
        String relayState,
        String realm, 
        String hostEntity,
        String hostEntityRole, 
        String remoteEntity,        
        HttpServletResponse response) throws SAML2Exception, IOException {
        String method = "doMNIByHttpRedirect: ";
        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(mniRequestXMLString);
        
        StringBuffer queryString = 
                new StringBuffer().append(SAML2Constants.SAML_REQUEST)
                                  .append(SAML2Constants.EQUAL)
                                  .append(encodedXML);
        if (relayState != null && relayState.length() > 0 && relayState.getBytes("UTF-8").length <= 80) {
            queryString.append("&").append(SAML2Constants.RELAY_STATE)
                       .append("=").append(urlEncodeQueryParameterNameOrValue(relayState));
         }

        String remoteEntityRole = hostEntityRole.equalsIgnoreCase(IDP_ROLE) ? SP_ROLE : IDP_ROLE;
        boolean needToSign = SAML2Utils.getWantMNIRequestSigned(realm, remoteEntity, remoteEntityRole);

        String signedQueryString = queryString.toString();
        if (needToSign) {
            signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity, hostEntityRole,
                    remoteEntity, remoteEntityRole);
        }

        String redirectURL = mniURL + (mniURL.contains("?") ? "&" : "?") +
            signedQueryString;
        if (logger.isDebugEnabled()) {
            logger.debug(method + "MNIRequestXMLString : "
                                          + mniRequestXMLString);
            logger.debug(method + "MNIRedirectURL : " + mniURL);
            logger.debug(method + "MNIRedirectURL : " + redirectURL);
        }
        
        response.sendRedirect(redirectURL);
    }

    static private boolean doMNIBySOAP(
        ManageNameIDRequest mniRequest,
                        String mniURL,
                        String metaAlias,
        String hostRole,
        HttpServletRequest request,
        HttpServletResponse response) throws SAML2Exception {


        String method = "doMNIBySOAP: ";
        boolean success = false;

        String mniRequestXMLString= mniRequest.toXMLString(true,true);

        if (logger.isDebugEnabled()) {
            logger.debug(method + "MNIRequestXMLString : "
                                          + mniRequestXMLString);
            logger.debug(method + "MNIRedirectURL : " + mniURL);
        }
        
        SOAPMessage resMsg = null;
        try {
            resMsg = SOAPCommunicator.getInstance().sendSOAPMessage(mniRequestXMLString, mniURL,
                    true);
        } catch (SOAPException se) {
            logger.error(SAML2Utils.bundle.getString("invalidSOAPMessge"), se);
            return false;
        }
        
        Element mniRespElem = SOAPCommunicator.getInstance().getSamlpElement(resMsg,
                "ManageNameIDResponse");
        ManageNameIDResponse mniResponse = 
            mniResponse = pf.createManageNameIDResponse(mniRespElem);
        
        if (logger.isDebugEnabled()) {
            if (mniResponse != null) {
                logger.debug(method + "ManageNameIDResponse without "+
                    "SOAP envelope:\n" + mniResponse.toXMLString());
            } else {
                logger.debug(method + "ManageNameIDResponse is null ");
            }
        }

        if (mniResponse != null) {
            try {
                String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
                String hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
                String remoteEntityID = mniResponse.getIssuer().getValue();
                Issuer resIssuer = mniResponse.getIssuer();
                String requestId = mniResponse.getInResponseTo();
                SAML2Utils.verifyResponseIssuer(realm, hostEntityID, resIssuer,
                    requestId);
                    
                boolean validSign = verifyMNIResponse(mniResponse, realm,
                    remoteEntityID, hostEntityID, hostRole,
                    mniResponse.getDestination());
                if (!validSign) {
                    logError("invalidSignInResponse",
                        LogUtil.CANNOT_INSTANTIATE_MNI_RESPONSE , null);
                    throw new SAML2Exception(
                       SAML2Utils.bundle.getString("invalidSignInResponse"));
                }
                StringBuffer mniUserId = new StringBuffer();
                success = checkMNIResponse(mniResponse, realm, hostEntityID,
                                           hostRole, mniUserId);
                if (success && hostRole.equals(SP_ROLE)) {
                   // invoke SPAdapter for termination success, SP initied SOAP
                    postTerminationSuccess(hostEntityID, realm, request, response,
                        mniUserId.toString(), mniRequest, mniResponse,
                        SAML2Constants.SOAP);
                }
            } catch (SessionException e) {
                logger.error(SAML2Utils.bundle.getString("invalidSSOToken"), e);
                throw new SAML2Exception(e.toString());
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "Request success : " + success);
        }
        return success;
    }

    private static boolean checkMNIResponse(ManageNameIDResponse mniResponse,
        String realm, String hostEntityID, String hostRole,
        StringBuffer mniUserId) throws SAML2Exception, SessionException {

        boolean success = false;
        
        String remoteEntityID = mniResponse.getIssuer().getValue();
        String requestID = mniResponse.getInResponseTo();
        ManageNameIDRequestInfo reqInfo = getMNIRequestInfo(requestID,
            hostRole);
        if (reqInfo == null) {
            logError("invalidInResponseToInResponse", 
                     LogUtil.INVALID_MNI_RESPONSE , null);
            throw new SAML2Exception(
                 SAML2Utils.bundle.getString("invalidInResponseToInResponse"));
        }
        String retCode = mniResponse.getStatus().getStatusCode().getValue();

        if (retCode.equalsIgnoreCase(SAML2Constants.SUCCESS)) {
            Object session = reqInfo.getSession();
            if (session == null) {
                logError("nullSSOToken", LogUtil.INVALID_SSOTOKEN , null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullSSOToken"));
            }

            String userID = sessionProvider.getPrincipalName(session);
            mniUserId.append(userID);

            ManageNameIDRequest origMniReq = reqInfo.getManageNameIDRequest();
            NameID oldNameID = origMniReq.getNameID();
            List spFedSessions = null;
            NameIDInfo oldNameIDInfo = getNameIDInfo(userID, hostEntityID,
                remoteEntityID, hostRole, realm,
                oldNameID.getSPNameQualifier(), true);
            if (oldNameIDInfo == null) {
                logger.error("DoManageNameID.checkMNIResponse: NameIDInfo " +
                    "not found.");
                return false;
            }

            // Terminate
            if (hostRole.equalsIgnoreCase(SP_ROLE)) {
                String infoKeyStr =
                    oldNameIDInfo.getNameIDInfoKey().toValueString();
                spFedSessions = (List)SPCache.fedSessionListsByNameIDInfoKey.
                    remove(infoKeyStr);
                removeInfoKeyFromSession(session, infoKeyStr);
                if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
                    saml2Svc.setFedSessionCount(
		        (long)SPCache.fedSessionListsByNameIDInfoKey.size());
                }
            } else {
                removeIDPFedSession(remoteEntityID, oldNameID.getValue());
            }

            if (!AccountUtils.removeAccountFederation(oldNameIDInfo, userID)) {
                // log termination failure
                logError("unableToTerminate", LogUtil.UNABLE_TO_TERMINATE, 
                    userID);
                return false;
            }

            if (origMniReq.getTerminate()) {
                // log termination success
                logAccess("requestSuccess", LogUtil.SUCCESS_FED_TERMINATION, 
                    userID);
                return true;
            }

            // newID case
            String newIDValue = origMniReq.getNewID().getValue();
            boolean isAffiliation = oldNameIDInfo.isAffiliation();
            String spNameQualifier = oldNameID.getSPNameQualifier();
            if (hostRole.equalsIgnoreCase(SP_ROLE)) {
                NameID newNameID = AssertionFactory.getInstance().
                    createNameID();

                newNameID.setValue(oldNameID.getValue());
                newNameID.setFormat(oldNameID.getFormat());
                newNameID.setSPProvidedID(newIDValue);
                newNameID.setSPNameQualifier(spNameQualifier);
                newNameID.setNameQualifier(oldNameID.getNameQualifier());

                NameIDInfo newNameIDInfo = new NameIDInfo(
                    (isAffiliation ? spNameQualifier : hostEntityID),
                    remoteEntityID, newNameID, hostRole, isAffiliation);

                String newInfoKeyStr =
                    newNameIDInfo.getNameIDInfoKey().toValueString();
                if (spFedSessions != null) {
                    SPCache.fedSessionListsByNameIDInfoKey.put(
                        newInfoKeyStr, spFedSessions);
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml2Svc != null))
                    {
                        saml2Svc.setFedSessionCount(
		            (long)SPCache.fedSessionListsByNameIDInfoKey.
				size());
                    }
                }

                AccountUtils.setAccountFederation(newNameIDInfo, userID);

                try {
                    String infoKeyAttribute = 
                        AccountUtils.getNameIDInfoKeyAttribute();

                    String[] fromToken = sessionProvider.getProperty(
                        session, infoKeyAttribute);
                    if ((fromToken == null) || (fromToken.length == 0) ||
                        (fromToken[0] == null) ||
                        (fromToken[0].length() == 0)) {

                        String[] values = { newInfoKeyStr };
                        sessionProvider.setProperty(session,
                            infoKeyAttribute, values);
                    } else {
                        if (fromToken[0].indexOf(newInfoKeyStr) == -1) {
                            String[] values = { fromToken[0] +
                                SAML2Constants.SECOND_DELIM +
                                newInfoKeyStr };
                            sessionProvider.setProperty(session,
                                infoKeyAttribute, values);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("DoManageNameID.checkMNIResponse:",e);
                }        

            } else if (hostRole.equalsIgnoreCase(IDP_ROLE)){
                NameID newNameID = AssertionFactory.getInstance().
                    createNameID();

                newNameID.setValue(newIDValue);
                newNameID.setFormat(oldNameID.getFormat());
                newNameID.setSPProvidedID(oldNameID.getSPProvidedID());
                newNameID.setSPNameQualifier(spNameQualifier);
                newNameID.setNameQualifier(hostEntityID);

                NameIDInfo newNameIDInfo = new NameIDInfo(hostEntityID,
                    (isAffiliation ? spNameQualifier : remoteEntityID),
                    newNameID, IDP_ROLE, isAffiliation);

                AccountUtils.setAccountFederation(newNameIDInfo, userID);
                NameIDandSPpair pair = new NameIDandSPpair(newNameID,
                    remoteEntityID);
                IDPSession idpSession =
                    (IDPSession)IDPCache.idpSessionsBySessionID.
                    get(sessionProvider.getSessionID(session));

                if (idpSession != null) {
                    synchronized(IDPCache.idpSessionsByIndices) {
                        List list = (List)idpSession.getNameIDandSPpairs();
                        list.add(pair);
                    }
                }
            }
            // log manage name id success
            logAccess("newNameIDSuccess", LogUtil.SUCCESS_NEW_NAMEID, userID);
            success = true;

        } else {
            logError("mniFailed", LogUtil.INVALID_MNI_RESPONSE , null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("mniFailed"));
        }
        
        return success;
    }

    private static ManageNameIDRequestInfo getMNIRequestInfo(
        String requestID, String hostRole) {

        if (hostRole.equalsIgnoreCase(SP_ROLE)) {
            return (ManageNameIDRequestInfo)
                SPCache.mniRequestHash.get(requestID);
        } else if (hostRole.equalsIgnoreCase(IDP_ROLE)) {
            return (ManageNameIDRequestInfo)
                IDPCache.mniRequestHash.get(requestID);
        }

        return null;
    }

    private static NameIDInfo getNameIDInfo(String userID, String hostEntityID,
        String remoteEntityID, String hostRole, String realm,
        String affiliationID, boolean invalidAffiIDAllowed)
        throws SAML2Exception {
    
        NameIDInfo nameInfo = null;
        if (StringUtils.isNotEmpty(affiliationID)) {
            AffiliationDescriptorType affiDesc =
                metaManager.getAffiliationDescriptor(realm, affiliationID);
            if (affiDesc != null) {
                if (hostRole.equals(SP_ROLE)) {
                    if (!affiDesc.getAffiliateMember().contains(hostEntityID)){
                        throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "spNotAffiliationMember"));
                    }
                    nameInfo = AccountUtils.getAccountFederation(userID,
                        affiliationID, remoteEntityID);
                } else {
                    if (!affiDesc.getAffiliateMember().contains(
                        remoteEntityID)) {
                        throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "spNotAffiliationMember"));
                    }
                    nameInfo = AccountUtils.getAccountFederation(userID,
                        hostEntityID, affiliationID);
                }
            } else if (invalidAffiIDAllowed) {
                nameInfo = AccountUtils.getAccountFederation(userID,
                    hostEntityID, remoteEntityID);
            } else {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "affiliationNotFound"));
            }
        } else {
            nameInfo = AccountUtils.getAccountFederation(userID, hostEntityID,
                remoteEntityID);
        }

        return nameInfo;
    }

    private static boolean removeFedAccount(String userID, String hostEntityID,
        String remoteEntityID, String hostRole, String realm,
        String affiliationID) throws SAML2Exception {

        NameIDInfo nameInfo = getNameIDInfo(userID, hostEntityID,
             remoteEntityID, hostRole, realm, affiliationID, true);
        return AccountUtils.removeAccountFederation(nameInfo, userID);
    }
    
    private static EndpointType getMNIServiceElement(
                    String realm, String entityID,  
                    String hostEntityRole, String binding)
        throws SAML2MetaException, SessionException, SAML2Exception {
        EndpointType mniService = null;
        String method = "getMNIServiceElement: ";
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + "Realm : " + realm);
            logger.debug(method + "Entity ID : " + entityID);
            logger.debug(method + "Host Entity Role : " + hostEntityRole);
        }

        if (hostEntityRole.equalsIgnoreCase(SP_ROLE)) {
            mniService = getIDPManageNameIDConfig(realm, entityID, binding);
        } else if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)){
            mniService = getSPManageNameIDConfig(realm, entityID, binding);
        } else {
            logError("nullHostEntityRole", 
                             LogUtil.MISSING_ENTITY_ROLE , null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullHostEntityRole"));
        }
        
        return mniService;
    }

    static private NameID getNameID(String userID, String hostEntityID,
        String remoteEntityID, String hostEntityRole, String affiliationID,
        String realm) throws SAML2Exception {

        NameIDInfo nameIDInfo = getNameIDInfo(userID, hostEntityID,
            remoteEntityID, hostEntityRole, realm, affiliationID, false);

        NameID nameID = null;
        if (nameIDInfo != null) {
            nameID = nameIDInfo.getNameID();
            if (logger.isDebugEnabled()) {
                logger.debug("DoManageNameID.getNameID: userID = " +
                    userID + ", nameID = " + nameID.toXMLString());
            }
        } else {
            logger.error("DoManageNameID.getNameID: " +
                SAML2Utils.bundle.getString("nullNameID"));
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullNameID"));
        }
        
        return nameID;
    }    
    
    static private void setNameIDForMNIRequest(ManageNameIDRequest mniRequest, 
        NameID nameID, boolean changeID, String realm, String hostEntity,
        String hostEntityRole, String remoteEntity) throws SAML2Exception {

        String method = "DoManageNameID.setNameIDForMNIRequest: ";
        boolean needEncryptIt = false;
        
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            needEncryptIt = SAML2Utils.getWantNameIDEncrypted(realm,
                remoteEntity, SP_ROLE);
        } else {
            needEncryptIt = SAML2Utils.getWantNameIDEncrypted(realm,
                remoteEntity, IDP_ROLE);
        }

        NewID newID = null;
        if (changeID) {
            String newIDValue = SAML2Utils.createNameIdentifier();
            newID = ProtocolFactory.getInstance().createNewID(newIDValue);
            mniRequest.setNewID(newID);
        }

        mniRequest.setNameID(nameID);

        if (!needEncryptIt) {
            logger.debug("{} NameID doesn't need to be encrypted.", method);
            return;
        }
        
        EncryptionConfig encryptionConfig = null;
        if (hostEntityRole.equalsIgnoreCase(IDP_ROLE)) {
            SPSSODescriptorType spSSODesc =
                metaManager.getSPSSODescriptor(realm, remoteEntity);
            encryptionConfig = KeyUtil.getEncryptionConfig(spSSODesc, remoteEntity, SP_ROLE, realm);
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
            logError("UnableToFindEncryptKeyInfo", LogUtil.METADATA_ERROR,
                null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "UnableToFindEncryptKeyInfo"));
        }
        
        EncryptedID encryptedID = nameID.encrypt(encryptionConfig, remoteEntity);
        // This non-encrypted NameID will be removed just 
        // after saveMNIRequestInfo and just before it send to 
        mniRequest.setEncryptedID(encryptedID);

        if (newID != null) {
            NewEncryptedID newEncID = newID.encrypt(encryptionConfig, remoteEntity);
            // This non-encrypted newID will be removed just 
            // after saveMNIRequestInfo and just before it send to 
            mniRequest.setNewEncryptedID(newEncID);
        }

    }    

    static private NewID getNewIDFromMNIRequest(ManageNameIDRequest request,
        String realm, String hostEntityID, String hostEntityRole)
        throws SAML2Exception {
        
        boolean needDecryptIt = SAML2Utils.getWantNameIDEncrypted(realm, 
            hostEntityID, hostEntityRole);
        
        if (!needDecryptIt) {
            if (logger.isDebugEnabled()) {
                logger.debug("DoManageNameID.getNewIDFromMNIRequest: " +
                    "NameID doesn't need to be decrypted.");
                logger.debug("DoManageNameID.getNewIDFromMNIRequest: " +
                    "request is " + request);
            }
            NewID newID = null;
            if (request != null) {
                newID = request.getNewID();
                logger.debug("DoManageNameID.getNewIDFromMNIRequest: " +
                    "newid is " + newID.getValue());
            }

            return newID;
        }

        NewEncryptedID encryptedID = request.getNewEncryptedID();

        final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveValidDecryptionCredentials(realm, hostEntityID, Saml2EntityRole.fromString(hostEntityRole));
        return encryptedID.decrypt(decryptionKeys);
    }    

    static private NameID getNameIDFromMNIRequest(ManageNameIDRequest request, 
        String realm, String hostEntity, String hostEntityRole)
        throws SAML2Exception {

        String method = "DoManageNameID.getNameIDFromMNIRequest: ";
        
        boolean needDecryptIt = SAML2Utils.getWantNameIDEncrypted(realm, 
            hostEntity, hostEntityRole);
        
        if (!needDecryptIt) {
            logger.debug("{} NameID doesn't need to be decrypted.", method);
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

    /**
     * Returns first ManageNameID configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>ManageNameIDServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     * @throws SessionException invalid or expired single-sign-on session
     */
    static public EndpointType getIDPManageNameIDConfig(
                                                 String realm, 
                                                 String entityId,
                                                 String binding)
        throws SAML2MetaException, SessionException {
        EndpointType mni = null;

        IDPSSODescriptorType idpSSODesc =
                    metaManager.getIDPSSODescriptor(realm, entityId);
        if (idpSSODesc == null) {
                logger.error(SAML2Utils.bundle.getString("noIDPEntry"));
            return null;
        }

        List<EndpointType> list = idpSSODesc.getManageNameIDService();

        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return list.get(0);
            }
            Iterator<EndpointType> it = list.iterator();
            while (it.hasNext()) {
                mni = it.next();
                if (binding.equalsIgnoreCase(mni.getBinding())) {
                    break;
                }
            }
        }

        return mni;
    }

    /**
     * Returns first ManageNameID configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>ManageNameIDServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     * @throws SessionException invalid or expired single-sign-on session.
     */
    static public EndpointType getSPManageNameIDConfig(
                                                String realm, String entityId,
                                                String binding)
        throws SAML2MetaException, SessionException {
        EndpointType mni = null;

        SPSSODescriptorType spSSODesc =
                          metaManager.getSPSSODescriptor(realm, entityId);
        if (spSSODesc == null) {
            return null;
        }

        List<EndpointType> list = spSSODesc.getManageNameIDService();

        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return list.get(0);
            }
            Iterator<EndpointType> it = list.iterator();
            while (it.hasNext()) {
                mni = it.next();
                if (binding.equalsIgnoreCase(mni.getBinding())) {
                    break;
                }
            }
        }

        return mni;
    }

    static IDPSession removeIDPFedSession(String spEntity, String nameID) {
        String method = "DoManageNameID.removeIDPFedSession ";
        Enumeration keys = null;
        String idpSessionIndex = null;
        IDPSession idpSession = null;
        
        if (logger.isDebugEnabled()) {
            logger.debug(method + " trying to remove entity=" + spEntity
               + ", nameID=" + nameID + " from IDP session cache");
        }
        if (IDPCache.idpSessionsByIndices != null) {
            keys = IDPCache.idpSessionsByIndices.keys();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(method+"IDPCache.idpSessionsByIndices is null.");
            }

            return null;
        }
        
        if (keys == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(method +
                   "IDPCache.idpSessionsByIndices return null.");
            }
            return null;
        }
        
        while (keys.hasMoreElements()) {
            NameIDandSPpair nameIDPair = null;
            idpSessionIndex = (String)keys.nextElement();   
            idpSession = (IDPSession)IDPCache.
                    idpSessionsByIndices.get(idpSessionIndex);
            if (idpSession != null) {
                List nameIDSPlist = idpSession.getNameIDandSPpairs();
                if (nameIDSPlist != null) {
                    // synchronize to avoid con-current modification
                    synchronized (nameIDSPlist) {
                        Iterator iter = nameIDSPlist.listIterator();
                        while (iter.hasNext()) {
                            nameIDPair = (NameIDandSPpair) iter.next();
                            String spID = nameIDPair.getSPEntityID();
                            if (spID.equalsIgnoreCase(spEntity) && nameIDPair.
                                getNameID().getValue().equals(nameID)) {
                                iter.remove();
                                if (logger.isDebugEnabled()) {
                                    logger.debug(method + " removed entity="
                                        + spID + ", nameID=" + nameID);
                                }
                                return idpSession;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    static private void removeInfoKeyFromSession(
        Object session, String infoKey) throws SessionException {
        
        String method = "removeInfoKeyFromSession ";
        String infoKeyString = null;
        String[] values = sessionProvider.getProperty(
            session, AccountUtils.getNameIDInfoKeyAttribute());
        if (values != null && values.length > 0) {
            infoKeyString = values[0];
        }
        if (infoKeyString == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(method+"InfoKeyString from session is null.");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(method+"InfoKeyString from session : "
                                + infoKeyString);
            logger.debug(method+"InfoKey need to delete : " + infoKey);
        }

        StringTokenizer st =
                new StringTokenizer(infoKeyString, SAML2Constants.SECOND_DELIM);
        StringBuffer newInfoKey = new StringBuffer();
        if (st != null && st.hasMoreTokens()) {
            while (st.hasMoreTokens()) {
                String tmpInfoKey = (String)st.nextToken();
                logger.debug(method+"InfoKey from session : " + tmpInfoKey);
                if (infoKey.equals(tmpInfoKey)) {
                    continue;
                }
                
                if (newInfoKey.length() > 0){
                    newInfoKey.append(SAML2Constants.SECOND_DELIM);
                }
                newInfoKey.append(tmpInfoKey);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(method+"New InfoKey to session : "
                                + newInfoKey.toString());
            }
            String[] v = { newInfoKey.toString() };
            sessionProvider.setProperty(
                session, AccountUtils.getNameIDInfoKeyAttribute(), v);
            if (logger.isDebugEnabled()) {
                logger.debug("{}New InfoKey from session : {}", method, Arrays.toString(
                        sessionProvider.getProperty(session, AccountUtils.getNameIDInfoKeyAttribute())));
            }
        } else {
            logger.debug(method+"No InfoKey to remove.");
            return;
        }
    }    

    private static void doMNIByPOST(String mniXMLString, String mniURL,
        String relayState, String realm, String hostEntity,
        String hostEntityRole, String remoteEntity,
        HttpServletResponse response, HttpServletRequest request) throws SAML2Exception {

        String encMsg = SAML2Utils.encodeForPOST(mniXMLString);
        SAML2Utils.postToTarget(request, response, "SAMLRequest", encMsg, "RelayState",
            relayState, mniURL);
    }

    public static void processPOSTRequest(HttpServletRequest request,
        HttpServletResponse response, Map paramsMap) 
        throws SAML2Exception, IOException, SOAPException, SessionException,
        ServletException {

        String classMethod = "DoManageNameID.processPOSTRequest:";
        String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);

        if (samlRequest == null) {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "MissingSAMLRequest",
                SAML2Utils.bundle.getString("MissingSAMLRequest"));
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "MissingSAMLRequest"));
        }
        String metaAlias =
            SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (metaAlias == null) {
            logError("MetaAliasNotFound", LogUtil.MISSING_META_ALIAS,
                metaAlias);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "MetaAliasNotFound"));
        }

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
        String hostEntityRole = SAML2Utils.getHostEntityRole(paramsMap);
        boolean isSupported = false;
        if (IDP_ROLE.equals(hostEntityRole)) {
            isSupported = SAML2Utils.isIDPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_POST);
        } else {
            isSupported = SAML2Utils.isSPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_POST);
        }
        if (!isSupported) {
            logger.error(classMethod +
                "MNI binding: POST is not supported for " + hostEntityID);
            String[] data = { hostEntityID, SAML2Constants.HTTP_POST };
            LogUtil.error(
                Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "unsupportedBinding"));
        }

        ManageNameIDRequest mniRequest = null;

        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlRequest);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis);
                if (doc != null) {
                    mniRequest = ProtocolFactory.getInstance().
                        createManageNameIDRequest(doc.getDocumentElement());
                }
            }
        } catch (SAML2Exception se) {
            logger.error("DoManageNameID.processPOSTRequest:", se);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "nullDecodedStrFromSamlResponse",
                SAML2Utils.bundle.getString("nullDecodedStrFromSamlResponse") +
                " " + se.getMessage());
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        } catch (Exception e) {
            logger.error("DoManageNameID.processPOSTRequest:", e);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "nullDecodedStrFromSamlResponse",
                SAML2Utils.bundle.getString("nullDecodedStrFromSamlResponse") +
                " " + e.getMessage());
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("DoManageNameID.processPOSTRequest:",ie);
                    }
                }
            }
        }

        if (mniRequest != null) {
            String remoteEntityID = mniRequest.getIssuer().getValue();
            if (remoteEntityID == null)  {
               logError("nullRemoteEntityID", LogUtil.MISSING_ENTITY, metaAlias);
               throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "nullRemoteEntityID"));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("DoManageNameID.processPOSTRequest: " +
                    "Meta Alias is : "+ metaAlias);
                logger.debug("DoManageNameID.processPOSTRequest: " +
                    "Host EntityID is : " + hostEntityID);
                logger.debug("DoManageNameID.processPOSTRequest: " +
                    "Remote EntityID is : " + remoteEntityID);
            }
            
            String dest = mniRequest.getDestination();
            boolean valid = verifyMNIRequest(mniRequest, realm, remoteEntityID,
                hostEntityID, hostEntityRole, dest);
            if (!valid)  {
                logError("invalidSignInRequest",
                    LogUtil.MNI_REQUEST_INVALID_SIGNATURE, metaAlias);
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSignInRequest"));
            }
            EndpointType mniService = getMNIServiceElement(realm,
                remoteEntityID, hostEntityRole, SAML2Constants.HTTP_POST);
            String mniURL = mniService.getResponseLocation();
            if (mniURL == null){
                mniURL = mniService.getLocation();
            }

            ///common for post, redirect, soap
            ManageNameIDResponse mniResponse = processManageNameIDRequest(
                mniRequest, metaAlias, remoteEntityID,paramsMap, null,
                SAML2Constants.HTTP_POST, request, response);
        
            signMNIResponse(mniResponse, realm, hostEntityID, hostEntityRole,
                remoteEntityID);

            //send MNI Response by POST
            String mniRespString = mniResponse.toXMLString(true, true);
            String encMsg  = SAML2Utils.encodeForPOST(mniRespString);

            String relayState = request.getParameter(SAML2Constants.RELAY_STATE);

            try {
                SAML2Utils.postToTarget(request, response, "SAMLResponse", encMsg,
                    "RelayState", relayState, mniURL);
            } catch (Exception e) {
                logger.debug("DoManageNameID.processPOSTRequest:", e);
                throw new SAML2Exception("Error posting to target");
            }
        }

        return;
    }

    static String getMNIResponseFromPost(String samlResponse,
        HttpServletResponse response) throws SAML2Exception {

        if (samlResponse == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "missingSAMLResponse"));
        }

        ManageNameIDResponse resp = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlResponse);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis);
                if (doc != null) {
                    resp = ProtocolFactory.getInstance().
                        createManageNameIDResponse(doc.getDocumentElement());
                }
            }
        } catch (SAML2Exception se) {
            logger.error("DoManageNameID.getMNIResponseFromPost:", se);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        } catch (Exception e) {
            logger.error("DoManageNameID.getMNIResponseFromPost:", e);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("DoManageNameID.getMNIResponseFromPost:",
                            ie);
                    }
                }
            }
        }

        String respStr = null;
        if (resp != null) {
            respStr = resp.toXMLString();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("DoManageNameID.getMNIResponseFromPost: " + respStr);
        }
        return respStr;
    }

    public static boolean processMNIResponsePOST(HttpServletRequest request,
        HttpServletResponse response, Map paramsMap)
        throws SAML2Exception {

        String method = "processMNIResponsePOST: ";
        boolean success = false;
        String requestURL = request.getRequestURI();
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        if (metaAlias == null) {
            logError("MetaAliasNotFound", LogUtil.MISSING_META_ALIAS, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "MetaAliasNotFound"));
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntityID = metaManager.getEntityByMetaAlias(metaAlias);
        String hostRole = SAML2Utils.getHostEntityRole(paramsMap);
        boolean isSupported = false;
        if (IDP_ROLE.equals(hostRole)) {
            isSupported = SAML2Utils.isIDPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_POST);
        } else {
            isSupported = SAML2Utils.isSPProfileBindingSupported(
                realm, hostEntityID,
                SAML2Constants.MNI_SERVICE, SAML2Constants.HTTP_POST);
        }
        if (!isSupported) {
            logger.error(method +
                "MNI binding: POST is not supported for " + hostEntityID);
            String[] data = { hostEntityID, SAML2Constants.HTTP_POST };
            LogUtil.error(
                Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "unsupportedBinding"));
        }
        String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
        String mniRes = request.getParameter(SAML2Constants.SAML_RESPONSE);

        String mniResStr = getMNIResponseFromPost(mniRes,response);

        if (mniResStr == null) {
            logError("nullDecodedStrFromSamlResponse", 
                LogUtil.CANNOT_DECODE_RESPONSE, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("DoManageNameID.processMNIResponsePOST: " +
                "Meta Alias is : "+ metaAlias);
            logger.debug("DoManageNameID.processMNIResponsePOST: " +
                "Host role is : " + hostRole);
            logger.debug("DoManageNameID.processMNIResponsePOST: " +
                "Relay state is : " + relayState);
            logger.debug("DoManageNameID.processMNIResponsePOST: " +
                "MNI Response : " + mniResStr);
        }

        String requestUrl = request.getRequestURL().toString();
        // Validate the RelayState URL.
        SAML2Utils.validateRelayStateURL(realm,
                                         hostEntityID,
                                         relayState,
                                         hostRole,
                                         requestUrl);

        ManageNameIDResponse mniResponse = null;
        try {
            mniResponse = pf.createManageNameIDResponse(mniResStr);
            String remoteEntityID = mniResponse.getIssuer().getValue();
            Issuer resIssuer = mniResponse.getIssuer();
            String requestId = mniResponse.getInResponseTo();

            SAML2Utils.verifyResponseIssuer(realm, hostEntityID, resIssuer,
                requestId);
                            
            boolean needToVerify = SAML2Utils.getWantMNIResponseSigned(realm,
                hostEntityID, hostRole);
              
            if (needToVerify) {
                boolean valid = verifyMNIResponse(mniResponse, realm, 
                    remoteEntityID, hostEntityID, hostRole, 
                    mniResponse.getDestination());

                if (!valid) {
                    logError("invalidSignInResponse",
                        LogUtil.MNI_RESPONSE_INVALID_SIGNATURE, null);
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "invalidSignInResponse"));
                }
            }

            success = checkMNIResponse(mniResponse, realm, hostEntityID,
                hostRole, new StringBuffer());
        } catch (SessionException e) {
            logError("invalidSSOToken", LogUtil.INVALID_SSOTOKEN, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSSOToken"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("DoManageNameID.processMNIResponsePOST: " +
                "Request success : " + success);
        }

        return success;
    }

    static ManageNameIDRequest getMNIRequestFromPost(String samlRequest)
        throws SAML2Exception {

        logger.debug("DoManageNameID.getMNIRequestFromPost: samlRequest = " +
            samlRequest);

        ManageNameIDRequest mniReq = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlRequest);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis);
                if (doc != null) {
                    mniReq = ProtocolFactory.getInstance().
                        createManageNameIDRequest(doc.getDocumentElement());
                }
            }
        } catch (SAML2Exception se) {
            logger.error("DoManageNameID.getMNIRequestFromPost:", se);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        } catch (Exception e) {
            logger.error("DoManageNameID.getMNIRequestFromPost:", e);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
        }  finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("DoManageNameID.getMNIRequestFromPost:",
                          ie);
                    }
                }
            }
        }
        return mniReq;
    }
}
