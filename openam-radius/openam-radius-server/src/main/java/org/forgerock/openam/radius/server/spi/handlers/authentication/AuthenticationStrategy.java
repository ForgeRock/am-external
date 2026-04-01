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

/**
 * Authentication strategy used in RADIUS authentication flows.
 */
public interface AuthenticationStrategy {

    /**
     * Starts the authentication process.
     *
     * @param holder the {@link ContextHolder} for which to start the authentication process
     * @param clientIPAddress the IP address of the client
     * @return {@code true} if the authentication is successfully started, {@code false} otherwise
     */
    boolean startAuthentication(ContextHolder holder, String clientIPAddress);

    /**
     * Gets the next set of {@link Callback}s.
     *
     * @param holder the {@link ContextHolder}
     * @return the next set of {@link Callback}s
     * @throws InvalidCallbackException if an unsupported callback for the strategy is encountered
     */
    Callback[] getNextCallbacks(ContextHolder holder) throws InvalidCallbackException;

    /**
     * Submits the provided callbacks to the authentication context.
     *
     * @param holder the {@link ContextHolder}
     * @param callbacks the {@link Callback}s to submit
     */
    void submitCallbacks(ContextHolder holder, Callback[] callbacks);

    /**
     * Determines if the authentication was successful.
     *
     * @param holder the {@link ContextHolder}
     * @return {@code true} if authentication was successful, {@code false} otherwise
     */
    boolean isAuthenticationSuccessful(ContextHolder holder);

    /**
     * Terminates the authentication process.
     *
     * @param holder the {@link ContextHolder}
     * @return {@code true} if the termination was successful, {@code false} otherwise
     */
    boolean terminateAuthentication(ContextHolder holder);

    /**
     * Get the universal ID of the authenticated user.
     * @param holder the {@link ContextHolder}
     *
     * @return the universalId, or {@code null} if none is available
     */
    String getUniversalId(ContextHolder holder);
}
