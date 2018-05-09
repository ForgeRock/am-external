/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.SUCCESS_URL;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which places the configured success Url into shared state to be returned when authentication succeeds.
 *
 * <p>Places the result in the shared state as 'successUrl'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = SetSuccessUrlNode.Config.class)
public class SetSuccessUrlNode extends SingleOutcomeNode {

    /**
     *  Configuration for the node.
     */
    public interface Config {
        /**
         * The success Url.
         * @return the success URL.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String successUrl();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");


    /**
     * Constructs a SetSuccessUrlNode.
     *
     * @param config the node configuration.
     */
    @Inject
    public SetSuccessUrlNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("SetSuccessUrlNode started");
        JsonValue newState = context.sharedState.copy().put(SUCCESS_URL, config.successUrl());
        logger.debug("successUrl {}", config.successUrl());
        return goToNext().replaceSharedState(newState).build();
    }
}
