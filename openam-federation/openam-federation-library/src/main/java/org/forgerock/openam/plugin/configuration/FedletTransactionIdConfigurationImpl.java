/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.plugin.configuration;

/**
 *  TransactionIdConfiguration implementation for the OpenAM Fedlet.
 */
import org.forgerock.openam.audit.context.TransactionIdConfiguration;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Responsible for deciding whether or not transaction ID received as HTTP header should be accepted.
 */
public class FedletTransactionIdConfigurationImpl implements TransactionIdConfiguration {

    @Override
    public boolean trustHttpTransactionHeader() {
        return SystemPropertiesManager.getAsBoolean("org.forgerock.http.TrustTransactionHeader", false);
    }
}
