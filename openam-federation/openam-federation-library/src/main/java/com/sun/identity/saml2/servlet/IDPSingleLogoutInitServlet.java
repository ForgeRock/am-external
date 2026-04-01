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

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.IDPSingleLogout;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Single log out (SLO) servlet.
 */
@Singleton
public class IDPSingleLogoutInitServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(IDPSingleLogoutInitServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        // Retrieve the Request Query Parameters
        // binding are the required query parameters
        // binding - binding used for this request

        try {
            String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
            if ((relayState == null) || (relayState.length() == 0)) {
                relayState = request.getParameter(SAML2Constants.GOTO);
            }
            if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000,
                    true)) {
                relayState = null;
            }
            Object ssoToken = null;
            try {
                ssoToken = SessionManager.getProvider().getSession(request);
            } catch (SessionException e) {
                String intermmediatePage = SystemPropertiesManager.get(
                        "openam.idpsloinit.nosession.intermmediate.page", "");

                if (intermmediatePage.length() != 0) {
                    if (relayState != null) {
                        intermmediatePage = intermmediatePage + "?RelayState=" + relayState;
                    }
                    response.sendRedirect(intermmediatePage);
                } else {
                    if (relayState != null &&
                            SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE, true)) {
                        response.sendRedirect(relayState);
                    } else {
                        request.getRequestDispatcher(Constants.IDP_SLO_SUCCESS_URL).forward(request, response);
                    }
                }
                return;
            }
            if (ssoToken == null) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "nullSSOToken", SAML2Utils.bundle.getString("nullSSOToken"));
                return;
            }
            String[] values = SessionManager.getProvider().
                    getProperty(ssoToken, SAML2Constants.IDP_META_ALIAS);
            String metaAlias = null;
            if (values != null && values.length > 0) {
                metaAlias = values[0];
            }
            if (metaAlias == null) {
                SessionManager.getProvider().invalidateSession(ssoToken, request, response, null);
                if (relayState != null &&
                        SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE, true)) {
                    response.sendRedirect(relayState);
                } else {
                    request.getRequestDispatcher(Constants.IDP_SLO_SUCCESS_URL).forward(request, response);
                }
                return;
            }

            String idpEntityID =
                    SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

            String binding = LogoutUtil.getSLOBindingInfo(request, metaAlias,
                    SAML2Constants.SP_ROLE, idpEntityID);
            if (!SAML2Utils.isIDPProfileBindingSupported(
                    realm, idpEntityID, SAML2Constants.SLO_SERVICE, binding)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "unsupportedBinding",
                        SAML2Utils.bundle.getString("unsupportedBinding"));
                return;
            }

            String logoutAll = request.getParameter(SAML2Constants.LOGOUT_ALL);

            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put("metaAlias", metaAlias);
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
            paramsMap.put(SAML2Constants.BINDING, binding);
            paramsMap.put("Destination", request.getParameter("Destination"));
            paramsMap.put("Consent", request.getParameter("Consent"));
            paramsMap.put("Extension", request.getParameter("Extension"));
            if (relayState != null) {
                paramsMap.put(SAML2Constants.RELAY_STATE, relayState);
            }

            if (logoutAll != null) {
                paramsMap.put(SAML2Constants.LOGOUT_ALL, logoutAll);
            }

            IDPSingleLogout.initiateLogoutRequest(request, response, new PrintWriter(response.getWriter()), binding, paramsMap);
            
            if (!response.isCommitted()) {
                String requestUrl = request.getRequestURL().toString();
                if (relayState != null &&
                        SAML2Utils.isRelayStateURLValid(metaAlias, relayState, SAML2Constants.IDP_ROLE, true,
                                requestUrl)) {
                    response.sendRedirect(relayState);
                } else {
                    request.getRequestDispatcher(Constants.IDP_SLO_SUCCESS_URL).forward(request, response);
                }
            }
        } catch (SAML2Exception sse) {
            logger.error("Error sending Logout Request ", sse);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "LogoutRequestProcessingError",
                    SAML2Utils.bundle.getString("LogoutRequestProcessingError") + " " +
                            sse.getMessage());
            return;
        } catch (Exception e) {
            logger.error("Error processing Request ", e);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "LogoutRequestProcessingError",
                    SAML2Utils.bundle.getString("LogoutRequestProcessingError") + " " +
                            e.getMessage());
        }

    }

}
