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
