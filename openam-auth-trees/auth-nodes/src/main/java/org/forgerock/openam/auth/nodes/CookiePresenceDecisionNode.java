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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

//smoff

/**
 * Checks for the presence of the named cookie in the authentication request. Doesn't check cookie value, only presence
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = CookiePresenceDecisionNode.Config.class,
        tags = {"contextual"})
public class CookiePresenceDecisionNode extends AbstractDecisionNode {
    private final Logger logger = LoggerFactory.getLogger(CookiePresenceDecisionNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the HTTP header we want to look for.
         *
         * @return the name.
         */
        @Attribute(order = 100)
        String cookieName();

    }

    private final Config config;

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public CookiePresenceDecisionNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        //Pull cookies out of headers
        Map<String, String> cookies = context.request.cookies;
        logger.debug("CookiePresenceCheckNode cookies found: " + cookies);

        //If specified cookie name exists...
        if (cookies.containsKey(config.cookieName())) {
            logger.debug("CookiePresenceCheckNode cookie called " + config.cookieName() + " found!");
            return goTo(true).build();
        }

        //Cookie not present
        return goTo(false).build();
    }
}
