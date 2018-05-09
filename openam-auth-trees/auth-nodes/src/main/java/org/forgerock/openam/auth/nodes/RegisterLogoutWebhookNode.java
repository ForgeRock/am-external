/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
import org.forgerock.openam.webhook.node.WebhookChoices;
import org.forgerock.openam.webhook.WebhookDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that adds a new logout webhook.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RegisterLogoutWebhookNode.Config.class)
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
    private final Logger logger = LoggerFactory.getLogger("amAuth");

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
