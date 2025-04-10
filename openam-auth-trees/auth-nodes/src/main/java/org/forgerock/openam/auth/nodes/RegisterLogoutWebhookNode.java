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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.webhook.WebhookDataStore;
import org.forgerock.openam.webhook.node.WebhookChoices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that adds a new logout webhook.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RegisterLogoutWebhookNode.Config.class,
        tags = {"utilities"})
public class RegisterLogoutWebhookNode extends SingleOutcomeNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A webhook name string.
         * @return webhook name.
         */
        @Attribute(order = 100, requiredValue = true)
        @WebhookChoices
        String webhookName();
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(RegisterLogoutWebhookNode.class);

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     *
     * @param config Node configuration.
     * @param realm The realm of the node.
     * @param webhookDataStore Injected WebhookDataStore object to check the webhook exist by webhook name.
     * @throws NodeProcessException If the webhook associated with this node does not exist.
     */
    @Inject
    public RegisterLogoutWebhookNode(@Assisted Config config, @Assisted Realm realm, WebhookDataStore webhookDataStore)
            throws NodeProcessException {
        this.config = config;
        if (webhookDataStore.getWebhook(config.webhookName(), realm.asPath()) == null) {
            throw new NodeProcessException("Configured webhook does not exist: " + config.webhookName());
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("RegisterLogoutWebhookNode started");
        Action.ActionBuilder actionBuilder = goToNext();
        actionBuilder.addWebhook(config.webhookName());
        logger.debug("webhook has been registered with name {}", config.webhookName());
        return actionBuilder.build();
    }
}
