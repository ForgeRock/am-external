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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import javax.inject.Provider;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which gets specified data from the session and places it into shared state.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = SessionDataNode.Config.class)
public class SessionDataNode extends SingleOutcomeNode {

    interface Config {
        /**
         * The session data key specifies the name of session data to get.
         *
         * @return the session data key.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String sessionDataKey();

        /**
         * The shared state key specifies the name under which the data is placed into shared state.
         *
         * @return the shared state key.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        String sharedStateKey();
    }

    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final Config config;
    private final Provider<SessionService> sessionServiceProvider;

    /**
     * Constructs a SessionDataNode.
     *
     * @param config the node configuration.
     * @param sessionServiceProvider provides Sessions.
     */
    @Inject
    public SessionDataNode(@Assisted Config config, Provider<SessionService> sessionServiceProvider) {
        this.config = config;
        this.sessionServiceProvider = sessionServiceProvider;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("SessionDataNode started");
        String ssoTokenId = context.request.ssoTokenId;
        if (ssoTokenId != null) {
            String sessionData = getSessionData(ssoTokenId);
            if (sessionData != null) {
                logger.debug("Placing session data in shared state as {}", config.sharedStateKey());
                JsonValue newState = context.sharedState.copy().put(config.sharedStateKey(), sessionData);
                return goToNext().replaceSharedState(newState).build();
            }
        } else {
            logger.debug("No existing session found");
        }
        return goToNext().build();
    }

    private String getSessionData(String ssoTokenId) {
        String sessionData = null;
        try {
            Session oldSession = sessionServiceProvider.get().getSession(new SessionID(ssoTokenId));
            sessionData = oldSession.getProperties().get(config.sessionDataKey());
            logger.debug("Got session data {} of value {}", config.sessionDataKey(), sessionData);
        } catch (SessionException e) {
            logger.error("Exception occurred trying to get data ({}) from existing session",
                    config.sessionDataKey(), e);
        }
        return sessionData;
    }
}
