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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.webhook.WebhookData;
import org.forgerock.openam.webhook.WebhookDataStore;

/**
 * Validate the webhook configured in RegisterLogoutWebhookNode.
 */
public class WebhookValidator implements ServiceConfigValidator {
    private final WebhookDataStore webhookDataStore;

    /**
     * Creates a new webhook validator for validation the webhook name provided for the Register Logout Webhook node.
     *
     * @param webhookDataStore The webhook data store to get the webhook object by name and realm.
     */
    @Inject
    public WebhookValidator(WebhookDataStore webhookDataStore) {
        this.webhookDataStore = webhookDataStore;
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
