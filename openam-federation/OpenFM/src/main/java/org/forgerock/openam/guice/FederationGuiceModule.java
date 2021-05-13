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
 * Copyright 2016-2020 ForgeRock AS.
 */
package org.forgerock.openam.guice;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.federation.config.Saml2DataStoreListener;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.saml2.plugins.SecretsSaml2CredentialResolver;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;

import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

/**
 * Responsible for declaring the bindings required for the federation code base.
 */
public class FederationGuiceModule extends AbstractModule {

    /**
     * Tag for all operations for use within the federation session management code. e.g. Thread names, Debugger etc.
     */
    public static final String FEDERATION_SESSION_MANAGEMENT = "FederationSessionManagement";

    @Override
    protected void configure() {
        bind(Saml2CredentialResolver.class).to(SecretsSaml2CredentialResolver.class);
        Multibinder.newSetBinder(binder(), DataStoreServiceChangeNotifier.class)
                .addBinding().to(Saml2DataStoreListener.class);
    }

    @Provides
    @Singleton
    @Named(FEDERATION_SESSION_MANAGEMENT)
    public ScheduledExecutorService getFederationScheduledService(AMExecutorServiceFactory esf) {
        return esf.createCancellableScheduledService(1, FEDERATION_SESSION_MANAGEMENT);
    }
}
