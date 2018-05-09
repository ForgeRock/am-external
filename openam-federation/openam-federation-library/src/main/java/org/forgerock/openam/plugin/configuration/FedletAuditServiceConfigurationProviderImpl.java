/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.plugin.configuration;

import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;

/**
 * No-Op AuditServiceConfigurationProvider implementation for the OpenAM Fedlet.
 */
public class FedletAuditServiceConfigurationProviderImpl implements AuditServiceConfigurationProvider {

    @Override
    public void setupComplete() {
        // this section intentionally left blank
    }

    @Override
    public void addConfigurationListener(AuditServiceConfigurationListener listener) {
        // this section intentionally left blank
    }

    @Override
    public void removeConfigurationListener(AuditServiceConfigurationListener listener) {
        // this section intentionally left blank
    }

    @Override
    public AMAuditServiceConfiguration getDefaultConfiguration() {
        return new AMAuditServiceConfiguration(false);
    }

    @Override
    public AMAuditServiceConfiguration getRealmConfiguration(String realm) {
        return new AMAuditServiceConfiguration(false);
    }

    @Override
    public Set<AuditEventHandlerConfiguration> getDefaultEventHandlerConfigurations() {
        return null;
    }

    @Override
    public Set<AuditEventHandlerConfiguration> getRealmEventHandlerConfigurations(String realm) {
        return null;
    }

    @Override
    public EventTopicsMetaData getEventTopicsMetaData() {
        return null;
    }
}
