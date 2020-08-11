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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.FAILURE_URL;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutputState;
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
            configClass = SetFailureUrlNode.Config.class,
        tags = {"utilities"})
public class SetFailureUrlNode extends SingleOutcomeNode {

    /**
     * Node Config Declaration.
     */
    public interface Config {
        /**
         * The failure URL.
         *
         * @return THe failure URL.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String failureUrl();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(SetFailureUrlNode.class);

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

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(FAILURE_URL)
        };
    }
}
