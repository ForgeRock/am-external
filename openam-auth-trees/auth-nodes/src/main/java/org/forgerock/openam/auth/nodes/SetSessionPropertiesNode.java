/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.SessionPropertyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetSessionPropertiesNode.Config.class)
public class SetSessionPropertiesNode extends SingleOutcomeNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         * @return a map of properties.
         */
        @Attribute(order = 100, validators = SessionPropertyValidator.class)
        Map<String, String> properties();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

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
        Action.ActionBuilder actionBuilder = goToNext();
        config.properties().entrySet().forEach(property -> {
            actionBuilder.putSessionProperty(property.getKey(), property.getValue());
            logger.debug("set session property {}", property);
        });
        return actionBuilder.build();
    }
}
