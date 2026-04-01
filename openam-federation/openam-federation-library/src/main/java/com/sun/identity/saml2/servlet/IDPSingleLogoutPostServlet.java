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

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSingleLogout;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Single log out (SLO) servlet.
 */
@Singleton
public class IDPSingleLogoutPostServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(IDPSingleLogoutPostServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Retrieves the LogoutRequest or LogoutResponse
        //Retrieves :
        //- RelayState - the target URL on successful Single Logout
        //- SAMLRequest - the LogoutRequest
        //OR
        //- SAMLResponse - the LogoutResponse

        String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
        if (relayState != null) {
            String tmpRs = (String) IDPCache.relayStateCache.remove(relayState);
            if (tmpRs != null) {
                relayState = tmpRs;
            }
        }
        if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000, true)) {
            relayState = null;
        }
        String samlResponse = request.getParameter(SAML2Constants.SAML_RESPONSE);
        if (samlResponse != null) {
            boolean doRelayState = true;
            try {
                /**
                 * Gets and processes the Single <code>LogoutResponse</code> from SP,
                 * destroys the local session, checks response's issuer
                 * and inResponseTo.
                 *
                 * @param request the HttpServletRequest.
                 * @param response the HttpServletResponse.
                 * @param samlResponse <code>LogoutResponse</code> in the
                 *          XML string format.
                 * @param relayState the target URL on successful
                 * <code>LogoutResponse</code>.
                 * @throws SAML2Exception if error processing
                 *          <code>LogoutResponse</code>.
                 */
                doRelayState = IDPSingleLogout.processLogoutResponse(
                        request, response, new PrintWriter(response.getWriter(), true), samlResponse, relayState);
            } catch (SAML2Exception sse) {
                logger.error("Error processing LogoutResponse :",
                        sse);
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "LogoutResponseProcessingError",
                        SAML2Utils.bundle.getString("LogoutResponseProcessingError") +
                                " " + sse.getMessage());
                return;
            } catch (Exception e) {
                logger.error("Error processing LogoutResponse ",e);
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "LogoutResponseProcessingError",
                        SAML2Utils.bundle.getString("LogoutResponseProcessingError") +
                                " " + e.getMessage());
                return;
            }

            if (!doRelayState && !response.isCommitted()) {
                if (relayState != null &&
                        SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE, true)) {
                    if (relayState.indexOf("?") != -1) {
                        response.sendRedirect(relayState
                                + "&logoutStatus=logoutSuccess");
                    } else {
                        response.sendRedirect(relayState
                                + "?logoutStatus=logoutSuccess");
                    }
                } else {
                    request.getRequestDispatcher(Constants.IDP_SLO_SUCCESS_URL).forward(request, response);
                }
            }
        } else {
            String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);
            if (samlRequest != null) {
                try {
                    /**
                     * Gets and processes the Single <code>LogoutRequest</code> from SP.
                     *
                     * @param request the HttpServletRequest.
                     * @param response the HttpServletResponse.
                     * @param samlRequest <code>LogoutRequest</code> in the
                     *          XML string format.
                     * @param relayState the target URL on successful
                     * <code>LogoutRequest</code>.
                     * @throws SAML2Exception if error processing
                     *          <code>LogoutRequest</code>.
                     */
                    IDPSingleLogout.processLogoutRequest(request,response, new PrintWriter(response.getWriter(), true),
                            samlRequest,relayState);
                } catch (SAML2Exception sse) {
                    logger.error("Error processing LogoutRequest :", sse);
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "LogoutRequestProcessingError",
                            SAML2Utils.bundle.getString("LogoutRequestProcessingError")
                                    + " " + sse.getMessage());
                } catch (Exception e) {
                    logger.error("Error processing LogoutRequest ",e);
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "LogoutRequestProcessingError",
                            SAML2Utils.bundle.getString("LogoutRequestProcessingError")
                                    + " " + e.getMessage());
                }
            }
        }
    }
}
