/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.webhook.WebhookData;
import org.forgerock.openam.webhook.WebhookDataStore;

import com.sun.identity.shared.debug.Debug;

/**
 * Validate the webhook configured in RegisterLogoutWebhookNode.
 */
public class WebhookValidator implements ServiceConfigValidator {
    private final WebhookDataStore webhookDataStore;
    private final Debug debug;

    /**
     * Creates a new webhook validator for validation the webhook name provided for the Register Logout Webhook node.
     *
     * @param webhookDataStore The webhook data store to get the webhook object by name and realm.
     * @param debug The auth debug instance.
     */
    @Inject
    public WebhookValidator(WebhookDataStore webhookDataStore, @Named("amAuth") Debug debug) {
        this.webhookDataStore = webhookDataStore;
        this.debug = debug;
    }

    /**
     * Validation for the webhook existence in the system by the name provided in the Register Logout Webhook node.
     *
     * @param realm The realm the config is in, or {@code null} if it is not in a realm.
     * @param configPath The names of the service config. The last element in the list will be the name of this config.
     *                   In the case of service-level (non-SubSchema) config, this will be an empty list.
     * @param attributes The map of attribute names to values. This map will contain the default values for attributes
     *                   where a value is not defined in this instance.
     * @throws ServiceErrorException
     */
    @Override
    public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
            throws ServiceErrorException {
        String webhookId = CollectionUtils.getFirstItem(attributes.get("webhookName"));
        WebhookData webhook = webhookDataStore.getWebhook(webhookId, realm.toString());
        if (webhook == null) {
            throw new ServiceErrorException("Configured webhook does not exist");
        }
    }
}
