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
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.DoManageNameID;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A ManageNameID (MNI) servlet to provide a way to initiate name identifier changes or terminations.
 */
@Singleton
public class SPMniRedirectServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SPMniRedirectServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);
            String samlRequest =
                    request.getParameter(SAML2Constants.SAML_REQUEST);

            String samlResponse =
                    request.getParameter(SAML2Constants.SAML_RESPONSE);

            if (samlRequest != null) {
                DoManageNameID.processHttpRequest(request, response, paramsMap);
            }

            if (samlResponse != null) {
                boolean success = DoManageNameID.processManageNameIDResponse(
                        request, response, paramsMap);
                if (success == true) {
                    String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
                    if (relayState != null &&
                            SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.SP_ROLE, true)) {
                        response.sendRedirect(relayState);
                    } else {
                        request.getRequestDispatcher(Constants.MNI_SUCCESS_URL).forward(request,
                                response);

                    }
                } else {
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "requestProcessingMNIError",
                            SAML2Utils.bundle.getString("requestProcessingMNIError"));
                    return;
                }
            }
        } catch (SAML2Exception | SessionException e) {
            logger.error("Error processing ManageNameIDRequest ", e);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "requestProcessingMNIError",
                    SAML2Utils.bundle.getString("requestProcessingMNIError") + " " +
                            e.getMessage());
        }
    }
}
