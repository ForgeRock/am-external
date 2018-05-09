/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.SessionPropertyNameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which removes session properties that are contributed by nodes that executed earlier in the tree.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RemoveSessionPropertiesNode.Config.class)
public class RemoveSessionPropertiesNode extends SingleOutcomeNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A set of property names to remove.
         * @return a set of property names.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, SessionPropertyNameValidator.class})
        Set<String> propertyNames();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Constructs a new RemoveSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public RemoveSessionPropertiesNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("RemoveSessionPropertiesNode started");
        Action.ActionBuilder actionBuilder = goToNext();
        config.propertyNames().forEach(propertyName -> {
            actionBuilder.removeSessionProperty(propertyName);
            logger.debug("sessionPropertyRemoved {}", propertyName);
        });
        return actionBuilder.build();
    }
}
