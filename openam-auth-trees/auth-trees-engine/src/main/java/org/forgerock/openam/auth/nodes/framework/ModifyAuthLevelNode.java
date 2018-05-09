/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that increases or decreases the current auth level by a fixed, configurable amount.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
    configClass = ModifyAuthLevelNode.Config.class)
public class ModifyAuthLevelNode extends SingleOutcomeNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The amount to increment/decrement the auth level.
         * @return the amount.
         */
        @Attribute(order = 100)
        int authLevelIncrement();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ModifyAuthLevelNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ModifyAuthLevelNode started");
        JsonValue newSharedState = context.sharedState.copy();
        JsonValue authLevel = context.sharedState.get(AUTH_LEVEL);
        logger.debug("authLevel {} to increment by {}", authLevel, config.authLevelIncrement());
        if (authLevel.isNull()) {
            newSharedState.add(AUTH_LEVEL, config.authLevelIncrement());
        } else {
            newSharedState.put(AUTH_LEVEL, authLevel.asInteger() + config.authLevelIncrement());
        }
        return goToNext().replaceSharedState(newSharedState).build();
    }

    /**
     * Returns value of the auth level increment.
     *
     * @return the auth level increment.
     */
    public int getAuthLevelIncrement() {
        return config.authLevelIncrement();
    }
}
