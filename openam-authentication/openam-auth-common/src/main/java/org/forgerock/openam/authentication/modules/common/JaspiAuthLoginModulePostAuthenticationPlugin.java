/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import java.util.Map;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;

/**
 * Implementations of this class must only have a zero-arg constructor.
 */
public abstract class JaspiAuthLoginModulePostAuthenticationPlugin implements AMPostAuthProcessInterface {

    protected final Debug DEBUG;

    private final String resourceBundleName;
    private final JaspiAuthModuleWrapper jaspiAuthModule;

    protected JaspiAuthLoginModulePostAuthenticationPlugin(String resourceBundleName,
                                                           JaspiAuthModuleWrapper jaspiAuthModule) {
        this.resourceBundleName = resourceBundleName;
        this.jaspiAuthModule = jaspiAuthModule;
        this.DEBUG = Debug.getInstance(resourceBundleName);
    }

    /**
     * Generates the required configuration to initialise the underlying JASPI ServerAuthModule.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @return The Map of configuration information for the underlying JASPI ServerAuthModule.
     * @throws AuthenticationException If an error occurs.
     */
    protected abstract Map<String, Object> generateConfig(HttpServletRequest request,
                                                          HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException;

    /**
     * Post processing of successful authentication, which initialises the underlying JASPI ServerAuthModule, as a new
     * instance of this class is created for the Post Authentication Process, and then calls the subtypes
     * onLoginSuccess method, and then finally calls the JASPI ServerAuthModule's secureResponse method.
     *
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    public final void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response,
                               SSOToken ssoToken) throws AuthenticationException {

        try {
            Map<String, Object> config = generateConfig(request, response, ssoToken);

            jaspiAuthModule.initialize(null, config);

            MessageInfo messageInfo = jaspiAuthModule.prepareMessageInfo(request, response);

            onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

            AuthStatus authStatus = jaspiAuthModule.secureResponse(messageInfo);

            if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                // nothing to do here just carry on
                DEBUG.message("Successfully secured response.");
            } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.message("Failed to secured response, included response message");
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.message("Has not finished securing response. Requires more information from client.");
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            } else {
                DEBUG.error("Invalid AuthStatus, " + authStatus.toString());
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            }

        } catch (AuthException e) {
            DEBUG.error("Authentication Failed", e);
            throw new AuthenticationException(resourceBundleName, "authFailed", null);
        }
    }

    /**
     * Internal call to subtype to perform any required logic before the secureResponse method is called on the
     * underlying JASPI ServerAuthModule.
     *
     * @param messageInfo The ServerAuthModules MessageInfo instance.
     * @param requestParamsMap A Map containing the HttpServletRequest parameters.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @throws AuthenticationException If an error occurs.
     */
    protected abstract void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                                           HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException;

}