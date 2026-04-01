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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.spi.handlers.authentication;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.radius.server.InvalidCallbackException;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * An {@link AuthenticationStrategy} that uses chains to perform user authentication.
 */
class ChainAuthenticationStrategy implements AuthenticationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ChainAuthenticationStrategy.class);

    private final OpenAMAuthFactory amAuthFactory;
    private final String realm;
    private final String authChain;

    /**
     * Constructs a new {@code ChainAuthenticationStrategy}.
     *
     * @param amAuthFactory the {@link OpenAMAuthFactory} to create the {@link AuthContext}
     * @param realm the name of the realm in which the authentication chain is defined
     * @param authChain the name of the authentication chain
     */
    ChainAuthenticationStrategy(OpenAMAuthFactory amAuthFactory, String realm, String authChain) {
        this.amAuthFactory = amAuthFactory;
        this.realm = realm;
        this.authChain = authChain;
    }

    @Override
    public boolean startAuthentication(ContextHolder holder, String clientIPAddress) {
        logger.debug("Entering ChainAuthenticationStrategy.startAuthentication");

        // now create an authContext and trigger loading of whatever authN modules will be used
        try {
            holder.setAuthContext(amAuthFactory.getAuthContext(realm));
        } catch (final AuthLoginException e) {
            logger.error("Unable to start create " + AuthContext.class.getName() + ". Denying Access.", e);
            logger.debug("Leaving ChainAuthenticationStrategy.startAuthentication");
            return false;
        }

        // Set the client IP address so that auditing reflects the client, not the Radius Server
        if (StringUtils.isNotBlank(clientIPAddress)) {
            holder.getAuthContext().setClientHostName(clientIPAddress);
        }
        try {
            holder.getAuthContext().login(AuthContext.IndexType.SERVICE, authChain);
        } catch (final AuthLoginException e) {
            logger.error("Unable to start login process. Denying Access.", e);
            logger.debug("Leaving ChainAuthenticationStrategy.startAuthentication");
            return false;
        }

        return true;
    }

    @Override
    public Callback[] getNextCallbacks(ContextHolder holder) throws InvalidCallbackException {
        if (!holder.getAuthContext().hasMoreRequirements()) {
            return new Callback[0];
        }
        Callback[] callbacks = holder.getAuthContext().getRequirements(true);
        if (!(callbacks[0] instanceof PagePropertiesCallback)) {
            throw new InvalidCallbackException("Invalid callback sequence: Expected first callback to be "
                    + "PagePropertiesCallback but received " + callbacks[0].getClass().getName());
        }
        return callbacks;
    }

    @Override
    public void submitCallbacks(ContextHolder holder, Callback[] callbacks) {
        holder.getAuthContext().submitRequirements(callbacks);
    }

    @Override
    public boolean isAuthenticationSuccessful(ContextHolder holder) {
        return holder.getAuthContext() != null && holder.getAuthContext().getStatus() == AuthContext.Status.SUCCESS;
    }

    @Override
    public boolean terminateAuthentication(ContextHolder holder) {
        try {
            holder.getAuthContext().logout();
        } catch (AuthLoginException e) {
            return false;
        }
        return true;
    }

    @Override
    public String getUniversalId(ContextHolder holder) {
        String ssoTokenId = null;
        try {
            SSOToken token = holder.getAuthContext().getSSOToken();
            if (token != null) {
                ssoTokenId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
            } else {
                logger.debug("No SSO token available from the auth context.");
            }
        } catch (L10NMessageImpl e) {
            logger.warn("Could not get universal ID from the SSOToken.", e);
        }
        return ssoTokenId;
    }
}
