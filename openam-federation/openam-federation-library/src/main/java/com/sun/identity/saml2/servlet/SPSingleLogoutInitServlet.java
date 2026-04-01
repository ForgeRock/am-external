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
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.saml2.audit.SAML2Auditor;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSingleLogout;

/**
 * Single log out (SLO) servlet.
 */
@Singleton
public class SPSingleLogoutInitServlet extends AbstractSAMLHttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SPSingleLogoutInitServlet.class);
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;

    @Inject
    SPSingleLogoutInitServlet(AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
    }

    void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        SAML2Auditor saml2Auditor = new SAML2Auditor(auditEventPublisher, auditEventFactory, request);

        saml2Auditor.setMethod("spSingleLogoutInit");
        saml2Auditor.setSessionTrackingId(request.getSession().getId());
        saml2Auditor.auditAccessAttempt();

        // Retrieves the Request Query Parameters
        // Binding are the required query parameters
        // binding - binding used for this request

        try {
            String RelayState = request.getParameter(SAML2Constants.RELAY_STATE);
            if (RelayState == null || RelayState.isEmpty()) {
                RelayState = request.getParameter(SAML2Constants.GOTO);
            }
            if (!ESAPI.validator().isValidInput("RelayState", RelayState, "HTTPQueryString", 2000, true)) {
                RelayState = null;
            }

            String metaAlias = null;

            Object ssoToken = null;
            try {
                ssoToken = SessionManager.getProvider().getSession(request);
                saml2Auditor.setSSOTokenId(ssoToken);
            } catch (SessionException se) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No session.");
                }
                ssoToken = null;
            }

            String spEntityID = null;
            SAML2MetaManager manager = new SAML2MetaManager();
            if (!SPCache.isFedlet) {
                if (ssoToken == null) {
                    //There is no local session, so we can't perform the logout on the IdP,
                    //let's just return with HTTP 200
                    if (RelayState != null && !RelayState.isEmpty() &&
                            SAML2Utils.isRelayStateURLValid(request, RelayState, SAML2Constants.SP_ROLE, true)) {
                        saml2Auditor.auditAccessSuccess();
                        response.sendRedirect(RelayState);
                    } else {
                        saml2Auditor.auditAccessSuccess();
                        request.getRequestDispatcher(Constants.SP_SLO_SUCCESS).forward(request, response);
                    }
                    return;
                }
                String[] values = SessionManager.getProvider().
                        getProperty(ssoToken, SAML2Constants.SP_METAALIAS);
                if (values != null && values.length > 0) {
                    metaAlias = values[0];
                }
            } else {
                saml2Auditor.setSSOTokenId(ssoToken);

                spEntityID = request.getParameter("spEntityID");
                if ((spEntityID == null) || (spEntityID.length() == 0)) {
                    List spMetaAliases =
                            manager.getAllHostedServiceProviderMetaAliases("/");
                    if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                        // get first one
                        metaAlias = (String) spMetaAliases.get(0);
                    }
                } else {
                    SPSSOConfigElement spConfig =
                            manager.getSPSSOConfig("/", spEntityID);
                    if (spConfig != null) {
                        metaAlias = spConfig.getValue().getMetaAlias();
                    }
                }
            }
            saml2Auditor.setRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
            if (metaAlias == null) {
                try {
                    SessionManager.getProvider().invalidateSession(ssoToken, request, response, null);
                } catch (SessionException se) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No session.");
                    }
                }
                if (RelayState != null &&
                        SAML2Utils.isRelayStateURLValid(request, RelayState, SAML2Constants.SP_ROLE, true)) {
                    saml2Auditor.auditAccessSuccess();
                    response.sendRedirect(RelayState);
                } else {
                    saml2Auditor.auditAccessSuccess();
                    request.getRequestDispatcher(Constants.SP_SLO_SUCCESS).forward(request, response);
                }
                return;
            }

            String idpEntityID = request.getParameter("idpEntityID");
            String binding = LogoutUtil.getSLOBindingInfo(request, metaAlias,
                    SAML2Constants.SP_ROLE, idpEntityID);
            if (spEntityID == null) {
                spEntityID = manager.getEntityByMetaAlias(metaAlias);
            }
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

            /**
             * Parses the request parameters and builds the Logout
             * Request to be sent to the IDP.
             *
             * @param request the HttpServletRequest.
             * @param response the HttpServletResponse.
             * @param metaAlias metaAlias of Service Provider. The format of
             *               this parameter is /realm_name/SP_name.
             * @param binding binding used for this request.
             * @param paramsMap Map of all other parameters.
             *       Following parameters names with their respective
             *       String values are allowed in this paramsMap.
             *       "RelayState" - the target URL on successful Single Logout
             *       "Destination" - A URI Reference indicating the address to
             *                       which the request has been sent.
             *       "Consent" - Specifies a URI a SAML defined identifier
             *                   known as Consent Identifiers.
             *       "Extension" - Specifies a list of Extensions as list of
             *                   String objects.
             * @throws SAML2Exception if error initiating request to IDP.
             */
            HashMap<String, String> paramsMap = new HashMap<>();
            if (SPCache.isFedlet) {
                String sessionIndex = request.getParameter("SessionIndex");
                if ((sessionIndex == null) || (sessionIndex.length() == 0)) {
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "nullSessionIndex",
                            SAML2Utils.bundle.getString("nullSessionIndex"));
                    saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                            SAML2Utils.bundle.getString("nullSessionIndex"));
                    return;
                } else {
                    paramsMap.put("SessionIndex", sessionIndex);
                }
                String nameID = request.getParameter("NameIDValue");
                if ((nameID == null) || (nameID.length() == 0)) {
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "nullNameID",
                            SAML2Utils.bundle.getString("nullNameID"));
                    saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                            SAML2Utils.bundle.getString("nullNameID"));
                    return;
                } else {
                    if (spEntityID == null) {
                        if (manager == null) {
                            manager = new SAML2MetaManager();
                        }
                        spEntityID = manager.getEntityByMetaAlias(metaAlias);
                    }
                    if (idpEntityID == null) {
                        SAMLUtils.sendError(request, response,
                                response.SC_BAD_REQUEST,
                                "nullIDPEntityID",
                                SAML2Utils.bundle.getString("nullIDPEntityID"));
                        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                                SAML2Utils.bundle.getString("nullIDPEntityID"));
                        return;
                    }
                    paramsMap.put(
                            "infoKey", spEntityID + "|" + idpEntityID + "|" + nameID);
                }

            }
            paramsMap.put("metaAlias", metaAlias);
            paramsMap.put("idpEntityID", idpEntityID);
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);
            paramsMap.put(SAML2Constants.BINDING, binding);
            paramsMap.put("Destination", request.getParameter("Destination"));
            paramsMap.put("Consent", request.getParameter("Consent"));
            paramsMap.put("Extension", request.getParameter("Extension"));
            if (RelayState == null || RelayState.isEmpty()) {
                RelayState = SAML2Utils.getAttributeValueFromSSOConfig(
                        realm, spEntityID, SAML2Constants.SP_ROLE,
                        SAML2Constants.DEFAULT_RELAY_STATE);
            }
            if (RelayState != null) {
                paramsMap.put(SAML2Constants.RELAY_STATE, RelayState);
            }

            SPSingleLogout.initiateLogoutRequest(
                    request, response, new PrintWriter(response.getWriter(), true), binding, paramsMap, saml2Auditor);
            saml2Auditor.auditAccessSuccess();

            if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
                String requestUrl = request.getRequestURL().toString();
                if (RelayState != null && !RelayState.isEmpty() &&
                        SAML2Utils.isRelayStateURLValid(metaAlias, RelayState, SAML2Constants.SP_ROLE, true,
                                requestUrl)) {
                    response.sendRedirect(RelayState);
                } else {
                    request.getRequestDispatcher(Constants.SP_SLO_SUCCESS).forward(request, response);
                }
            }
        } catch (SAML2Exception sse) {
            logger.error("Error sending Logout Request ", sse);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST, "LogoutRequestCreationError",
                    SAML2Utils.bundle.getString("LogoutRequestCreationError") + " " + sse.getMessage());
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("LogoutRequestCreationError"));
        } catch (Exception e) {
            logger.error("Error initializing Request ", e);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST, "LogoutRequestCreationError",
                    SAML2Utils.bundle.getString("LogoutRequestCreationError") + " " + e.getMessage());
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("LogoutRequestCreationError"));
        }
    }

}
