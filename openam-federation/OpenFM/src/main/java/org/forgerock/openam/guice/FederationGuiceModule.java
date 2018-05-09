/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.guice;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;

import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;

/**
 * Responsible for declaring the bindings required for the federation code base.
 */
public class FederationGuiceModule extends PrivateModule {

    /**
     * Tag for all operations for use within the federation session management code. e.g. Thread names, Debugger etc.
     */
    public static final String FEDERATION_SESSION_MANAGEMENT = "FederationSessionManagement";

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @Inject
    @Exposed
    @Named(FEDERATION_SESSION_MANAGEMENT)
    public ScheduledExecutorService getFederationScheduledService(AMExecutorServiceFactory esf) {
        return esf.createCancellableScheduledService(1, FEDERATION_SESSION_MANAGEMENT);
    }
}
