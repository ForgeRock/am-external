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
 * Copyright 2010-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.saml2.plugins.IDPAdapter;

/**
 * This class <code>DefaultIDPAdapter</code> implements a SAML2 Identity Provider Adapter.
 */
public class DefaultIDPAdapter implements IDPAdapter {

    /**
     * Default Constructor.
     */
    public DefaultIDPAdapter() {
    }

    /**
     * Default implementation, takes no action.
     */
    @Override
    public void initialize(String hostedEntityID, String realm) {
        // Do nothing
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    @Override
    public boolean preSingleSignOn(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            String reqID) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    @Override
    public boolean preAuthentication(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            Object session,
            String reqID,
            String relayState) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    @Override
    public boolean preSendResponse(
            AuthnRequest authnRequest,
            String hostProviderID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            Object session,
            String reqID,
            String relayState) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action.
     */
    @Override
    public void preSendFailureResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String faultCode,
            String faultDetail) throws SAML2Exception {
        // Do nothing
    }

    /**
     * Default implementation, takes no action.
     */
    @Override
    public void preSendFailureResponse(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            String faultCode,
            String faultDetail) throws SAML2Exception {
        // Do nothing
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostProviderID, String realm,
                                HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        // Do nothing
    }
}
