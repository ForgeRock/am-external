/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.util.Uris;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.saml2.audit.SAML2Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.InvalidStatusCodeSaml2Exception;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.common.Saml2SpRedirectUrlBuilder;
import com.sun.identity.saml2.protocol.Response;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provide the SP ACS endpoint.
 */
@Singleton
public class SPAssertionConsumerServlet extends AbstractSAMLHttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SPAssertionConsumerServlet.class);
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Saml2SpRedirectUrlBuilder redirectUrlBuilder;

    @Inject
    SPAssertionConsumerServlet(AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory,
            Saml2SpRedirectUrlBuilder redirectUrlBuilder) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.redirectUrlBuilder = redirectUrlBuilder;
    }

    void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        try {
            // set up audit logger and attach initial information
            SAML2Auditor saml2Auditor = new SAML2Auditor(auditEventPublisher, auditEventFactory, request);
            saml2Auditor.setMethod("spAssertionConsumer");
            var session = request.getSession();
            saml2Auditor.setSessionTrackingId(session.getId());
            saml2Auditor.auditAccessAttempt();

            // check request, response, content length
            if ((request == null) || (response == null)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "nullInput", SAML2Utils.bundle.getString("nullInput"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                        SAML2Utils.bundle.getString("nullInput"));
                return;
            }
            // to avoid dos attack
            // or use SAML2Utils?
            try {
                SAMLUtils.checkHTTPContentLength(request);
            } catch (ServletException se) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "largeContentLength", se.getMessage());
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                        se.getMessage());
                return;
            }

            FSUtils.setLbCookieIfNecessary(request, response);

            String requestURL = request.getRequestURL().toString();
            // get entity id and realm
            String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            if (realm == null || realm.length() == 0) {
                realm = "/";
            }
            saml2Auditor.setRealm(realm);

            SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
            if (metaManager == null) {
                // logging?
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR, "errorMetaManager",
                        SAML2Utils.bundle.getString("errorMetaManager"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                        SAML2Utils.bundle.getString("errorMetaManager"));
                return;
            }
            String hostEntityId = null;
            try {
                hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            } catch (SAML2MetaException sme) {
                // logging?
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR, "metaDataError",
                        SAML2Utils.bundle.getString("metaDataError"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                        SAML2Utils.bundle.getString("metaDataError"));
                return;
            }
            if (hostEntityId == null) {
                // logging?
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR, "metaDataError",
                        SAML2Utils.bundle.getString("metaDataError"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                        SAML2Utils.bundle.getString("metaDataError"));
                return;
            }
            String relayState = request.getParameter(SAML2Constants.RELAY_STATE);

            // federate flag
            String federate = request.getParameter(SAML2Constants.FEDERATE);
            SessionProvider sessionProvider = null;
            ResponseInfo respInfo = null;
            try {
                sessionProvider = SessionManager.getProvider();
            } catch (SessionException se) {
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR, "nullSessionProvider",
                        se.getMessage());
                saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
                return;
            }
            try {
                respInfo = SPACSUtils.getResponse(
                        request, response, realm, hostEntityId, metaManager);
                saml2Auditor.setRequestId(respInfo.getResponse().getInResponseTo());
            } catch (SAML2Exception se) {
                // Only do a sendError if one hasn't already been called.
                if (!response.isCommitted()) {
                    SAMLUtils.sendError(request, response,
                            response.SC_INTERNAL_SERVER_ERROR, "getResponseError",
                            se.getMessage());
                }
                saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
                return;
            }

            String ecpRelayState = respInfo.getRelayState();
            if ((ecpRelayState != null) && (ecpRelayState.length() > 0)) {
                relayState = ecpRelayState;
            }

            Object token = null;
            try {
                token = sessionProvider.getSession(request);
                saml2Auditor.setAuthTokenId(token);

            } catch (SessionException se) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "spAssertionConsumer.jsp: Token is null." +
                                    se.getMessage());
                }
                token = null;
            }
            if (federate != null && federate.trim().equals("true") &&
                    token == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("spAssertionConsumer.jsp: federate "
                            + "is true, and token is null. do local login first.");
                }
                FSUtils.forwardRequest(request, response,
                        redirectUrlBuilder.getRedirectUrl(realm, hostEntityId, metaManager, respInfo, request, relayState));
                saml2Auditor.auditForwardToLocalUserLogin();
                return;
            }
            Object newSession = null;
            Response saml2Resp = respInfo.getResponse();

            String requestID = saml2Resp.getInResponseTo();
            boolean isProxyOn = IDPProxyUtil.isIDPProxyEnabled(requestID);
            try {
                newSession = SPACSUtils.processResponse(request, response, new PrintWriter(response.getWriter(), true),
                        metaAlias,
                        token,
                        respInfo, realm, hostEntityId, metaManager, saml2Auditor);
                saml2Auditor.setUserId(sessionProvider.getPrincipalName(newSession));
                saml2Auditor.setSSOTokenId(newSession);

            } catch (SAML2Exception se) {
                String[] data = {hostEntityId, se.getMessage(), ""};
                if (LogUtil.isErrorLoggable(Level.FINE)) {
                    data[2] = saml2Resp.toXMLString(true, true);
                }
                LogUtil.error(Level.INFO, LogUtil.SP_SSO_FAILED, data, null);
                logger.error("spAssertionConsumer.jsp: SSO failed.", se);
                if (se.isRedirectionDone()) {
                    //The redirection has already happened successfully and logging auditsuccess.
                    saml2Auditor.auditAccessSuccess();
                    return;
                } else if (se instanceof InvalidStatusCodeSaml2Exception && isProxyOn) {
                    logger.error("spAssertionConsumer.jsp: Non-Success status code in response");
                    String firstlevelStatusCodeValue = ((InvalidStatusCodeSaml2Exception) se).getFirstlevelStatuscode();
                    String secondlevelStatusCodeValue =
                            ((InvalidStatusCodeSaml2Exception) se).getSecondlevelStatuscode();
                    try {
                        IDPProxyUtil.sendResponseWithStatus(request, response,
                                new PrintWriter(response.getWriter(), true),
                                requestID, metaAlias, hostEntityId, realm, firstlevelStatusCodeValue,
                                secondlevelStatusCodeValue);
                    } catch (SAML2Exception samle) {
                        logger.error("Failed to send response with status ", samle);
                    }
                    return;
                } else {
                    if (se.getMessage().equals(SAML2Utils.bundle.getString("noUserMapping"))) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("spAssertionConsumer.jsp:need local login!!");
                        }
                        FSUtils.forwardRequest(request, response,
                                redirectUrlBuilder.getRedirectUrl(realm, hostEntityId, metaManager, respInfo, request, relayState));
                        saml2Auditor.auditForwardToLocalUserLogin();
                        return;
                    }
                    saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                            SAML2Utils.bundle.getString("SSOFailed"));
                    SAMLUtils.sendError(request, response, response.SC_INTERNAL_SERVER_ERROR, "SSOFailed",
                            SAML2Utils.bundle.getString("SSOFailed"));
                    return;
                }
            }

            if (newSession == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Session is null.");
                    logger.debug("spAssertionConsumer.jsp:Login has failed!!");
                }
                SAMLUtils.sendError(request, response, response.SC_INTERNAL_SERVER_ERROR, "SSOFailed",
                        SAML2Utils.bundle.getString("SSOFailed"));
                saml2Auditor.auditAccessFailure(String.valueOf(
                        response.SC_INTERNAL_SERVER_ERROR), SAML2Utils.bundle.getString("SSOFailed"));
                return;
            }
            logger.debug("SSO SUCCESS");
            String[] redirected = sessionProvider.getProperty(newSession,
                    SAML2Constants.RESPONSE_REDIRECTED);
            if ((redirected != null) && (redirected.length != 0) &&
                    redirected[0].equals("true")) {
                logger.debug("Redirection already done in SPAdapter.");
                // response redirected already in SPAdapter

                saml2Auditor.auditForwardToProxy();
                return;
            }
            if (isProxyOn) {
                try {
                    IDPProxyUtil.generateProxyResponse(request, response, new PrintWriter(response.getWriter(), true),
                            metaAlias,
                            respInfo,
                            newSession, saml2Auditor);
                    saml2Auditor.auditForwardToProxy();
                } catch (SAML2Exception se) {
                    logger.error("Failed sending proxy response", se);
                    saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
                }
                return;
            }
            // redirect to relay state
            String finalUrl = SPACSUtils.getRelayState(relayState, realm, hostEntityId, metaManager);

            String realFinalUrl = finalUrl;
            String redirectUrl = SPACSUtils.getIntermediateURL(realm, hostEntityId, metaManager);
            String realRedirectUrl = null;
            if (redirectUrl != null && redirectUrl.length() != 0) {
                if (realFinalUrl != null && realFinalUrl.length() != 0) {
                    if (redirectUrl.indexOf("?") != -1) {
                        redirectUrl += "&goto=";
                    } else {
                        redirectUrl += "?goto=";
                    }
                    redirectUrl += Uris.urlEncodeQueryParameterNameOrValue(realFinalUrl);
                }
                realRedirectUrl = redirectUrl;
                logger.debug("spAssertionConsumer.jsp: Intermediate URL present. Using this constructed final URL: {}",
                        realRedirectUrl);
            } else {
                realRedirectUrl = finalUrl;
            }
            if (realRedirectUrl == null || (realRedirectUrl.trim().length() == 0)) {
                if (isProxyOn) {
                    saml2Auditor.auditForwardToProxy();
                    return;
                } else {
                    saml2Auditor.auditAccessSuccess();
                    request.getRequestDispatcher(Constants.SSO_SUCCESS).forward(request,
                            response);
                }
            } else {
                // log it
                try {
                    String requestUrl = request.getRequestURL().toString();
                    SAML2Utils.validateRelayStateURL(realm, hostEntityId, realRedirectUrl, SAML2Constants.SP_ROLE,
                            requestUrl);
                } catch (SAML2Exception se) {
                    SAMLUtils.sendError(request, response,
                            response.SC_BAD_REQUEST, "requestProcessingError",
                            SAML2Utils.bundle.getString("requestProcessingError") + " " + se.getMessage());
                    saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
                    return;
                }

                saml2Auditor.auditAccessSuccess();
                response.sendRedirect(realRedirectUrl);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
