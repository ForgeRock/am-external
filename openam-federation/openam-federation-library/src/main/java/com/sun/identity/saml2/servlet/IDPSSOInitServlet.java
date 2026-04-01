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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.saml2.audit.SAML2Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.IDPSSOUtil;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Initiate an SSO session.
 */
@Singleton
public class IDPSSOInitServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(IDPSSOInitServlet.class);
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;

    @Inject
    IDPSSOInitServlet(AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
    }

    void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       
        SAML2Auditor saml2Auditor = new SAML2Auditor(auditEventPublisher, auditEventFactory, request);
        saml2Auditor.setMethod("idpSSOInit");
        var session = request.getSession(false);
        if (session != null) {
            saml2Auditor.setSessionTrackingId(session.getId());
        }
        saml2Auditor.auditAccessAttempt();
        // Retrieve the Request Query Parameters
        // metaAlias and spEntiyID are the required query parameters
        // metaAlias - Identity Provider Entity Id
        // spEntityID - Service Provider Identifier

        FSUtils.setLbCookieIfNecessary(request, response);
        try {
            String cachedResID = request.getParameter(SAML2Constants.RES_INFO_ID);
            // if this id is set, then this is a redirect from the COT
            // cookie writer. There is already an assertion response
            // cached in this provider. Send it back directly.
            if ((cachedResID != null) && (cachedResID.length() != 0)) {
                IDPSSOUtil.sendResponse(request, response, new PrintWriter(response.getWriter(), true), cachedResID);
                return;
            }

            String metaAlias = request.getParameter("metaAlias");
            saml2Auditor.setRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
            if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
                SAMLUtils.sendError(
                        request, response, response.SC_BAD_REQUEST, "nullIDPEntityID",
                        SAML2Utils.bundle.getString("nullIDPEntityID"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                        SAML2Utils.bundle.getString("nullSPEntityID"));
                return;
            }
            String spEntityID = request.getParameter("spEntityID");

            if ((spEntityID == null) || (spEntityID.length() == 0)) {
                SAMLUtils.sendError(
                        request, response, response.SC_BAD_REQUEST, "nullSPEntityID",
                        SAML2Utils.bundle.getString("nullSPEntityID"));
                saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                        SAML2Utils.bundle.getString("nullSPEntityID"));
                return;
            }

            // get the nameIDPolicy
            String nameIDFormat = request.getParameter(SAML2Constants.NAMEID_POLICY_FORMAT);
            String relayState = SAML2Utils.getRelayState(request);

            IDPSSOUtil.doSSOFederate(request, response, new PrintWriter(response.getWriter(), true), null, spEntityID, metaAlias,
                    nameIDFormat, relayState, saml2Auditor);
            saml2Auditor.auditAccessSuccess();
        } catch (SAML2Exception sse) {
            logger.error("Error processing request " , sse);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "requestProcessingError",
                    SAML2Utils.bundle.getString("requestProcessingError") + " " +
                            sse.getMessage());
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("requestProcessingError"));
            return;
        } catch (Exception e) {
            logger.error("Error processing request ",e);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "requestProcessingError",
                    SAML2Utils.bundle.getString("requestProcessingError") + " " +
                            e.getMessage());
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("requestProcessingError"));
            return;
        }
    }
}
