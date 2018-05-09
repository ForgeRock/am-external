/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.FAILURE_URL;

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
 * A node which places the configured failure Url into shared state to be returned when authentication fails.
 *
 * <p>Places the result in the shared state as 'failureUrl'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = SetFailureUrlNode.Config.class)
public class SetFailureUrlNode extends SingleOutcomeNode {

    interface Config {
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String failureUrl();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Constructs a SetFailureUrlNode.
     *
     * @param config the node configuration.
     */
    @Inject
    public SetFailureUrlNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("SetFailureUrlNode started");
        logger.debug("failure url set to {}", config.failureUrl());
        return goToNext().replaceSharedState(context.sharedState.copy().put(FAILURE_URL, config.failureUrl())).build();
    }
}
