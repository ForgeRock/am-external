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
        configClass = RemoveSessionPropertiesNode.Config.class,
        tags = {"utilities"})
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
    private final Logger logger = LoggerFactory.getLogger(RemoveSessionPropertiesNode.class);

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
