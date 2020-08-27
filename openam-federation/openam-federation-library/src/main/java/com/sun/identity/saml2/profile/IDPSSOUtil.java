/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IDPSSOUtil.java,v 1.56 2009/11/24 21:53:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2020 ForgeRock AS.
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static com.sun.identity.saml2.common.SAML2Utils.getBooleanAttributeValueFromSSOConfig;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.AuthLevelUtils.authLevelToInt;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.shared.concurrency.LockFactory;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.NewBoolean;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2InvalidNameIDPolicyException;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPResponse;
import com.sun.identity.saml2.idpdiscovery.IDPDiscoveryConstants;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The utility class is used by the identity provider to process
 * the authentication request from a service provider and send back
 * a proper response.
 * The identity provider can also send unsolicited response to a service
 * provider to do single sign on and/or federation.
 */
public class IDPSSOUtil {
    private static final Logger logger = LoggerFactory.getLogger(IDPSSOUtil.class);
    // key name for name id format on SSOToken
    public static final String NAMEID_FORMAT = "SAML2NameIDFormat";
    public static final String NULL = "null";
    private static final String REDIRECTED = "redirected";
    private static final String REDIRECTED_TRUE = "redirected=true";
    private static final LockFactory<String> LOCK_FACTORY = new LockFactory<>();
    public static SAML2MetaManager metaManager = null;
    public static CircleOfTrustManager cotManager = null;
    static IDPSessionListener sessionListener = new IDPSessionListener();
    static SessionProvider sessionProvider = null;

    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;

