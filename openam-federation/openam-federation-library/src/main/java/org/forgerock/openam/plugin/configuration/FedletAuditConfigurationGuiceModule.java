/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.plugin.configuration;

import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.openam.audit.context.TransactionIdConfiguration;

import com.google.inject.AbstractModule;

/**
 * Guice Module for configuring bindings for the OpenAM Fedlet Audit Configuration classes.
 *
 */
public class FedletAuditConfigurationGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AuditEventPublisher.class).to(FedletAuditEventPublisherImpl.class);
        bind(AuditServiceConfigurationProvider.class).to(FedletAuditServiceConfigurationProviderImpl.class);
        bind(TransactionIdConfiguration.class).to(FedletTransactionIdConfigurationImpl.class);
    }
}
