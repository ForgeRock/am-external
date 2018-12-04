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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that decides if the current auth level is greater than or equal to a fixed, configurable amount.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AuthLevelDecisionNode.Config.class)
public class AuthLevelDecisionNode extends AbstractDecisionNode {

    /**
     * Node Config Declaration.
     */
    public interface Config {
        /**
         * Auth Level.
         *
         * @return The auth level.
         */
        @Attribute(order = 100)
        int authLevelRequirement();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public AuthLevelDecisionNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AuthLevelDecisionNode started");
        JsonValue authLevel = context.sharedState.get(AUTH_LEVEL);
        logger.debug("authLevel {}", authLevel);
        boolean authLevelSufficient = !authLevel.isNull() && authLevel.asInteger() >= config.authLevelRequirement();
        logger.debug("authLevelSufficient {}", authLevelSufficient);
        return goTo(authLevelSufficient).build();
    }

    /**
     * Returns value of the threshold auth level.
     *
     * @return threshold auth level
     */
    public int getAuthLevelRequirement() {
        return config.authLevelRequirement();
    }
}
