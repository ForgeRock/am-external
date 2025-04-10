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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.helpers;

import static org.forgerock.openam.authentication.api.AuthenticationConstants.SESSION_ID;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.session.Session;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;

/**
 * Helper class for accessing the Auth Session.
 */
@Singleton
public class AuthSessionHelper {

    private final Provider<SessionService> sessionServiceProvider;
    private final JwtBuilderFactory jwtBuilderFactory;

    @Inject
    AuthSessionHelper(Provider<SessionService> sessionServiceProvider, JwtBuilderFactory jwtBuilderFactory) {
        this.sessionServiceProvider = sessionServiceProvider;
        this.jwtBuilderFactory = jwtBuilderFactory;
    }

    /**
     * Get the Authentication Session from the Auth ID.
     * @param authId The Auth ID, must not be null or empty.
     * @return An optional of the Auth Session if available, or empty if not.
     * @throws NodeProcessException If the session is unavailable.
     */
    public Session getAuthSession(String authId) throws NodeProcessException {
        Reject.ifBlank(authId);
        try {
            String sessionId = jwtBuilderFactory.reconstruct(authId, SignedJwt.class)
                                       .getClaimsSet()
                                       .getClaim(SESSION_ID, String.class);
            return sessionServiceProvider.get().getSession(sessionServiceProvider.get().asSessionID(sessionId));
        } catch (SessionException | JwtRuntimeException e) {
            throw new NodeProcessException("Session unavailable", e);
        }
    }
}
