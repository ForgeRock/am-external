/*
 * Copyright 2010-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class <code>DefaultIDPAdapter</code> implements a SAML2 Identity Provider Adapter.
 */
public class DefaultIDPAdapter implements SAML2IdentityProviderAdapter {

    /**
     * Default Constructor.
     */
    public DefaultIDPAdapter() {
    }

    /**
     * Default implementation, takes no action.
     */
    public void initialize(String hostedEntityID, String realm) {
        // Do nothing
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
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
    public void preSendFailureResponse(
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