    static {
        try {
            metaManager = new SAML2MetaManager();
            cotManager = new CircleOfTrustManager();
        } catch (COTException ce) {
            logger.error("Error retrieving circle of trust");
        } catch (SAML2MetaException sme) {
            logger.error("Error retrieving metadata", sme);
        }
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            logger.error(
                    "IDPSSOUtil static block: Error getting SessionProvider.",
                    se);
        }

        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }

    private IDPSSOUtil() {
    }

    /**
     * Does SSO with existing federation or new federation
     *
     * @param request      the <code>HttpServletRequest</code> object
     * @param response     the <code>HttpServletResponse</code> object
     * @param out          the print writer for writing out presentation
     * @param authnReq     the <code>AuthnRequest</code> object
     * @param spEntityID   the entity id of the service provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param nameIDFormat the <code>NameIDFormat</code>
     * @param relayState   the relay state
     * @param auditor      the auditor for logging SAML2 Events - may be null
     * @throws SAML2Exception if the operation is not successful
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     AuthnRequest authnReq,
                                     String spEntityID,
                                     String idpMetaAlias,
                                     String nameIDFormat,
                                     String relayState,
                                     SAML2EventLogger auditor)
            throws SAML2Exception {
        doSSOFederate(request, response, out, authnReq,
                spEntityID, idpMetaAlias, nameIDFormat,
                relayState, null, auditor);
    }

    /**
     * Does SSO with existing federation or new federation
     *
     * @param request      the <code>HttpServletRequest</code> object
     * @param response     the <code>HttpServletResponse</code> object
     * @param out          the print writer for writing out presentation
     * @param authnReq     the <code>AuthnRequest</code> object
     * @param spEntityID   the entity id of the service provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param nameIDFormat the <code>NameIDFormat</code>
     * @param relayState   the relay state
     * @param newSession   Session used in IDP Proxy Case
     * @param auditor      the auditor for logging SAML2 Events
     * @throws SAML2Exception if the operation is not successful
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     AuthnRequest authnReq,
                                     String spEntityID,
                                     String idpMetaAlias,
                                     String nameIDFormat,
                                     String relayState,
                                     Object newSession,
                                     SAML2EventLogger auditor)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.doSSOFederate: ";

        Object session = null;
        if (newSession != null) {
            session = newSession;
            auditor.setSSOTokenId(session);
        } else {
            try {
                session = sessionProvider.getSession(request);
                if (null != auditor) {
                    auditor.setAuthTokenId(session);
                }
            } catch (SessionException se) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            classMethod + "No session yet.");
                }
            }
        }

        // log the authnRequest       
        String authnRequestStr = null;
        if (authnReq != null) {
            authnRequestStr = authnReq.toXMLString();
            auditor.setRequestId(authnReq.getID());
        }
        String[] logdata = {spEntityID, idpMetaAlias, authnRequestStr};
        LogUtil.access(Level.INFO,
                LogUtil.RECEIVED_AUTHN_REQUEST, logdata, session);

        // retrieve IDP entity id from meta alias
        String idpEntityID = null;
        String realm = null;
        try {
            if (metaManager == null) {
                logger.error(classMethod +
                        "Unable to get meta manager.");
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorMetaManager"));
            }
            idpEntityID = metaManager.getEntityByMetaAlias(idpMetaAlias);
            if ((idpEntityID == null)
                    || (idpEntityID.trim().length() == 0)) {
                logger.error(classMethod +
                        "Unable to get IDP Entity ID from meta.");
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_IDP, data, session);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
            realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
        } catch (SAML2MetaException sme) {
            logger.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        // check if the remote provider is valid
        if (authnReq == null) {
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(spEntityID);
            if (!SAML2Utils.isSourceSiteValid(issuer, realm, idpEntityID)) {
                if (logger.isWarnEnabled()) {
                    logger.warn(classMethod +
                            "The remote provider is not valid.");
                }
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidReceiver"));
            }
        }

        // Check to see if session upgrade is required
        IDPAuthnContextMapper idpAuthnContextMapper = getIDPAuthnContextMapper(realm, idpEntityID);
        IDPAuthnContextInfo contextInfo = idpAuthnContextMapper.getIDPAuthnContextInfo(authnReq, idpEntityID, realm);
        Integer idpDefaultAuthLevel = contextInfo.getAuthnLevel();
        logger.debug("{}idpDefaultContextAuthLevel = {}", classMethod, idpDefaultAuthLevel);

        String authLevel = getAuthLevel(session);
        if (StringUtils.isNotEmpty(authLevel) && idpDefaultAuthLevel > authLevelToInt(authLevel)) {
            logger.debug("{}Session authLevel is less than the Default Authentication " +
                    "Context authLevel. Session upgrade is required.", classMethod);
            try {
                redirectAuthentication(request, response, authnReq, null, realm, idpEntityID, spEntityID);
            } catch (IOException ioe) {
                logger.error("{}Unable to redirect to authentication.", classMethod, ioe);
                SAMLUtils.sendError(request, response, response.SC_INTERNAL_SERVER_ERROR, "UnableToRedirectToAuth",
                        SAML2Utils.bundle.getString("UnableToRedirectToAuth"));
            }
            return;
        }

        if (authnReq == null && (session == null || !isValidSessionInRealm(realm, session))) {
            // idp initiated and not logged in yet, need to authenticate
            try {
                if (Boolean.parseBoolean(request.getParameter(REDIRECTED))) {
                    if (session == null) {
                        String[] data = {idpEntityID};
                        logger.error(classMethod + "The IdP was not able to create a session");
                        LogUtil.error(Level.INFO, LogUtil.SSO_NOT_FOUND, data, session, null);
                    } else {
                        try {
                            String ipAddress = ClientUtils.getClientIPAddress(request);
                            String sessionRealm = SAML2Utils.getSingleValuedSessionProperty(session,
                                    SAML2Constants.ORGANIZATION);
                            String[] data = {sessionRealm, realm, spEntityID, ipAddress, null};
                            logger.error(classMethod + "The realm of the session (" + sessionRealm
                                    + ") does not correspond to that of the IdP (" + realm + ")");
                            LogUtil.error(Level.INFO, LogUtil.INVALID_REALM_FOR_SESSION, data, session, null);
                        } catch (SessionException se) {
                            logger.error(classMethod + "Failed to retrieve realm from session", se);
                        }
                    }
                    String rbKey = "UnableToDOSSOOrFederation";
                    SAMLUtils.sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, rbKey,
                            SAML2Utils.bundle.getString(rbKey));
                } else {
                    redirectAuthentication(request, response, authnReq, null, realm, idpEntityID, spEntityID);
                }
            } catch (IOException ioe) {
                logger.error(classMethod +
                        "Unable to redirect to authentication.", ioe);
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR,
                        "UnableToRedirectToAuth",
                        SAML2Utils.bundle.getString("UnableToRedirectToAuth"));
            }
            return;
        }

        // Invoke the IDP Adapter
        try {
            logger.debug(classMethod + " Invoking the "
                    + "IDP Adapter");
            SAML2IdentityProviderAdapter idpAdapter =
                    IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
            if (idpAdapter != null) {
                // If the preSendResponse returns true we end here
                if (idpAdapter.preSendResponse(authnReq, idpEntityID,
                        realm, request, response, session, null, relayState)) {
                    return;
                }  // else we continue with the logic. Beware of loops
            }
        } catch (SAML2Exception se2) {
            logger.error(classMethod + " There was a problem when invoking"
                    + "the preSendResponse of the IDP Adapter: ", se2);
        }
        // End of invocation

        sendResponseToACS(request, response, out, session, authnReq, spEntityID,
                idpEntityID, idpMetaAlias, realm, nameIDFormat, relayState, null);
    }

    /**
     * Sends <code>Response</code> containing an <code>Assertion</code>
     * back to the requesting service provider
     *
     * @param request              the <code>HttpServletRequest</code> object
     * @param response             the <code>HttpServletResponse</code> object
     * @param out                  the print writer for writing out presentation
     * @param session              user session
     * @param authnReq             the <code>AuthnRequest</code> object
     * @param spEntityID           the entity id of the service provider
     * @param idpEntityID          the entity id of the identity provider
     * @param idpMetaAlias         the meta alias of the identity provider
     * @param realm                the realm
     * @param nameIDFormat         the <code>NameIDFormat</code>
     * @param relayState           the relay state
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find
     *                             authentication type and scheme.
     */
    public static void sendResponseToACS(HttpServletRequest request, HttpServletResponse response, PrintWriter out,
                                         Object session, AuthnRequest authnReq,
                                         String spEntityID, String idpEntityID, String idpMetaAlias,
                                         String realm, String nameIDFormat, String relayState,
                                         AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        StringBuffer returnedBinding = new StringBuffer();
        String acsURL = IDPSSOUtil.getACSurl(
                spEntityID, realm, authnReq, request, returnedBinding);
        String acsBinding = returnedBinding.toString();

        if ((acsURL == null) || (acsURL.trim().length() == 0)) {
            logger.error("IDPSSOUtil.sendResponseToACS:" +
                    " no ACS URL found.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.NO_ACS_URL, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableTofindACSURL"));
        }
        if ((acsBinding == null) || (acsBinding.trim().length() == 0)) {
            logger.error("IDPSSOUtil.sendResponseToACS:" +
                    " no return binding found.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.NO_RETURN_BINDING, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableTofindBinding"));
        }

        String affiliationID = request.getParameter(
                SAML2Constants.AFFILIATION_ID);

        // generate a response for the authn request
        Response res = getResponse(request, session, authnReq, spEntityID, idpEntityID,
                idpMetaAlias, realm, nameIDFormat, acsURL, affiliationID,
                matchingAuthnContext);

        if (res == null) {
            logger.error("IDPSSOUtil.sendResponseToACS:" +
                    " response is null");
            String errorMsg =
                    SAML2Utils.bundle.getString("UnableToCreateAssertion");
            if (authnReq == null) {
                //idp initiated case, will not send error response to sp
                throw new SAML2Exception(errorMsg);
            }
            res = SAML2Utils.getErrorResponse(authnReq,
                    SAML2Constants.RESPONDER, null, errorMsg, idpEntityID);
        } else {
            try {
                String[] values = {idpMetaAlias};
                sessionProvider.setProperty(
                        session, SAML2Constants.IDP_META_ALIAS, values);
            } catch (SessionException e) {
                logger.error("IDPSSOUtil.sendResponseToACS:" +
                        " error setting idpMetaAlias into the session: ", e);
            }
        }

        if (res != null) {
            // call multi-federation protocol to set the protocol
            MultiProtocolUtils.addFederationProtocol(session,
                    SingleLogoutManager.SAML2);
            // check if the COT cookie needs to be set
            if (setCOTCookie(request, response, acsBinding, spEntityID,
                    idpEntityID, idpMetaAlias, realm, relayState, acsURL, res,
                    session)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("IDPSSOUtil.sendResponseToACS:" +
                            " Redirected to set COT cookie.");
                }
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("IDPSSOUtil.sendResponseToACS:" +
                        " Doesn't set COT cookie.");
                logger.debug("IDPSSOUtil.sendResponseToACS:" +
                        " Response is:  " + res.toXMLString());
            }
            try {
                logger.debug("IDPSSOUtil.sendResponseToACS: Invoking the IDP Adapter");
                SAML2IdentityProviderAdapter idpAdapter = IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
                if (idpAdapter != null) {
                    idpAdapter.preSignResponse(authnReq, res, idpEntityID, realm, request, session, relayState);
                }
            } catch (SAML2Exception se) {
                logger.error("IDPSSOUtil.sendResponseToACS: There was a problem when invoking the "
                        + "preSendResponse of the IDP Adapter: ", se);
            }
            sendResponse(request, response, out, acsBinding, spEntityID, idpEntityID,
                    idpMetaAlias, realm, relayState, acsURL, res, session);
        } else {
            logger.error("IDPSSOUtil.sendResponseToACS:" +
                    " error response is null");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableToCreateErrorResponse"));
        }

    }

    private static boolean setCOTCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String acsBinding,
            String spEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String relayState,
            String acsURL,
            Response res,
            Object session) {

        String classMethod = "IDPSSOUtil.setCOTCookie: ";

        String writerURL = getWriterURL(realm, idpEntityID, spEntityID);
        if (writerURL == null) {
            // could not find the writer URL, do not set the COT cookie
            return false;
        }
        // save the needed info into cache so they can be used later
        // when it is redirected back
        ArrayList cacheList = new ArrayList(9);
        cacheList.add(0, acsBinding);
        cacheList.add(1, spEntityID);
        cacheList.add(2, idpEntityID);
        cacheList.add(3, idpMetaAlias);
        cacheList.add(4, realm);
        cacheList.add(5, relayState);
        cacheList.add(6, acsURL);
        cacheList.add(7, res);
        cacheList.add(8, session);
        String cachedResID = SAML2Utils.generateIDWithServerID();
        IDPCache.responseCache.put(cachedResID, cacheList);

        // construct redirect URL
        StringBuffer retURLSB = new StringBuffer(100);
        retURLSB.append(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort())
                .append(request.getRequestURI())
                .append("?")
                .append(SAML2Constants.RES_INFO_ID)
                .append("=")
                .append(cachedResID);
        String retURL = urlEncodeQueryParameterNameOrValue(retURLSB.toString());
        StringBuffer redirectURLSB = new StringBuffer(200);
        redirectURLSB.append(writerURL);
        if (writerURL.indexOf("?") > 0) {
            redirectURLSB.append("&");
        } else {
            redirectURLSB.append("?");
        }
        redirectURLSB.append(IDPDiscoveryConstants.SAML2_COOKIE_NAME)
                .append("=")
                .append(idpEntityID)
                .append("&")
                .append(SAML2Constants.RELAY_STATE)
                .append("=")
                .append(retURL);
        String redirectURL = redirectURLSB.toString();
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod +
                    "Writer redirect URL: " + redirectURL);
        }
        try {
            response.sendRedirect(redirectURL);
            return true;
        } catch (IOException ioe) {
            logger.error(classMethod +
                    "Unable to send redirect: ", ioe);
        }
        return false;
    }

    /**
     * A convenience method to construct response with First-level and Second-level status code for
     * SAML authentication requests.
     *
     * @param request The servlet request.
     * @param response The servlet response.
     * @param out The print writer for writing out presentation.
     * @param idpMetaAlias The IdP's metaAlias.
     * @param idpEntityID The IdP's entity ID.
     * @param realm The realm where the IdP belongs to.
     * @param authnReq The SAML AuthnRequest sent by the SP.
     * @param relayState The RelayState value.
     * @param spEntityID The SP's entity ID.
     * @param firstlevelStatusCodeValue First-level status code value passed.
     * @param secondlevelStatusCodeValue Second-level status code value passed.
     * @throws SAML2Exception If there was an error while creating or sending the response back to the SP.
     */
    public static void sendResponseWithStatus(HttpServletRequest request, HttpServletResponse response,
                                                     PrintWriter out, String idpMetaAlias, String idpEntityID,
                                                     String realm, AuthnRequest authnReq, String relayState,
                                                     String spEntityID, String firstlevelStatusCodeValue,
                                                     String secondlevelStatusCodeValue) throws SAML2Exception {
        Response res = SAML2Utils.getErrorResponse(authnReq, firstlevelStatusCodeValue, secondlevelStatusCodeValue,
                null, idpEntityID);
        StringBuffer returnedBinding = new StringBuffer();
        String acsURL = IDPSSOUtil.getACSurl(spEntityID, realm, authnReq, request, returnedBinding);
        String acsBinding = returnedBinding.toString();
        sendResponse(request, response, out, acsBinding, spEntityID, idpEntityID, idpMetaAlias, realm, relayState,
                acsURL, res, null);
    }

    /**
     * Sends a response to service provider
     *
     * @param request The servlet request.
     * @param response The servlet response.
     * @param out The print writer for writing out presentation.
     * @param cachedResID the key used to retrieve response information
     *                    from the response information cache
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            PrintWriter out,
            String cachedResID)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponse: ";
        ArrayList cacheList =
                (ArrayList) IDPCache.responseCache.remove(cachedResID);
        if ((cacheList != null) && (cacheList.size() == 9)) {
            String acsBinding = (String) cacheList.get(0);
            String spEntityID = (String) cacheList.get(1);
            String idpEntityID = (String) cacheList.get(2);
            String idpMetaAlias = (String) cacheList.get(3);
            String realm = (String) cacheList.get(4);
            String relayState = (String) cacheList.get(5);
            String acsURL = (String) cacheList.get(6);
            Response res = (Response) cacheList.get(7);
            Object session = cacheList.get(8);
            sendResponse(request, response, out, acsBinding, spEntityID, idpEntityID,
                    idpMetaAlias, realm, relayState, acsURL, res, session);
        } else {
            logger.error(classMethod +
                    "unable to get response information from cache.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString(
                            "UnableToGetResponseInfoFromCache"));
        }
    }

    /**
     * Sends a response to service provider
     *
     * @param response     the <code>HttpServletResponse</code> object
     * @param acsBinding   the assertion consumer service binding
     * @param spEntityID   the entity id of the service provider
     * @param idpEntityID  the entity id of the identity provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param realm        the realm name
     * @param relayState   the relay state
     * @param acsURL       the assertion consumer service <code>url</code>
     * @param res          the <code>SAML Response</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            PrintWriter out,
            String acsBinding,
            String spEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String relayState,
            String acsURL,
            Response res,
            Object session)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponse: ";

        String nameIDString = SAML2Utils.getNameIDStringFromResponse(res);
        Map props = new HashMap();
        props.put(LogUtil.NAME_ID, nameIDString);

        // send the response back through HTTP POST or Artifact
        if (acsBinding.equals(SAML2Constants.HTTP_POST)) {  
            // 4.1.4.5 POST-Specific Processing Rules (sstc-saml-profiles-errata-2.0-wd-06-diff.pdf)
            //If response is not signed and POST binding is used, the assertion(s) MUST be signed.
            // encryption is optional based on SP config settings.
            boolean signAssertion = true;
            // check if response needs to be signed.
            boolean signResponse = SAML2Utils.wantPOSTResponseSigned(realm, spEntityID, SP_ROLE);
            // if signing response then signing assertion is optional
            // so will base on wantAssertionsSigned flag
            if (signResponse) {
                signAssertion = wantAssertionsSigned(spEntityID, realm);
            }
            signAndEncryptResponseComponents(realm, spEntityID, idpEntityID, res, signAssertion);
            if (signResponse) {
                signResponse(realm, idpEntityID, spEntityID, res);
            }

            String resMsg = res.toXMLString(true, true);
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "SAML Response content :\n" + resMsg);
            }
            String encodedResMsg = SAML2Utils.encodeForPOST(resMsg);

            String[] logdata1 = {spEntityID, idpMetaAlias, resMsg};
            LogUtil.access(Level.INFO,
                    LogUtil.POST_RESPONSE, logdata1, session, props);
            try {
                SAML2Utils.postToTarget(request, response, "SAMLResponse",
                        encodedResMsg, "RelayState", relayState, acsURL);
            } catch (SAML2Exception saml2E) {
                String[] data = {acsURL};
                LogUtil.error(Level.INFO, LogUtil.POST_TO_TARGET_FAILED, data, session, props);
                throw saml2E;
            }
        } else if (acsBinding.equals(SAML2Constants.HTTP_ARTIFACT)) {
            IDPSSOUtil.sendResponseArtifact(request, response, idpEntityID,
                    spEntityID, realm, acsURL, relayState, res, session, props);
        } else if (acsBinding.equals(SAML2Constants.PAOS)) {
            // signing assertion is a must for ECP profile.
            // encryption is optional based on SP config settings.
            signAndEncryptResponseComponents(
                    realm, spEntityID, idpEntityID, res, true);
            IDPSSOUtil.sendResponseECP(request, response, out, idpEntityID,
                    realm, acsURL, res);
        } else {
            logger.error(classMethod +
                    "unsupported return binding.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnSupportedReturnBinding"));
        }
    }

    /**
     * Returns a <code>SAML Response</code> object.
     *
     * @param request The HTTP request.
     * @param session The user's session object.
     * @param authnReq The <code>AuthnRequest</code> object.
     * @param recipientEntityID The entity ID of the response recipient.
     * @param idpEntityID The entity ID of the identity provider.
     * @param realm The realm name.
     * @param nameIDFormat The <code>NameIDFormat</code>.
     * @param acsURL The <code>ACS</code> service <code>url</code>.
     * @param affiliationID AffiliationID for IDP initiated SSO.
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find authentication type and scheme.
     * @return the <code>SAML Response</code> object.
     * @throws SAML2Exception if the operation is not successful.
     */
    public static Response getResponse(
            HttpServletRequest request,
            Object session,
            AuthnRequest authnReq,
            String recipientEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String nameIDFormat,
            String acsURL,
            String affiliationID,
            AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getResponse: ";

        Response res = ProtocolFactory.getInstance().createResponse();
        Status status = ProtocolFactory.getInstance().createStatus();
        if (status == null) {
            return null;
        }
        StatusCode statusCode = ProtocolFactory.getInstance().
                createStatusCode();
        if (statusCode == null) {
            return null;
        }
        try {
            List assertionList = new ArrayList();

            Assertion assertion = getAssertion(request, session, authnReq,
                    recipientEntityID, idpEntityID, idpMetaAlias, realm,
                    nameIDFormat, acsURL, affiliationID, matchingAuthnContext);

            if (assertion == null) {
                logger.error(
                        classMethod + "Unable to get Assertion.");
                return null;
            }

            assertionList.add(assertion);
            res.setAssertion(assertionList);

            statusCode.setValue(SAML2Constants.SUCCESS);
        } catch (SAML2InvalidNameIDPolicyException se) {
            statusCode.setValue(SAML2Constants.REQUESTER);
            StatusCode subStatusCode = ProtocolFactory.getInstance().
                    createStatusCode();
            subStatusCode.setValue(SAML2Constants.INVALID_NAME_ID_POLICY);
            statusCode.setStatusCode(subStatusCode);
            status.setStatusMessage(se.getMessage());
        }
        status.setStatusCode(statusCode);
        res.setStatus(status);

        if (authnReq != null) {
            // sp initiated case, need to set InResponseTo attribute
            res.setInResponseTo(authnReq.getID());
        }
        res.setVersion(SAML2Constants.VERSION_2_0);
        res.setIssueInstant(newDate());
        res.setID(SAML2Utils.generateID());

        // set the idp entity id as the response issuer
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);
        res.setIssuer(issuer);
        res.setDestination(XMLUtils.escapeSpecialCharacters(acsURL));
        return res;
    }


    /**
     * Returns a <code>SAML Assertion</code> object
     *
     * @throws SAML2Exception if the operation is not successful
     * @param request The HTTP request.
     * @param session The user's session object.
     * @param authnReq The <code>AuthnRequest</code> object.
     * @param recipientEntityID The entity ID of the response recipient.
     * @param idpEntityID The entity ID of the identity provider.
     * @param realm The realm name.
     * @param nameIDFormat The <code>NameIDFormat</code>.
     * @param acsURL The <code>ACS</code> service <code>url</code>.
     * @param affiliationID AffiliationID for IDP initiated SSO.
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find authentication type and scheme.
     * @return the <code>SAML Assertion</code> object.
     * @throws SAML2Exception if the operation is not successful.
     */
    private static Assertion getAssertion(
            HttpServletRequest request,
            Object session,
            AuthnRequest authnReq,
            String recipientEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String nameIDFormat,
            String acsURL,
            String affiliationID,
            AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getAssertion: ";
        Assertion assertion = AssertionFactory.getInstance().createAssertion();
        String assertionID = SAML2Utils.generateID();
        assertion.setID(assertionID);
        assertion.setVersion(SAML2Constants.VERSION_2_0);
        assertion.setIssueInstant(newDate());
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);

        assertion.setIssuer(issuer);

        List statementList = new ArrayList();

        NewBoolean isNewSessionIndex = new NewBoolean();
        AuthnStatement authnStatement = null;
        IDPSession idpSession = null;
        String sessionIndex = null;
        String sessionID = sessionProvider.getSessionID(session);
        Lock lock = LOCK_FACTORY.acquireLock(sessionID);
        try {
            lock.lock();
            authnStatement = getAuthnStatement(request, session, isNewSessionIndex, authnReq, idpEntityID, realm,
                matchingAuthnContext, idpMetaAlias);
            if (authnStatement == null) {
                return null;
            }

            sessionIndex = authnStatement.getSessionIndex();
            if (isNewSessionIndex.getValue()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                        "This is a new IDP session with sessionIndex=" +
                        sessionIndex + ", and sessionID=" +
                        sessionID);
                }
                idpSession = IDPCache.idpSessionsBySessionID.get(sessionProvider.getSessionID(session));
                if (idpSession == null) {
                    logger.debug("Creating new IDPSession for SSO session {}", sessionID);
                    idpSession = new IDPSession(session);
                }
                // Set the metaAlias in the IDP session object
                idpSession.setMetaAlias(idpMetaAlias);

                logger.debug("Putting IDPSession for SSO session {} in IDPCache using sessionIndex {} as key",
                        sessionID, sessionIndex);
                IDPCache.idpSessionsByIndices.put(sessionIndex, idpSession);

                if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                    saml2Svc.setIdpSessionCount(
    		    (long)IDPCache.idpSessionsByIndices.size());
                }
            } else {
                logger.debug("Trying to get IDPSession from IDPCache for sessionIndex key {}",
                        sessionIndex);
                idpSession = retrieveCachedIdPSession(sessionIndex);
            }
        } finally {
            lock.unlock();
        }
		if (isNewSessionIndex.getValue()) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                    "a new IDP session has been saved in cache, " +
                    "with sessionIndex=" + sessionIndex);
            }
            try {
                sessionProvider.addListener(session, sessionListener);
            } catch (SessionException e) {
                logger.error(classMethod +
                    "Unable to add session listener.");
            }
		} else {
            if (idpSession == null) {
                logger.error("IDPSession is null");
                throw new SAML2Exception(SAML2Utils.bundle.getString("IDPSessionIsNULL"));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                        "This is an existing IDP session with sessionIndex="
                        + sessionIndex + ", and sessionID=" +
                        sessionProvider.getSessionID(idpSession.getSession()));
                }
            }
        }
        statementList.add(authnStatement);

        AttributeStatement attrStatement = getAttributeStatement(
                session, idpEntityID, recipientEntityID, realm);
        if (attrStatement != null) {
            List attrStatementList = new ArrayList();
            attrStatementList.add(attrStatement);
            assertion.setAttributeStatements(attrStatementList);
        }

        // get the assertion effective time (in seconds)
        int effectiveTime = getEffectiveTime(realm, idpEntityID);

        // get the NotBefore skew (in seconds)
        int notBeforeSkewTime = getNotBeforeSkewTime(realm, idpEntityID);

        // get the subject element
        Subject subject = getSubject(session, authnReq, acsURL,
                nameIDFormat, realm, idpEntityID,
                recipientEntityID, effectiveTime, affiliationID);

        // register (spEntityID, nameID) with the sso token
        // for later logout use 
        String spEntityID = null;
        if (authnReq != null) {
            spEntityID = authnReq.getIssuer().getValue();
        } else {
            spEntityID = recipientEntityID;
        }
        NameIDandSPpair pair = new NameIDandSPpair(subject.getNameID(), spEntityID);

        synchronized (IDPCache.idpSessionsByIndices) {
            List<NameIDandSPpair> list = idpSession.getNameIDandSPpairs();
            String id;
            if (authnReq != null) {
                id = authnReq.getIssuer().getValue();
            } else {
                id = spEntityID;
            }
            boolean found = false;
            for (NameIDandSPpair nameIDandSPpair : list) {
                if (nameIDandSPpair.getSPEntityID().equals(id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(pair);
            }
        }

        assertion.setAuthnStatements(statementList);
        assertion.setSubject(subject);
        Conditions conditions = getConditions(recipientEntityID,
                notBeforeSkewTime, effectiveTime, realm);
        assertion.setConditions(conditions);

        if (assertionCacheEnabled(realm, idpEntityID)) {
            String userName = null;
            try {
                userName = sessionProvider.getPrincipalName(session);
            } catch (SessionException se) {
                logger.error(classMethod +
                        "Unable to get principal name from the session.", se);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSSOToken"));
            }

            String cacheKey = userName.toLowerCase();

            List assertions = (List) IDPCache.assertionCache.get(cacheKey);
            if (assertions == null) {
                synchronized (IDPCache.assertionCache) {
                    assertions = (List) IDPCache.assertionCache.get(cacheKey);
                    if (assertions == null) {
                        assertions = new ArrayList();
                        IDPCache.assertionCache.put(cacheKey, assertions);
                    }
                }
            }
            synchronized (assertions) {
                assertions.add(assertion);
            }

            IDPCache.assertionByIDCache.put(assertionID, assertion);
            try {
                SAML2FailoverUtils.saveSAML2Token(assertionID, cacheKey,
                        assertion.toXMLString(true, true),
                        conditions.getNotOnOrAfter().getTime() / 1000);
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                            "Saving Assertion to SAML2 Token Repository. ID = " + assertionID);
                }
            } catch (SAML2TokenRepositoryException se) {
                logger.error(classMethod + "Unable to save Assertion to the SAML2 Token Repository", se);
            }
        }
        saveIdPSessionToTokenRepository(sessionIndex, sessionProvider, idpSession, session);
        return assertion;
    }


    /**
     * Returns a <code>SAML AuthnStatement</code> object.
     *
     * @param request The HTTP request.
     * @param session The user's session.
     * @param isNewSessionIndex A returned flag from which the caller knows if the session index in the returned
     *                          <code>AuthnStatement</code> is a new session index.
     * @param authnReq The <code>AuthnRequest</code> object.
     * @param idpEntityID The entity ID of the identity provider.
     * @param realm The realm name.
     * @param matchingAuthnContext The <code>AuthnContext</code> used to find authentication type and scheme.
     * @param metaAlias The meta alias.
     * @return The <code>SAML AuthnStatement</code> object.
     * @throws SAML2Exception If the operation is not successful.
     */
    private static AuthnStatement getAuthnStatement(
            HttpServletRequest request,
            Object session,
            NewBoolean isNewSessionIndex,
            AuthnRequest authnReq,
            String idpEntityID,
            String realm,
            AuthnContext matchingAuthnContext,
            String metaAlias)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getAuthnStatement: ";

        AuthnStatement authnStatement =
                AssertionFactory.getInstance().createAuthnStatement();

        Date authInstant = null;
        // will be used when we add SubjectLocality to the statement
        try {
            String[] values = sessionProvider.getProperty(
                    session, SessionProvider.AUTH_INSTANT);
            if (values != null && values.length != 0 &&
                    values[0] != null && values[0].length() != 0) {
                authInstant = DateUtils.stringToDate(values[0]);
            }
        } catch (Exception e) {
            logger.error(classMethod +
                    "exception retrieving info from the session: ", e);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
        }
        if (authInstant == null) {
            authInstant = newDate();
        }
        authnStatement.setAuthnInstant(authInstant);

        final Response idpResponse = (Response) request.getAttribute(SAML2Constants.SAML_PROXY_IDP_RESPONSE_KEY);

        AuthnContext authnContext = matchingAuthnContext;
        if (idpResponse == null) {
            if (authnContext == null) {
                String authLevel = getAuthLevel(session);

                IDPAuthnContextMapper idpAuthnContextMapper =
                        getIDPAuthnContextMapper(realm, idpEntityID);

                authnContext =
                        idpAuthnContextMapper.getAuthnContextFromAuthLevel(
                                authLevel, realm, idpEntityID);
            }
        } else {
            // IdP proxy case: we already received an assertion from the remote IdP and now the IdP proxy is generating
            // a new SAML response for the SP.
            logger.debug("{} SAML Response from IdP present - IdP-Proxy case - fetching AuthContext", classMethod);
            logger.debug("{} Using SAML response issued by {}", classMethod, idpResponse.getIssuer().getValue());
            Set<String> authenticatingAuthorities = new LinkedHashSet<String>();
            final List<Assertion> assertions = idpResponse.getAssertion();
            if (CollectionUtils.isNotEmpty(assertions)) {
                authnContext = getAuthContextFromAssertion(assertions.get(0));
                for (Assertion assertion : assertions) {
                    authenticatingAuthorities.addAll(extractAuthenticatingAuthorities(assertion));
                }
                // According to SAML profile 4.1.4.2 each assertion within the SAML Response MUST have the same issuer, so
                // this should suffice. We should have at least one assertion or encrypted assertion available since the 
                // IdP proxy's SP already accepted it.
                authenticatingAuthorities.add(assertions.iterator().next().getIssuer().getValue());
                authnContext.setAuthenticatingAuthority(new ArrayList<String>(authenticatingAuthorities));
            } else {
                // Check for Encrypted Assertions
                logger.debug("{} no Assertion present, check for encrypted Assertion", classMethod);
                final List<EncryptedAssertion> encryptedAssertions = idpResponse.getEncryptedAssertion();
                if (CollectionUtils.isNotEmpty(encryptedAssertions)) {
                    boolean firstAssertion = true;
                    List<Assertion> decryptedAssertions = decryptAssertions(metaAlias, realm ,encryptedAssertions);
                    if (CollectionUtils.isNotEmpty(decryptedAssertions)) {
                        authnContext = getAuthContextFromAssertion(decryptedAssertions.get(0));
                    }
                    for (Assertion assertion : decryptedAssertions) {
                        authenticatingAuthorities.addAll(extractAuthenticatingAuthorities(assertion));
                        if (firstAssertion) {
                            authenticatingAuthorities.add(assertion.getIssuer().getValue());
                            firstAssertion = false;
                        }
                    }
                    authnContext.setAuthenticatingAuthority(new ArrayList<String>(authenticatingAuthorities));
                } else {
                    logger.debug("{} no Assertions present", classMethod);
                }
            }
        }
        authnStatement.setAuthnContext(authnContext);

        String sessionIndex = getSessionIndex(session);

        if (sessionIndex == null) { // new sessionIndex
            sessionIndex = SAML2Utils.generateIDWithServerID();
            try {
                String[] values = {sessionIndex};
                sessionProvider.setProperty(
                        session,
                        SAML2Constants.IDP_SESSION_INDEX,
                        values);
            } catch (SessionException e) {
                logger.error(classMethod +
                        "error setting session index into the session: ", e);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
            }
            isNewSessionIndex.setValue(true);
        } else {
            isNewSessionIndex.setValue(false);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod +
                    "SessionIndex (in AuthnStatement) =" + sessionIndex);
        }
        if (sessionIndex != null) {
            Set authContextSet = (HashSet)
                    IDPCache.authnContextCache.get(sessionIndex);
            if (authContextSet == null || authContextSet.isEmpty()) {
                authContextSet = new HashSet();
            }
            authContextSet.add(authnContext);
            // cache the AuthContext to use in the case of session upgrade.
            IDPCache.authnContextCache.put(sessionIndex, authContextSet);
            authnStatement.setSessionIndex(sessionIndex);
        }
        return authnStatement;
    }

    /**
     * Returns a <code>SAML AttributeStatement</code> object
     *
     * @param session           the user's session
     * @param idpEntityID       the entity id of the identity provider
     * @param recipientEntityID the entity id of the response recipient
     * @param realm             the realm name
     * @return the <code>SAML AttributeStatement</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static AttributeStatement getAttributeStatement(
            Object session,
            String idpEntityID,
            String recipientEntityID,
            String realm)
            throws SAML2Exception {

        IDPAttributeMapper idpAttrMapper =
                getIDPAttributeMapper(realm, idpEntityID);

        List attributes = idpAttrMapper.getAttributes(
                session, idpEntityID, recipientEntityID, realm);

        if ((attributes == null) || (attributes.isEmpty())) {
            return null;
        }

        AttributeStatement attrStatement =
                AssertionFactory.getInstance().createAttributeStatement();

        attrStatement.setAttribute(attributes);
        return attrStatement;
    }

    /**
     * Returns an <code>IDPAttributeMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPAttributeMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    static IDPAttributeMapper getIDPAttributeMapper(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getIDPAttributeMapper: ";
        String idpAttributeMapperName = null;
        IDPAttributeMapper idpAttributeMapper = null;
        try {
            idpAttributeMapperName = getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID, SAML2Constants.IDP_ATTRIBUTE_MAPPER);
            if (idpAttributeMapperName == null) {
                idpAttributeMapperName =
                        SAML2Constants.DEFAULT_IDP_ATTRIBUTE_MAPPER_CLASS;
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod + "use " +
                            SAML2Constants.DEFAULT_IDP_ATTRIBUTE_MAPPER_CLASS);
                }
            }
            idpAttributeMapper = (IDPAttributeMapper)
                    IDPCache.idpAttributeMapperCache.get(
                            idpAttributeMapperName);
            if (idpAttributeMapper == null) {
                idpAttributeMapper = (IDPAttributeMapper)
                        Class.forName(idpAttributeMapperName).newInstance();
                IDPCache.idpAttributeMapperCache.put(
                        idpAttributeMapperName, idpAttributeMapper);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                            "got the IDPAttributeMapper from cache");
                }
            }
        } catch (Exception ex) {
            logger.error(classMethod +
                    "Unable to get IDP Attribute Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAttributeMapper;
    }


    /**
     * Returns an <code>IDPAuthnContextMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPAuthnContextMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static IDPAuthnContextMapper getIDPAuthnContextMapper(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getIDPAuthnContextMapper: ";
        String idpAuthnContextMapperName = null;
        IDPAuthnContextMapper idpAuthnContextMapper = null;
        try {
            idpAuthnContextMapperName = getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID,
                    SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS);
            if (idpAuthnContextMapperName == null) {
                idpAuthnContextMapperName =
                        SAML2Constants.DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS;
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod + "use " +
                            SAML2Constants.DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS);
                }
            }
            idpAuthnContextMapper = (IDPAuthnContextMapper)
                    IDPCache.idpAuthnContextMapperCache.get(
                            idpAuthnContextMapperName);
            if (idpAuthnContextMapper == null) {
                idpAuthnContextMapper = (IDPAuthnContextMapper)
                        Class.forName(idpAuthnContextMapperName).newInstance();
                IDPCache.idpAuthnContextMapperCache.put(
                        idpAuthnContextMapperName, idpAuthnContextMapper);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                            "got the IDPAuthnContextMapper from cache");
                }
            }
        } catch (Exception ex) {
            logger.error(classMethod +
                    "Unable to get IDP AuthnContext Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAuthnContextMapper;
    }

    /**
     * Returns an <code>IDPECPSessionMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPECPSessionMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static IDPECPSessionMapper getIDPECPSessionMapper(String realm,
                                                      String idpEntityID) throws SAML2Exception {

        String idpECPSessionMapperName = null;
        IDPECPSessionMapper idpECPSessionMapper = null;
        try {
            idpECPSessionMapperName = getAttributeValueFromIDPSSOConfig(realm,
                    idpEntityID, SAML2Constants.IDP_ECP_SESSION_MAPPER_CLASS);
            if (idpECPSessionMapperName == null) {
                idpECPSessionMapperName =
                        SAML2Constants.DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS;
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "IDPSSOUtil.getIDPECPSessionMapper: use " +
                                    SAML2Constants.DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS);
                }
            }
            idpECPSessionMapper = (IDPECPSessionMapper)
                    IDPCache.idpECPSessionMapperCache.get(
                            idpECPSessionMapperName);
            if (idpECPSessionMapper == null) {
                idpECPSessionMapper = (IDPECPSessionMapper)
                        Class.forName(idpECPSessionMapperName).newInstance();
                IDPCache.idpECPSessionMapperCache.put(
                        idpECPSessionMapperName, idpECPSessionMapper);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "IDPSSOUtil.getIDPECPSessionMapper: " +
                                    "got the IDPECPSessionMapper from cache");
                }
            }
        } catch (Exception ex) {
            logger.error("IDPSSOUtil.getIDPECPSessionMapper: " +
                    "Unable to get IDPECPSessionMapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpECPSessionMapper;
    }

    /**
     * Returns a <code>SAML Subject</code> object
     *
     * @param session           the user's session
     * @param authnReq          the <code>AuthnRequest</code> object
     * @param acsURL            the <code>ACS</code> service <code>url</code>
     * @param nameIDFormat      the <code>NameIDFormat</code>
     * @param realm             The realm name
     * @param idpEntityID       the entity id of the identity provider
     * @param recipientEntityID the entity id of the response recipient
     * @param effectiveTime     the effective time of the assertion
     * @param affiliationID     affiliationID for IDP initiated SSO
     * @return the <code>SAML Subject</code> object
     * @throws SAML2Exception if the operation is not successful
     */

    private static Subject getSubject(Object session,
                                      AuthnRequest authnReq,
                                      String acsURL,
                                      String nameIDFormat,
                                      String realm,
                                      String idpEntityID,
                                      String recipientEntityID,
                                      int effectiveTime,
                                      String affiliationID)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getSubject: ";

        Subject subject = AssertionFactory.getInstance().createSubject();
        boolean ignoreProfile = false;
        String userName = null;
        try {
            userName = sessionProvider.getPrincipalName(session);
            ignoreProfile = SAML2Utils.isIgnoreProfileSet(session);
        } catch (SessionException se) {
            logger.error(classMethod +
                    "There was a problem with the session.", se);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidSSOToken"));
        }
        boolean allowCreate = true; // allow create is the default
        String remoteEntityID = null;
        String spNameQualifier = null;
        boolean isAffiliation = false;
        if (authnReq != null) {
            remoteEntityID = authnReq.getIssuer().getValue();
            NameIDPolicy nameIDPolicy = authnReq.getNameIDPolicy();
            boolean hasNameIDPolicy = nameIDPolicy != null &&
                    (StringUtils.isNotEmpty(nameIDPolicy.getSPNameQualifier())
                    || StringUtils.isNotEmpty(nameIDPolicy.getFormat()));

            if (hasNameIDPolicy) {
                // this will take care of affiliation
                allowCreate = nameIDPolicy.isAllowCreate();
                spNameQualifier = nameIDPolicy.getSPNameQualifier();
                if (spNameQualifier != null && !spNameQualifier.isEmpty()) {
                    AffiliationDescriptorType affiDesc = metaManager.getAffiliationDescriptor(realm, spNameQualifier);

                    if (affiDesc != null) {
                        if (affiDesc.getAffiliateMember().contains(remoteEntityID)) {
                            isAffiliation = true;
                            remoteEntityID = spNameQualifier;
                        } else {
                            throw new SAML2Exception(SAML2Utils.bundle.getString("spNotAffiliationMember"));
                        }
                    }
                } else {
                    spNameQualifier = recipientEntityID;
                }
            } else {
                spNameQualifier = recipientEntityID;
            }
        } else {
            // IDP initiated SSO
            if (affiliationID != null) {
                AffiliationDescriptorType affiDesc = metaManager.
                        getAffiliationDescriptor(realm, affiliationID);
                if (affiDesc == null) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "affiliationNotFound"));
                }
                if (affiDesc.getAffiliateMember().contains(recipientEntityID)) {
                    isAffiliation = true;
                    remoteEntityID = affiliationID;
                    spNameQualifier = affiliationID;
                } else {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "spNotAffiliationMember"));
                }
            } else {
                remoteEntityID = recipientEntityID;
                spNameQualifier = recipientEntityID;
            }
        }
        SPSSODescriptorType spsso = getSPSSODescriptor(
                realm, recipientEntityID, classMethod);
        if (spsso == null) {
            String[] data = {recipientEntityID};
            LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "metaDataError"));
        }

        IDPSSODescriptorType idpsso =
                metaManager.getIDPSSODescriptor(realm, idpEntityID);
        if (idpsso == null) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "metaDataError"));
        }

        nameIDFormat = SAML2Utils.verifyNameIDFormat(nameIDFormat, spsso, idpsso);
        boolean isTransient = SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameIDFormat);

        NameIDInfo nameIDInfo;
        NameID nameID = null;
        IDPAccountMapper idpAccountMapper = SAML2Utils.getIDPAccountMapper(realm, idpEntityID);

        //Use-cases for NameID persistence:
        //* transient NameID -> The NameID MUST NOT be stored
        //* ignored user profile mode -> The NameID CANNOT be stored
        //* for any other cases -> The NameID MAY be stored based on customizable logic
        boolean shouldPersistNameID = !isTransient && !ignoreProfile
                && idpAccountMapper.shouldPersistNameIDFormat(realm, idpEntityID, remoteEntityID, nameIDFormat);

        if (!isTransient) {
            String userID;
            try {
                userID = sessionProvider.getPrincipalName(session);
            } catch (SessionException se) {
                logger.error(classMethod + "Unable to get principal name from the session.", se);
                throw new SAML2Exception(SAML2Utils.bundle.getString("invalidSSOToken"));
            }

            if (shouldPersistNameID) {
                nameIDInfo = AccountUtils.getAccountFederation(userID, idpEntityID, remoteEntityID);

                if (nameIDInfo != null) {
                    nameID = nameIDInfo.getNameID();

                    if (!nameIDFormat.equals(nameID.getFormat())) {
                        AccountUtils.removeAccountFederation(nameIDInfo, userID);
                        DoManageNameID.removeIDPFedSession(remoteEntityID, nameID.getValue());
                        nameID = null;
                    }
                }
            }
        }

        if (nameID == null) {
            if (!allowCreate && shouldPersistNameID) {
                throw new SAML2InvalidNameIDPolicyException(SAML2Utils.bundle.getString("cannotCreateNameID"));
            }

            nameID = idpAccountMapper.getNameID(session, idpEntityID, spNameQualifier, realm, nameIDFormat);

            logger.debug(classMethod + " shouldPersistNameID = " + shouldPersistNameID);
            if (shouldPersistNameID && allowCreate) {
                // write federation info into the persistent datastore
                if (SAML2Utils.isDualRole(idpEntityID, realm)) {
                    nameIDInfo = new NameIDInfo(idpEntityID, remoteEntityID, nameID, SAML2Constants.DUAL_ROLE, false);
                } else {
                    nameIDInfo = new NameIDInfo(idpEntityID, remoteEntityID, nameID, SAML2Constants.IDP_ROLE,
                            isAffiliation);
                }
                AccountUtils.setAccountFederation(nameIDInfo, userName);
            }
        }

        subject.setNameID(nameID);

        if (isTransient) {
            IDPCache.userIDByTransientNameIDValue.put(nameID.getValue(),
                    userName);
        }

        String inResponseTo = null;
        if (authnReq != null) {
            inResponseTo = authnReq.getID();
        }
        SubjectConfirmation sc = getSubjectConfirmation(
                inResponseTo, acsURL, effectiveTime);
        if (sc == null) {
            logger.error(classMethod +
                    "Unable to get subject confirmation");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("noSubjectConfirmation"));
        }

        List list = new ArrayList();
        list.add(sc);
        subject.setSubjectConfirmation(list);

        return subject;
    }


    /**
     * Returns a <code>SAML SubjectConfirmation</code> object
     *
     * @param inResponseTo  the request id of the <code>AuthnRequest</code>
     * @param acsURL        the <code>ACS</code> service <code>url</code>
     * @param effectiveTime the effective time of the assertion
     * @return the <code>SAML SubjectConfirmation</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static SubjectConfirmation getSubjectConfirmation(
            String inResponseTo, String acsURL, int effectiveTime)
            throws SAML2Exception {

        SubjectConfirmation sc = AssertionFactory.getInstance().
                createSubjectConfirmation();

        sc.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);

        SubjectConfirmationData scd = AssertionFactory.getInstance().
                createSubjectConfirmationData();

        scd.setRecipient(XMLUtils.escapeSpecialCharacters(acsURL));

        if (inResponseTo != null) {
            scd.setInResponseTo(inResponseTo);
        }

        Date date = newDate();
        date.setTime(date.getTime() + effectiveTime * 1000);
        scd.setNotOnOrAfter(date);
        sc.setSubjectConfirmationData(scd);

        return sc;
    }


    /**
     * Returns a <code>SAML Conditions</code> object
     *
     * @param audienceEntityID the entity id of the audience
     * @param effectiveTime    the effective time of the assertion
     * @return the <code>SAML Conditions</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    protected static Conditions getConditions(String audienceEntityID,
            int notBeforeSkewTime, int effectiveTime, String realm) throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getConditions: ";

        SPSSOConfigElement spConfig = metaManager.getSPSSOConfig(realm, audienceEntityID);
        Conditions conditions = AssertionFactory.getInstance().createConditions();
        Date date = newDate();
        date.setTime(date.getTime() - notBeforeSkewTime * 1000);
        conditions.setNotBefore(date);

        date = newDate();
        date.setTime(date.getTime() + effectiveTime * 1000);
        conditions.setNotOnOrAfter(date);

        List list = new ArrayList();
        AudienceRestriction ar = getAudienceRestriction(audienceEntityID);
        if (ar == null) {
            logger.error(classMethod +
                    "Unable to get Audience Restriction");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("noAudienceRestriction"));
        }
        String audienceUri = SAML2Utils.getAttributeValueFromSPSSOConfig(spConfig, SAML2Constants.AUDIENCE_URI);
        if (audienceUri != null) {
            List<String> audienceList = ar.getAudience();
            audienceList.add(audienceUri);
            ar.setAudience(audienceList);
        }
        list.add(ar);

        conditions.setAudienceRestrictions(list);
        return conditions;
    }


    /**
     * Returns a <code>SAML AudienceRestriction</code> object
     *
     * @param audienceEntityID the entity id of the audience
     * @return the <code>SAML AudienceRestriction</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static AudienceRestriction getAudienceRestriction(
            String audienceEntityID) throws SAML2Exception {

        AudienceRestriction ar = AssertionFactory.getInstance().
                createAudienceRestriction();
        if (audienceEntityID != null) {
            List list = new ArrayList();
            list.add(audienceEntityID);
            ar.setAudience(list);
        }
        return ar;
    }


    /**
     * Returns the assertion consumer service <code>URL</code>
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @param authnRequest   the <code>AuthnRequest</code> object
     * @param request    the <code>HttpServletRequest</code> object
     * @param rBinding   the binding used to send back <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurl(String spEntityID,
            String realm,
            AuthnRequest authnRequest,
            HttpServletRequest request,
            StringBuffer rBinding)
            throws SAML2Exception {
        String acsURL = null;
        String acsBinding;
        Integer acsIndex = null;
        if (authnRequest != null) {
            acsURL = authnRequest.getAssertionConsumerServiceURL();
            acsBinding = authnRequest.getProtocolBinding();
            acsIndex = authnRequest.getAssertionConsumerServiceIndex();
        } else {
            acsBinding = request.getParameter(SAML2Constants.BINDING);
        }
        return getACSurl(spEntityID, realm, authnRequest, acsURL, acsBinding, acsIndex, rBinding);
    }

    /**
     * Returns the assertion consumer service <code>URL</code>.
     *
     * @param spEntityID The entity id of the service provider.
     * @param realm The realm name of the identity provider.
     * @param acsURL AssertionConsumerServiceURL in AuthnRequest.
     * @param binding ProtocolBinding in AuthnRequest.
     * @param index AssertionConsumerServiceIndex in AuthnRequest.
     * @param rBinding The binding used to send back <code>Response</code>.
     * @return The assertion consumer service <code>URL</code>.
     * @throws SAML2Exception if the operation is not successful.
     */
    public static String getACSurl(String spEntityID, String realm, AuthnRequest authnRequest, String acsURL,
            String binding, Integer index, StringBuffer rBinding) throws SAML2Exception {
        if (StringUtils.isNotBlank(binding) && !binding.startsWith(SAML2Constants.BINDING_PREFIX)) {
            // convert short format binding to long format
            binding = SAML2Constants.BINDING_PREFIX + binding;
        }
        if (StringUtils.isEmpty(acsURL)) {
            StringBuffer returnedBinding = new StringBuffer();
            if (StringUtils.isNotBlank(binding)) {
                acsURL = IDPSSOUtil.getACSurlFromMetaByBinding(spEntityID, realm, binding, returnedBinding);
            } else {
                int acsIndex;
                if (index == null) {
                    acsURL = getDefaultACSurl(spEntityID, realm, returnedBinding);
                } else {
                    acsIndex = index.intValue();
                    if (acsIndex < 0 || acsIndex > 65535) {
                        acsIndex = 0;
                    }
                    acsURL = IDPSSOUtil.getACSurlFromMetaByIndex(spEntityID, realm, acsIndex, returnedBinding);
                }
            }
            binding = returnedBinding.toString();
        } else {
            validateAcsUrl(authnRequest, acsURL, spEntityID, realm);
            if (StringUtils.isEmpty(binding)) {
                binding = getBindingForAcsUrl(spEntityID, realm, acsURL);
            }
        }
        rBinding.append(binding);
        return acsURL;
    }

    /**
     * Returns the default assertion consumer service url and binding
     * from the metadata.
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @return the assertion consumer service url with returned binding.
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getDefaultACSurl(
            String spEntityID,
            String realm,
            StringBuffer returnedBinding) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getDefaultACSurl: ";
        SPSSODescriptorType spSSODescriptorElement = getSPSSODescriptor(
                realm, spEntityID, classMethod);
        List<IndexedEndpointType> acsList = spSSODescriptorElement.getAssertionConsumerService();
        IndexedEndpointType acs = null;
        String acsURL = null;
        String binding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = acsList.get(i);
            if (acs.isIsDefault()) {
                acsURL = acs.getLocation();
                binding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }

        if (acsURL == null) {
            acsURL = firstAcsURL;
            binding = firstBinding;
        }

        if (binding != null) {
            returnedBinding.append(binding);
        }
        return acsURL;
    }

    /**
     * Returns the assertion consumer service url binding from
     * the metadata.
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @return the assertion consumer service url binding
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getBindingForAcsUrl(
            String spEntityID,
            String realm,
            String acsURL) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getBindingForAcsUrl: ";
        SPSSODescriptorType spSSODescriptorElement = getSPSSODescriptor(
                realm, spEntityID, classMethod);
        List<IndexedEndpointType> acsList = spSSODescriptorElement.getAssertionConsumerService();
        IndexedEndpointType acs = null;
        String binding = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = acsList.get(i);
            String location = acs.getLocation();
            if (location != null && location.equals(acsURL)) {
                return acs.getBinding();
            }
        }
        return null;
    }

    /**
     * Returns the assertion consumer service <code>URL</code> from
     * meta data by binding
     *
     * @param spEntityID      the entity id of the service provider
     * @param realm           the realm name of the identity provider
     * @param desiredBinding  the desired binding
     * @param returnedBinding the binding used to send back
     *                        <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurlFromMetaByBinding(
            String spEntityID,
            String realm,
            String desiredBinding,
            StringBuffer returnedBinding)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getACSurlFromMetaByBinding: ";
        SPSSODescriptorType spSSODescriptorElement = getSPSSODescriptor(
                realm, spEntityID, classMethod);
        List<IndexedEndpointType> acsList = spSSODescriptorElement.getAssertionConsumerService();
        String acsURL = null;
        String binding = null;
        String defaultAcsURL = null;
        String defaultBinding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        IndexedEndpointType acs = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = acsList.get(i);

            binding = acs.getBinding();
            if (binding.equals(desiredBinding)) {
                acsURL = acs.getLocation();
                break;
            }
            if (acs.isIsDefault()) {
                defaultAcsURL = acs.getLocation();
                defaultBinding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }
        if (acsURL == null || acsURL.length() == 0) {
            acsURL = defaultAcsURL;
            if (acsURL == null || acsURL.length() == 0) {
                acsURL = firstAcsURL;
                if (acsURL == null || acsURL.length() == 0) {
                    acsURL = null;
                    logger.error(classMethod +
                            "Unable to get valid Assertion " +
                            "Consumer Service URL");
                    return null;
                }
                returnedBinding.append(firstBinding);
            } else {
                returnedBinding.append(defaultBinding);
            }
        } else {
            returnedBinding.append(binding);
        }
        return acsURL;
    }


    /**
     * Returns the assertion consumer service <code>URL</code> from
     * meta data by binding
     *
     * @param spEntityID      the entity id of the service provider
     * @param realm           the realm name of the identity provider
     * @param acsIndex        the <code>ACS</code> index
     * @param returnedBinding the binding used to send back
     *                        <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurlFromMetaByIndex(
            String spEntityID,
            String realm,
            int acsIndex,
            StringBuffer returnedBinding)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getACSurlFromMetaByIndex: ";

        SPSSODescriptorType spSSODescriptorElement = getSPSSODescriptor(
                realm, spEntityID, classMethod);
        List<IndexedEndpointType> acsList = spSSODescriptorElement.getAssertionConsumerService();
        int index;
        String acsURL = null;
        String binding = null;
        String defaultAcsURL = null;
        String defaultBinding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        IndexedEndpointType acs = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = acsList.get(i);

            index = acs.getIndex();
            binding = acs.getBinding();
            if (index == acsIndex) {
                acsURL = acs.getLocation();
                binding = acs.getBinding();
                break;
            }
            if (acs.isIsDefault()) {
                defaultAcsURL = acs.getLocation();
                defaultBinding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }
        if (acsURL == null || acsURL.length() == 0) {
            acsURL = defaultAcsURL;
            if (acsURL == null || acsURL.length() == 0) {
                acsURL = firstAcsURL;
                if (acsURL == null || acsURL.length() == 0) {
                    acsURL = null;
                    logger.error(classMethod +
                            "Unable to get valid Assertion " +
                            "Consumer Service URL");
                    return null;
                }
                returnedBinding.append(firstBinding);
            } else {
                returnedBinding.append(defaultBinding);
            }
        } else {
            returnedBinding.append(binding);
        }
        return acsURL;
    }

    /**
     * This method opens a URL connection to the target specified and
     * sends artifact response to it using the
     * <code>HttpServletResponse</code> object.
     *
     * @param response    the <code>HttpServletResponse</code> object
     * @param idpEntityID the entity id of the identity provider
     * @param realm       the realm name of the identity provider
     * @param acsURL      the assertion consumer service <code>URL</code>
     * @param relayState  the value of the <code>RelayState</code>
     * @param res         the <code>SAML Response</code> object
     * @param session     user session
     * @param props       property map including nameIDString for logging
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponseArtifact(HttpServletRequest request,
                                            HttpServletResponse response,
                                            String idpEntityID, String spEntityID, String realm, String acsURL,
                                            String relayState, Response res, Object session, Map props)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponseArtifact: ";

        IDPSSODescriptorType idpSSODescriptorElement = null;
        try {
            idpSSODescriptorElement = metaManager.getIDPSSODescriptor(
                    realm, idpEntityID);
            if (idpSSODescriptorElement == null) {
                logger.error(classMethod
                        + "Unable to get IDP SSO Descriptor from meta.");
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.IDP_METADATA_ERROR, data, session, props);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            logger.error(classMethod
                    + "Unable to get IDP SSO Descriptor from meta.");
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session, props);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        IndexedEndpointType ars =
                idpSSODescriptorElement.getArtifactResolutionService().get(0);
        if (ars == null) {
            logger.error(classMethod +
                    "Unable to get ArtifactResolutionServiceElement from meta.");
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session, props);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        Artifact art = null;
        try {
            art = ProtocolFactory.getInstance().createArtifact(
                    null,
                    ars.getIndex(),
                    SAML2Utils.generateSourceID(idpEntityID),
                    SAML2Utils.generateMessageHandleWithServerID()
            );
        } catch (SAML2Exception se) {
            logger.error(classMethod +
                    "Unable to create artifact: ", se);
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.CANNOT_CREATE_ARTIFACT, data, session, props);
            SAMLUtils.sendError(request, response,
                    response.SC_INTERNAL_SERVER_ERROR, "errorCreateArtifact",
                    SAML2Utils.bundle.getString("errorCreateArtifact"));
            return;
        }
        String artStr = art.getArtifactValue();
        try {
            IDPCache.responsesByArtifacts.put(artStr, res);
            try {
                long expireTime = getValidTimeofResponse(realm, idpEntityID, res) / 1000;
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(artStr,
                        res, expireTime);
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod + "Saved Response to SAML2 Token Repository using key "
                            + artStr);
                }
            } catch (SAML2TokenRepositoryException se) {
                logger.error(classMethod + "Unable to save Response to the SAML2 Token Repository", se);
            }

            String messageEncoding = SAML2Utils.getAttributeValueFromSSOConfig(
                    realm, spEntityID, SP_ROLE,
                    SAML2Constants.RESPONSE_ARTIFACT_MESSAGE_ENCODING);

            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "messageEncoding = " + messageEncoding);
                logger.debug(classMethod +
                        "artStr = " + artStr);
            }

            if ((messageEncoding != null) &&
                    (messageEncoding.equals(SAML2Constants.FORM_ENCODING))) {

                String[] logdata = {idpEntityID, realm, acsURL};
                LogUtil.access(Level.INFO, LogUtil.SEND_ARTIFACT, logdata,
                        session, props);

                SAML2Utils.postToTarget(request, response, SAML2Constants.SAML_ART,
                        artStr, "RelayState", relayState, acsURL);
            } else {
                String redirectURL = acsURL +
                        (acsURL.contains("?") ? "&" : "?") + "SAMLart=" +
                        urlEncodeQueryParameterNameOrValue(artStr);
                if ((relayState != null) && (relayState.trim().length() != 0)) {
                    redirectURL += "&RelayState=" +
                            urlEncodeQueryParameterNameOrValue(relayState);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                            "Redirect URL = " + redirectURL);
                }

                String[] logdata = {idpEntityID, realm, redirectURL};
                LogUtil.access(Level.INFO, LogUtil.SEND_ARTIFACT, logdata,
                        session, props);
                response.sendRedirect(redirectURL);
            }
        } catch (IOException ioe) {
            logger.error(classMethod +
                    "Unable to send redirect: ", ioe);
        }
    }

    /**
     * This method sends SAML Response back to ECP.
     *
     * @param request The servlet request.
     * @param response The servlet response.
     * @param out The print writer for writing out presentation.
     * @param idpEntityID the entity id of the identity provider
     * @param realm       the realm name of the identity provider
     * @param acsURL      the assertion consumer service <code>URL</code>
     * @param res         the <code>SAML Response</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponseECP(HttpServletRequest request,
                                       HttpServletResponse response,
                                       PrintWriter out,
                                       String idpEntityID, String realm, String acsURL,
                                       Response res) throws SAML2Exception {

        ECPFactory ecpFactory = ECPFactory.getInstance();
        ECPResponse ecpResponse = ecpFactory.createECPResponse();
        ecpResponse.setMustUnderstand(Boolean.TRUE);
        ecpResponse.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
        ecpResponse.setAssertionConsumerServiceURL(acsURL);

        String header = ecpResponse.toXMLString(true, true);
        String body = res.toXMLString(true, true);

        try {
            SOAPMessage reply = SOAPCommunicator.getInstance().createSOAPMessage(header, body,
                    false);

            String[] logdata = {idpEntityID, realm, acsURL, ""};
            if (LogUtil.isAccessLoggable(Level.FINE)) {
                logdata[3] = SOAPCommunicator.getInstance().soapMessageToString(reply);
            }
            LogUtil.access(Level.INFO, LogUtil.SEND_ECP_RESPONSE, logdata,
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

            // Write out the message on the response stream
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            reply.writeTo(stream);
            out.println(stream.toString());
            out.flush();
        } catch (Exception ex) {
            logger.error("IDPSSOUtil.sendResponseECP", ex);
            String[] data = {idpEntityID, realm, acsURL};
            LogUtil.error(Level.INFO, LogUtil.SEND_ECP_RESPONSE_FAILED, data,
                    null);
            SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "failedToSendECPResponse", ex.getMessage());
            return;
        }
    }

    /**
     * Returns the session index of an <code>IDPSession</code>
     *
     * @param session the session corresponding to the <code>IDPSession</code>
     * @return the session index string
     */
    public static String getSessionIndex(Object session) {

        String classMethod = "IDPSSOUtil.getSessionIndex: ";
        if (session == null) {
            return null;
        }
        String[] values = null;
        try {
            values = sessionProvider.getProperty(
                    session, SAML2Constants.IDP_SESSION_INDEX);
        } catch (SessionException e) {
            logger.error(classMethod +
                    "error retrieving session index from the session: ", e);
            values = null;
        }
        if (values == null || values.length == 0) {
            return null;
        }
        String index = values[0];
        if (index == null || index.length() == 0) {
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(
                    classMethod + "Returning sessionIndex=" + index);
        }
        return index;
    }


    /**
     * Returns the authentication service <code>URL</code> of the
     * identity provider
     *
     * @param realm        the realm name of the identity provider
     * @param hostEntityId the entity id of the identity provider
     * @param request      the <code>HttpServletRequest</code> object
     * @return the authentication service <code>URL</code> of the
     *         identity provider
     */

    public static String getAuthenticationServiceURL(
            String realm,
            String hostEntityId,
            HttpServletRequest request) {
        String classMethod = "IDPSSOUtil.getAuthenticationServiceURL: ";
        String authUrl = getAttributeValueFromIDPSSOConfig(
                realm, hostEntityId, SAML2Constants.AUTH_URL);
        if ((authUrl == null) || (authUrl.trim().length() == 0)) {
            StringBuffer sb = new StringBuffer(100);
            sb.append(request.getScheme()).append("://")
                    .append(request.getServerName()).append(":")
                    .append(request.getServerPort())
                    .append(request.getContextPath())
                    .append("/UI/Login?realm=").append(realm);
            authUrl = sb.toString();
        }
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "auth url=:" + authUrl);
        }
        return authUrl;
    }

    public static String getAttributeValueFromIDPSSOConfig(
            String realm,
            String hostEntityId,
            String attrName) {
        String classMethod = "IDPSSOUtil.getAttributeValueFromIDPSSOConfig: ";
        String result = null;
        try {
            IDPSSOConfigElement config = metaManager.getIDPSSOConfig(
                    realm, hostEntityId);
            Map attrs = SAML2MetaUtils.getAttributes(config);
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = (String) value.get(0);
            }
        } catch (SAML2MetaException sme) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "get IDPSSOConfig failed:", sme);
            }
            result = null;
        }
        return result;
    }

    /**
     * Redirects to authenticate service
     *
     * @param request     the <code>HttpServletRequest</code> object
     * @param response    the <code>HttpServletResponse</code> object
     * @param authnReq    the <code>AuthnRequest</code> object
     * @param reqID       the <code>AuthnRequest ID</code>
     * @param realm       the realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param spEntityID  the entity id of the service provider
     */
    static void redirectAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnReq,
            String reqID,
            String realm,
            String idpEntityID,
            String spEntityID)
            throws SAML2Exception, IOException {
        // get the authentication service url
        StringBuilder newURL = new StringBuilder(
                IDPSSOUtil.getAuthenticationServiceURL(
                        realm, idpEntityID, request));
        newURL = generateNewUrl(request, authnReq, reqID, realm, idpEntityID, spEntityID, newURL);
        response.sendRedirect(newURL.toString());

        return;
    }

    /**
     * Redirects to authenticate service specyfing a ForceAuthn flag
     *
     * @param request     the <code>HttpServletRequest</code> object
     * @param response    the <code>HttpServletResponse</code> object
     * @param authnReq    the <code>AuthnRequest</code> object
     * @param reqID       the <code>AuthnRequest ID</code>
     * @param realm       the realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param spEntityID  the entity id of the service provider
     * @param isForceAuthn true if this is ForceAuthn
     */
    public static void redirectAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnReq,
            String reqID,
            String realm,
            String idpEntityID,
            String spEntityID,
            boolean isForceAuthn)
            throws SAML2Exception, IOException {
        StringBuilder newURL = new StringBuilder(
                IDPSSOUtil.getAuthenticationServiceURL(
                        realm, idpEntityID, request));
        if (isForceAuthn) {
            if (newURL.indexOf("?") == -1) {
                newURL.append("?ForceAuth=true");
            } else {
                newURL.append("&ForceAuth=true");
            }
        }
        newURL = generateNewUrl(request, authnReq, reqID, realm, idpEntityID, spEntityID, newURL);
        response.sendRedirect(newURL.toString());
        return;
    }

    /**
     * Generates a new redirect url
     *
     * @param request     the <code>HttpServletRequest</code> object
     * @param authnReq    the <code>AuthnRequest</code> object
     * @param reqID       the <code>AuthnRequest ID</code>
     * @param realm       the realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param spEntityID  the entity id of the service provider
     * @param newURL      the new url to append to
     * @return A new redirect URL
     * @throws SAML2Exception
     */
    private static StringBuilder generateNewUrl(
            HttpServletRequest request, AuthnRequest authnReq, String reqID,
            String realm, String idpEntityID, String spEntityID,
            StringBuilder newURL) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.generateNewUrl: ";
        // Pass spEntityID to IdP Auth Module
        if (spEntityID != null) {
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(SAML2Constants.SPENTITYID);
            newURL.append("=");
            newURL.append(urlEncodeQueryParameterNameOrValue(spEntityID));
        }

        // find out the authentication method, e.g. module=LDAP, from
        // authn context mapping
        IDPAuthnContextMapper idpAuthnContextMapper =
                getIDPAuthnContextMapper(realm, idpEntityID);

        IDPAuthnContextInfo info =
                idpAuthnContextMapper.getIDPAuthnContextInfo(
                        authnReq, idpEntityID, realm);
        Set authnTypeAndValues = info.getAuthnTypeAndValues();
        if ((authnTypeAndValues != null)
                && (!authnTypeAndValues.isEmpty())) {
            Iterator iter = authnTypeAndValues.iterator();
            StringBuffer authSB = new StringBuffer((String) iter.next());
            while (iter.hasNext()) {
                authSB.append("&");
                authSB.append((String) iter.next());
            }
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(authSB.toString());
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "authString=" + authSB.toString());
            }
        }
        if (newURL.indexOf("?") == -1) {
            newURL.append("?goto=");
        } else {
            newURL.append("&goto=");
        }

        String gotoURL = request.getRequestURL().toString();
        String gotoQuery = request.getQueryString();

        //We are appending redirected=true to the goto URL so that we can tell if the user was already redirected
        //to the login interface for authentication.
        if (gotoQuery != null) {
            gotoURL += "?" + gotoQuery + "&" + REDIRECTED_TRUE;
        } else {
            gotoURL += "?" + REDIRECTED_TRUE;
        }
        if (reqID != null) {
            gotoURL += "&ReqID=" + reqID;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(classMethod +
                    "gotoURL=" + gotoURL);
        }

        newURL.append(urlEncodeQueryParameterNameOrValue(gotoURL));
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod +
                    "New URL for authentication: " + newURL.toString());
        }
        return newURL;
    }

    /**
     * Signs an <code>Assertion</code>
     *
     * @param realm The realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param assertion The <code>Assertion</code> to be signed
     * @param spEntityId The remote SP's entity ID.
     */
    static void signAssertion(String realm, String idpEntityID, Assertion assertion, String spEntityId)
            throws SAML2Exception {
        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, idpEntityID, IDP);
        Key signingKey = credentials.getSigningKey();
        X509Certificate signingCert = credentials.getSigningCertificate();
        assertion.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey, signingCert,
                        metaManager.getEntityDescriptor(realm, spEntityId), Saml2EntityRole.SP));
    }

    /**
     * Signs and encrypts the components of a <code>SAML Response</code>
     * based on the service provider meta data. If the flag of
     * encrypting <code>Assertion</code> is on, then the embedded
     * <code>Assertion</code> object will be encrypted; if the flag
     * of encrypting <code>Assertion</code> is off and the flag of
     * encrypting <code>NameID</code> is on, then the <code>NameID</code>
     * embedded in the <code>Assertion</code> will be encrypted; if the
     * flag of encrypting <code>Assertion</code> is off and the flag of
     * encrypting <code>Attribute</code> is on, then the
     * <code>Attribute</code> embedded in the <code>Assertion</code>
     * will be encrypted. If the flag signAssertion is on, then the
     * <code>Assertion</code> will be signed. It will be signed before
     * it is encrypted and after its embedded <code>NameID</code> or
     * <code>Attribute</code> is encrypted.
     *
     * @param realm         the realm name of the identity provider
     * @param spEntityID    the entity id of the service provider
     * @param idpEntityID   the entity id of the identity provider
     * @param res           The <code>Response</code> whose components may be
     *                      encrypted based on the service provider meta data setting
     * @param signAssertion A flag to indicate if <code>Assertion</code>
     *                      signing is required
     */
    static void signAndEncryptResponseComponents(String realm, String spEntityID, String idpEntityID, Response res,
            boolean signAssertion) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.signAndEncryptResponseComponents: ";
        boolean toEncryptAssertion;
        boolean toEncryptNameID = false;
        boolean toEncryptAttribute = false;

        if (res == null) {
            return;
        }

        List<Assertion> assertions = res.getAssertion();
        if (isEmpty(assertions)) {
            return;
        }

        Assertion assertion = assertions.get(0);

        // get the encryption related flags from the SP Entity Config
        toEncryptAssertion = getBooleanAttributeValueFromSSOConfig(realm, spEntityID, SP_ROLE,
                SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        if (!toEncryptAssertion) {
            toEncryptNameID = getBooleanAttributeValueFromSSOConfig(realm, spEntityID, SP_ROLE,
                    SAML2Constants.WANT_NAMEID_ENCRYPTED);
            toEncryptAttribute = getBooleanAttributeValueFromSSOConfig(realm, spEntityID, SP_ROLE,
                    SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED);
        }

        if (!toEncryptAssertion && !toEncryptNameID && !toEncryptAttribute) {
            // all encryption flags are off, no encryption needed
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion, spEntityID);
                List<Assertion> assertionList = new ArrayList<>();
                assertionList.add(assertion);
                res.setAssertion(assertionList);
            }
            return;
        }
        SPSSODescriptorType spSSODescriptorElement = getSPSSODescriptor(
                realm, spEntityID, classMethod);
        // get the encryption information
        EncryptionConfig encryptionConfig = KeyUtil.getEncryptionConfig(spSSODescriptorElement, spEntityID, SP_ROLE, realm);
        if (encryptionConfig == null) {
            logger.error("failed to get service provider encryption key info.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("UnableToFindEncryptKeyInfo"));
        }
        if (toEncryptAssertion) {
            // sign assertion first, then encrypt the assertion
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion, spEntityID);
            }
            // we only encrypt the Assertion
            EncryptedAssertion encryptedAssertion = assertion.encrypt(encryptionConfig, spEntityID);
            if (encryptedAssertion == null) {
                logger.error("failed to encrypt the assertion.");
                throw new SAML2Exception(SAML2Utils.bundle.getString("FailedToEncryptAssertion"));
            }
            List<EncryptedAssertion> assertionList = new ArrayList<>();
            assertionList.add(encryptedAssertion);
            res.setEncryptedAssertion(assertionList);
            res.setAssertion(new ArrayList<>()); // reset assertion list
            logger.debug("Assertion encrypted.");
        } else {
            // we only encrypt NameID and/or Attribute.
            // encrypt NameID and/or Attribute first, then sign the 
            // assertion if applicable
            if (toEncryptNameID) {
                // we need to encrypt the NameID            
                Subject subject = assertion.getSubject();
                if (subject == null) {
                    return;
                }
                NameID nameID = subject.getNameID();
                if (nameID == null) {
                    return;
                }
                EncryptedID encryptedNameID = nameID.encrypt(encryptionConfig, spEntityID);
                if (encryptedNameID == null) {
                    logger.error("failed to encrypt the NameID.");
                    throw new SAML2Exception(SAML2Utils.bundle.getString("FailedToEncryptNameID"));
                }
                subject.setEncryptedID(encryptedNameID);
                subject.setNameID(null); // reset NameID
                assertion.setSubject(subject);
                logger.debug("NameID encrypted.");
            }

            if (toEncryptAttribute) {
                // we need to encrypt the Attribute
                List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
                if (CollectionUtils.isNotEmpty(attributeStatements)) {
                    // to hold all the AttributeStatements
                    List<AttributeStatement> stmts = new ArrayList<>();
                    for (AttributeStatement attributeStatement : attributeStatements) {
                        List<Attribute> attributes = attributeStatement.getAttribute();
                        if (isEmpty(attributes)) {
                            continue;
                        }
                        // holds all the encrypted Attributes in this statement
                        List<EncryptedAttribute> eaList = new ArrayList<>();
                        for (Attribute attribute : attributes) {
                            EncryptedAttribute encryptedAttribute = attribute.encrypt(encryptionConfig, spEntityID);
                            if (encryptedAttribute == null) {
                                logger.error("failed to encrypt the Attribute.");
                                throw new SAML2Exception(SAML2Utils.bundle.getString("FailedToEncryptAttribute"));
                            }
                            eaList.add(encryptedAttribute);
                        }
                        attributeStatement.setEncryptedAttribute(eaList);
                        attributeStatement.setAttribute(new ArrayList<>());
                        stmts.add(attributeStatement);
                    }
                    assertion.setAttributeStatements(stmts);
                    logger.debug("Attribute encrypted.");
                }
            }
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion, spEntityID);
            }
            List<Assertion> assertionList = new ArrayList<>();
            assertionList.add(assertion);
            res.setAssertion(assertionList);
        }
    }

    private static String getWriterURL(String realm,
                                       String idpEntityID,
                                       String spEntityID) {

        String classMethod = "IDPSSOUtil.getWriterURL: ";
        String writerURL = null;
        try {
            // get cot list of the idp
            IDPSSOConfigElement idpEntityCfg =
                    metaManager.getIDPSSOConfig(realm, idpEntityID);
            Map idpConfigAttrsMap = null;
            if (idpEntityCfg != null) {
                idpConfigAttrsMap = SAML2MetaUtils.getAttributes(idpEntityCfg);
            }
            if (isEmpty(idpConfigAttrsMap)) {
                return null;
            }
            List idpCOTList =
                    (List) idpConfigAttrsMap.get(SAML2Constants.COT_LIST);
            if (isEmpty(idpCOTList)) {
                return null;
            }

            // get cot list of the sp
            SPSSOConfigElement spEntityCfg =
                    metaManager.getSPSSOConfig(realm, spEntityID);
            Map spConfigAttrsMap = null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }
            if (isEmpty(spConfigAttrsMap)) {
                return null;
            }
            List spCOTList = (List) spConfigAttrsMap.get(SAML2Constants.COT_LIST);
            if (isEmpty(spCOTList)) {
                return null;
            }

            List<String> idpCOTListCopy = new ArrayList<>(idpCOTList);
            idpCOTListCopy.retainAll(spCOTList);
            for (String cotName : idpCOTListCopy) {
                CircleOfTrustDescriptor cotDescriptor =
                        cotManager.getCircleOfTrust(realm, cotName);
                writerURL = cotDescriptor.getSAML2WriterServiceURL();
                if (StringUtils.isNotEmpty(writerURL)) {
                    break;
                }
            }
        } catch (COTException ce) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "Error retreiving of "
                        + "circle of trust", ce);
            }
        } catch (SAML2Exception se) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "Not able to getting writer URL : ", se);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod +
                        "Not able to getting writer URL : ", e);
            }
        }
        return writerURL;
    }

    /**
     * Returns the effective time from the IDP
     * extended metadata . If the attreibute is not
     * defined in the metadata then defaults to
     * a value of 600 seconds (5 minutes).
     *
     * @return the effective time value in seconds.
     */
    protected static int getEffectiveTime(String realm, String idpEntityID) {
        int effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
        String effectiveTimeStr = getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID,
                SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE);
        if (effectiveTimeStr != null) {
            try {
                effectiveTime = Integer.parseInt(effectiveTimeStr);
                if (logger.isDebugEnabled()) {
                    logger.debug("IDPSSOUtil.getEffectiveTime: " +
                            "got effective time from config:" + effectiveTime);
                }
            } catch (NumberFormatException nfe) {
                logger.error("IDPSSOUtil.getEffectiveTime: " +
                        "Failed to get assertion effective time from " +
                        "IDP SSO config: ", nfe);
                effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
            }
        }
        return effectiveTime;
    }

    /**
     * Returns the NotBefore skew time from the IDP
     * extended metadata . If the attreibute is not
     * defined in the metadata then defaults to
     * a value of 600 seconds (5 minutes).
     *
     * @return the NotBefore skew value in seconds.
     */
    protected static int getNotBeforeSkewTime(String realm,
                                              String idpEntityID) {
        String classMethod = "IDPSSOUtil.getNotBeforeSkewTime:";
        int notBeforeSkewTime =
                SAML2Constants.NOTBEFORE_ASSERTION_SKEW_DEFAULT;
        // get the assertion effective time (in seconds)
        String skewTimeStr = getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID,
                SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE);
        if (skewTimeStr != null) {
            try {
                notBeforeSkewTime = Integer.parseInt(skewTimeStr);
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                            "got NotBefore skew time from config:"
                            + notBeforeSkewTime);
                }
            } catch (NumberFormatException nfe) {
                logger.error(classMethod + "IDP SSO config: ", nfe);
                notBeforeSkewTime =
                        SAML2Constants.NOTBEFORE_ASSERTION_SKEW_DEFAULT;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "NotBefore Skew time :" +
                    notBeforeSkewTime);
        }
        return notBeforeSkewTime;
    }

    private static boolean assertionCacheEnabled(String realm,
                                                 String idpEntityID) {

        String enabled = SAML2Utils.getAttributeValueFromSSOConfig(realm,
                idpEntityID, SAML2Constants.IDP_ROLE,
                SAML2Constants.ASSERTION_CACHE_ENABLED);

        return "true".equalsIgnoreCase(enabled) ? true : false;
    }

    public static byte[] stringToByteArray(String input) {
        char chars[] = input.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    public static long getValidTimeofResponse(
            String realm, String idpEntityID, Response response)
            throws SAML2Exception {
        // in seconds
        int timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
        String timeskewStr = getAttributeValueFromIDPSSOConfig(
                realm,
                idpEntityID,
                SAML2Constants.ASSERTION_TIME_SKEW);
        if (timeskewStr != null && timeskewStr.trim().length() > 0) {
            timeskew = Integer.parseInt(timeskewStr);
            if (timeskew < 0) {
                timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("timeskew = " + timeskew);
        }

        List assertions = response.getAssertion();
        if ((assertions == null) || (assertions.size() == 0)) {
            // failed case
            return (currentTimeMillis()
                    + getEffectiveTime(realm, idpEntityID)
                    + timeskew * 1000);
        }

        Assertion assertion = (Assertion) assertions.get(0);
        Conditions cond = assertion.getConditions();
        if (cond == null) {
            throw new SAML2Exception("nullConditions");
        }
        Date notOnOrAfter = cond.getNotOnOrAfter();

        long ret = notOnOrAfter.getTime() + timeskew * 1000;
        if (notOnOrAfter == null ||
                (ret < currentTimeMillis())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Time in Assertion "
                        + " is invalid.");
            }
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidTimeOnResponse"));
        }
        return ret;
    }

    /**
     * Signs SAMLv2 Response.
     *
     * @param realm the realm name.
     * @param idpEntityID the identity provider entity identifier
     * @param spEntityId The remote SP's entity ID.
     * @param response the SAMLv2 <code>Response</code>
     * @throws SAML2Exception if there is an error signing the response.
     */
    private static void signResponse(String realm, String idpEntityID, String spEntityId, Response response)
            throws SAML2Exception {
        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, idpEntityID, IDP);
        Key signingKey = credentials.getSigningKey();
        X509Certificate signingCert = credentials.getSigningCertificate();
        response.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey, signingCert,
                        metaManager.getEntityDescriptor(realm, spEntityId), Saml2EntityRole.SP));
    }

    /**
     * Returns a <code>SAML2IdentityProviderAdapter</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>SAML2IdenityProviderAdapter</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static SAML2IdentityProviderAdapter getIDPAdapterClass(String realm, String idpEntityID)
            throws SAML2Exception {
        return SAML2Utils.getIDPAdapterClass(realm, idpEntityID);
    }

    /**
     * Validates if the Assertion Consumer Service URL acsURL exists in the metadata of the Service Provider.
     *
     * @param authnRequest The SAML Authentication Request. May be null.
     * @param acsURL The assertion consumer service <code>URL</code>.
     * @param spEntityID The entity id of the service provider.
     * @param realm The realm name of the identity provider.
     */
    private static void validateAcsUrl(AuthnRequest authnRequest, String acsURL, String spEntityID,
            String realm) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.validateAcsUrl: ";
        SPSSODescriptorType spDescriptor = getSPSSODescriptor(realm, spEntityID, classMethod);
        // We check authnRequest here to ensure that validation can be only skipped for SP initiated SSO.
        // The Authn Request Signed flag is checked here, because AuthnRequest#isSigned only tackles HTTP-POST request
        // binding and by the time this validation is called, the HTTP-Redirect binding's Signature parameter is already
        // lost. When this setting is on, the authentication request signature was definitely verified at an earlier
        // stage of request processing.
        if (authnRequest != null && spDescriptor.isAuthnRequestsSigned()) {
            boolean skipValidation = getBooleanAttributeValueFromSSOConfig(realm, spEntityID, SP_ROLE,
                    SAML2Constants.SKIP_ENDPOINT_VALIDATION_WHEN_SIGNED);
            if (skipValidation) {
                logger.debug("Skipping endpoint validation for SP {} in realm {}", spEntityID, realm);
                return;
            }
        }

        if (spDescriptor.getAssertionConsumerService().stream()
                .map(IndexedEndpointType::getLocation)
                .noneMatch(url -> url.equalsIgnoreCase(acsURL))) {
            throw new SAML2Exception("libSAML2", "invalidAssertionConsumerServiceURL", acsURL, spEntityID);
        }
    }
    
    /**
     * Returns the the value of the wantAssertionsSigned property
     * @param spEntityID ID of the SP entity to be retrieved.
     * @param realm The realm under which the entity resides.
     * @return boolean  value of the wantAssertionsSigned property.
     * @throws SAML2MetaException if unable to retrieve the service
     *         provider's SSO descriptor.
     */
    private static boolean wantAssertionsSigned(String realm, String spEntityID) throws SAML2Exception {
       
        String method = "IPDSSOUtil:wantAssertionsSigned : ";
        if (logger.isDebugEnabled()) {
            logger.debug(method + ": realm - " + realm + "/: spEntityID - " + spEntityID);
        }
        SPSSODescriptorType spSSODescriptor = getSPSSODescriptor(spEntityID, realm, method);
        return spSSODescriptor.isWantAssertionsSigned();
    }

    /**
     * Returns the service provider's SSO descriptor in an entity under the realm.
     * @param realm The realm under which the entity resides.
     * @param spEntityID ID of the SP entity to be retrieved.
     * @param classMethod the calling class method
     * @return <code>SPSSODescriptorElement</code> for the entity
     * @throws SAML2Exception if entity is not found
     */
    private static SPSSODescriptorType getSPSSODescriptor(String realm,
            String spEntityID, String classMethod)
            throws SAML2Exception {
       
        SPSSODescriptorType spSSODescriptor = null;
        if (metaManager == null) {
            logger.error(classMethod + "Unable to get meta manager.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("errorMetaManager"));
        }
        try {
            spSSODescriptor = metaManager.getSPSSODescriptor(realm, spEntityID);
            if (spSSODescriptor == null) {
                logger.error(classMethod
                        + "Unable to get SP SSO Descriptor from metadata, descriptor is null.");
                String[] data = { spEntityID };
                LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            logger.error(classMethod + "Unable to get SP SSO Descriptor from metadata, descriptor is null.");
            String[] data = { spEntityID };
            LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
        return spSSODescriptor;
    }

    /**
     * Check that the authenticated session belongs to the same realm where the IDP is defined.
     *
     * @param realm The realm where the IdP is defined.
     * @param session The Session object of the authenticated user.
     * @return true If the session was initiated in the same realm as the session's realm.
     */
    public static boolean isValidSessionInRealm(String realm, Object session) {
        String classMethod = "IDPSSOUtil.isValidSessionInRealm: ";
        boolean isValidSessionInRealm = false;
        try {
            // A user can only be authenticated in one realm
            String sessionRealm = SAML2Utils.getSingleValuedSessionProperty(session, SAML2Constants.ORGANIZATION);
            if (sessionRealm != null && !sessionRealm.isEmpty()) {
                if (realm.equalsIgnoreCase(sessionRealm)) {
                    isValidSessionInRealm = true;
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(classMethod + "Invalid realm for the session:" + sessionRealm
                                + ", while the realm of the IdP is:" + realm);
                    }
                }
            }
        } catch (SessionException ex) {
            logger.error(classMethod + "Could not retrieve the session information", ex);
        }
        return isValidSessionInRealm;
    }

    private static List<String> extractAuthenticatingAuthorities(Assertion assertion) {
        final List<String> authenticatingAuthorities = new ArrayList<String>();
        final List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        if (authnStatements != null) {
            for (AuthnStatement authnStatement : authnStatements) {
                final List<String> authorities = authnStatement.getAuthnContext().getAuthenticatingAuthority();
                if (authorities != null) {
                    authenticatingAuthorities.addAll(authorities);
                }
            }
        }
        return authenticatingAuthorities;
    }

    /**
     * Obtain the AuthnContext from the provided assertion.
     * Used in an IdP Proxy scenario to relay the AuthnContext from the IdP back to the SP.
     * @param assertion The SAML Assertion to obtain the AuthnContext from.
     * @return The AuthnContext.  Note that returned instance is mutable.
     * @throws SAML2Exception If the setting of the AuthnContextClassRef fails.
     */
    private static AuthnContext getAuthContextFromAssertion(Assertion assertion) throws SAML2Exception {
        AuthnContext authnContextFromAssertion = null;
        final List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        if (CollectionUtils.isNotEmpty(authnStatements)) {
            logger.debug("AuthStatements present - reading first one");
            final AuthnStatement authnStatement = authnStatements.get(0);
            final AuthnContext authnContext = authnStatement.getAuthnContext();
            if (authnContext != null) {
                final String authnContextClassRef = authnContext.getAuthnContextClassRef();
                if (StringUtils.isNotEmpty(authnContextClassRef)) {
                    logger.debug("Setting AuthContextClassRef to <{}>", authnContextClassRef);
                    // Obtain a new AuthnContext to guarantee that this is mutable
                    authnContextFromAssertion = AssertionFactory.getInstance().createAuthnContext();
                    authnContextFromAssertion.setAuthnContextClassRef(authnContextClassRef);
                }
            }
        }
        return authnContextFromAssertion;
    }

    /**
     * Decrypt the provided encrypted SAML assertions.
     * @param metaAlias The meta alias.
     * @param realm The realm.
     * @param encryptedAssertions List of encrypted assertions.
     * @return A list of (decrypted) assertions.
     * @throws SAML2Exception If the entity or SP config cannot be found or the decryption fails.
     */
    private static List<Assertion> decryptAssertions(String metaAlias, String realm,
            List<EncryptedAssertion> encryptedAssertions) throws SAML2Exception {
        logger.debug("Decrypting Assertion for metaAlias <{}> in realm <{}>", metaAlias, realm);
        List<Assertion> decryptedAssertions = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(encryptedAssertions)) {
            decryptedAssertions = new ArrayList<>();
            final String hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                    .resolveValidDecryptionCredentials(realm, hostEntityId, SP);
            for (final EncryptedAssertion encryptedAssertion : encryptedAssertions) {
                final Assertion decryptedAssertion = encryptedAssertion.decrypt(decryptionKeys);
                logger.debug("Decrypted Assertion");
                decryptedAssertions.add(decryptedAssertion);
            }
        }
        return decryptedAssertions;
    }

    /**
     * Get the AuthLevel of the session.
     * @param session The session object.
     * @return the AuthLevel as a String.
     * @throws SAML2Exception If authLevel cannot be retrieved from the session.
     */
    private static String getAuthLevel(Object session) throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getAuthLevel: ";
        String authLevel = null;

        try {
            String[] values = sessionProvider.getProperty(session, SessionProvider.AUTH_LEVEL);
            if (values != null && values.length != 0 && StringUtils.isNotEmpty(values[0])) {
                authLevel = values[0];
            }
        } catch (SessionException se) {
            logger.error("{}exception retrieving auth level info from the session: ", classMethod, se);
            throw new SAML2Exception(SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
        }
        logger.debug("{}session authLevel = {}", classMethod, authLevel);
        return authLevel;
    }

    /**
     * Save (create/update) the IdP Session to the SAML2 failover store (token repository).
     * @param sessionIndex The session index key for the entry.
     * @param sessionProvider The session provider to use to determine the remaining time for the session.
     * @param idpSession The idpSession object to save.
     * @param session The session for which this relates to (and for which the expiry time will be determined).
     * @throws SAML2Exception If the session has expired or issues obtaining the session remaining time left.
     */
    public static void saveIdPSessionToTokenRepository(String sessionIndex, SessionProvider sessionProvider,
            IDPSession idpSession, Object session) {
        try {
            long sessionExpireTime;
            try {
                sessionExpireTime = currentTimeMillis() / 1000 + (sessionProvider.getTimeLeft(session));
            } catch (SessionException e) {
                logger.warn("Unable to get session timeout", e);
                sessionExpireTime = 0;
            }
            SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(sessionIndex, new IDPSessionCopy(idpSession),
                    sessionExpireTime);
            logger.debug("IDPSession saved to SAML2 token repository, sessionIndex: {}", sessionIndex);
        } catch (SAML2TokenRepositoryException se) {
            logger.error("Unable to save IDPSession to the SAML2 Token Repository", se);
        }
    }

    /**
     * Retrieve the IdP session from cache.  Always read from failover store then cache in memory.
     * This avoids a situation where the IdPCache can become stale, and not include all SP entities
     * that the failover store copy of the session includes.
     * @param sessionIndex The IdP Session index to retrieve
     * @return IdPSession The cache or failover store copy of IdPSession
     */
    public static IDPSession retrieveCachedIdPSession(String sessionIndex) {
        // Guards against the IDPSession being cached but not yet saved
        IDPSession idpSession = IDPCache.idpSessionsByIndices.get(sessionIndex);
        // Read from SAML2 Token Repository
        IDPSessionCopy idpSessionCopy = null;
        try {
            idpSessionCopy = (IDPSessionCopy) SAML2FailoverUtils.retrieveSAML2Token(sessionIndex);
        } catch (SAML2TokenRepositoryException se) {
            logger.error("Error while obtaining token from SAML2 Token Repository for sessionIndex: {}",
                    sessionIndex, se);
        }
        // Copy back to IDPSession
        if (idpSessionCopy != null) {
            idpSession = new IDPSession(idpSessionCopy);
            IDPCache.idpSessionsByIndices.put(sessionIndex, idpSession);
            if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                saml2Svc.setIdpSessionCount((long) IDPCache.idpSessionsByIndices.size());
            }
        } else {
            logger.warn("IDPSessionCopy is NULL");
        }
        return idpSession;
    }

    /**
     * Remove the specified IdP session from the idpSessionsByIndices and authnContext caches and failover store.
     * @param sessionIndex The session index to remove.
     */
    public static void removeIdPSessionFromCachesAndFailoverStore(String sessionIndex) {
        IDPCache.idpSessionsByIndices.remove(sessionIndex);
        if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
            saml2Svc.setIdpSessionCount((long)IDPCache.idpSessionsByIndices.size());
        }
        IDPCache.authnContextCache.remove(sessionIndex);
        try {
            SAML2FailoverUtils.deleteSAML2Token(sessionIndex);
        } catch (SAML2TokenRepositoryException se) {
            logger.warn("Error while deleting token from SAML2 Token Repository for idpSessionIndex: {}", sessionIndex,
                    se);
        }
    }
}
