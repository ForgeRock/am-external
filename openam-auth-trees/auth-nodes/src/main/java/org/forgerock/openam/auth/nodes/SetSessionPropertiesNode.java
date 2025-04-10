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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetSessionPropertiesNode.Config.class,
        tags = {"utilities"},
        configValidator = SetSessionPropertiesNodeValidator.class)
public class SetSessionPropertiesNode extends SingleOutcomeNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         * @return a map of properties.
         */
        @Attribute(order = 100)
        Map<String, String> properties();

        /**
         * The maximum session time, in minutes, for the session created by this journey.
         * @return the maximum session time.
         */
        @Attribute(order = 200, validators = GreaterThanZeroValidator.class)
        @TimeUnit(MINUTES)
        Optional<Duration> maxSessionTime();

        /**
         * The maximum idle time, in minutes, for the session created by this journey.
         * @return the maximum idle time.
         */
        @Attribute(order = 300, validators = GreaterThanZeroValidator.class)
        @TimeUnit(MINUTES)
        Optional<Duration> maxIdleTime();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(SetSessionPropertiesNode.class);

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public SetSessionPropertiesNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("SetSessionPropertiesNode started");
        ActionBuilder actionBuilder = goToNext();
        config.properties().forEach(actionBuilder::putSessionProperty);
        config.maxSessionTime().ifPresent(actionBuilder::withMaxSessionTime);
        config.maxIdleTime().ifPresent(actionBuilder::withMaxIdleTime);
        return actionBuilder.build();
    }
}
