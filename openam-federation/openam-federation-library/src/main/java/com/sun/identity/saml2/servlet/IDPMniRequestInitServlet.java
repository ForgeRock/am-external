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

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.DoManageNameID;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A ManageNameID (MNI) servlet to provide a way to initiate name identifier changes or terminations.
 */
@Singleton
public class IDPMniRequestInitServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(IDPMniRequestInitServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        FSUtils.setLbCookieIfNecessary(request, response);

        try {
            String metaAlias = request.getParameter("metaAlias");
            if ((metaAlias == null) || (metaAlias.length() == 0)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "nullIDPEntityID",
                        SAML2Utils.bundle.getString("nullIDPEntityID"));
                return;
            }

            String idpEntityID =
                    SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            String spEntityID = request.getParameter("spEntityID");

            if ((spEntityID == null) || (spEntityID.length() == 0)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "nullSPEntityID",
                        SAML2Utils.bundle.getString("nullSPEntityID"));
                return;
            }

            String binding = DoManageNameID.getMNIBindingInfo(request, metaAlias,
                    SAML2Constants.IDP_ROLE, spEntityID);
            if (!SAML2Utils.isIDPProfileBindingSupported(
                    realm, idpEntityID, SAML2Constants.MNI_SERVICE, binding)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "unsupportedBinding",
                        SAML2Utils.bundle.getString("unsupportedBinding"));
                return;
            }

            String requestType = request.getParameter("requestType");

            if ((requestType == null) || (requestType.length() == 0)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                        "nullRequestType",
                        SAML2Utils.bundle.getString("nullRequestType"));
                return;
            }

            String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
            if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000,
                    true)) {
                relayState = null;
            }
            String affiliationID =
                    request.getParameter(SAML2Constants.AFFILIATION_ID);

            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put("metaAlias", metaAlias);
            paramsMap.put("spEntityID", spEntityID);
            paramsMap.put("requestType", requestType);
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
            paramsMap.put(SAML2Constants.BINDING, binding);

            if (relayState != null) {
                paramsMap.put(SAML2Constants.RELAY_STATE, relayState);
            }

            if (affiliationID != null) {
                paramsMap.put(SAML2Constants.AFFILIATION_ID, affiliationID);
            }

            Object sess = SAML2Utils.checkSession(request, response,
                    metaAlias, paramsMap);
            if (sess == null) {
                return;
            }

            DoManageNameID.initiateManageNameIDRequest(request, response,
                    metaAlias, spEntityID, paramsMap);

            if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
                if (relayState != null &&
                        SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE, true)) {
                    response.sendRedirect(relayState);
                } else {
                    request.getRequestDispatcher(Constants.MNI_SUCCESS_URL).forward(request,response);

                }
            }
        } catch (SAML2Exception e) {
            logger.error("Error processing ManageNameID Request ", e);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "requestProcessingMNIError",
                    SAML2Utils.bundle.getString("requestProcessingMNIError"));
        }
    }
}
