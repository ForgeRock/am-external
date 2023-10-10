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
 * Copyright 2018-2023 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.google.inject.name.Names.named;

import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.secrets.config.SecretStoreConfigChangeListener;
import org.forgerock.openam.services.datastore.DataStoreConsistencyController;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.services.datastore.DataStoreService;
import org.forgerock.opendj.ldap.ConnectionFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.iplanet.services.naming.ServiceListeners;
import com.sun.identity.sm.DataStoreInitializer;
import com.sun.identity.sm.SMSNotificationManager;

/**
 * Guice module to bind the data store features together.
 *
 * @since 6.0.0
 */
public class DataStoreGuiceModule extends AbstractModule {
    public static final String SERVICE_DATA_STORE_ID_ATTRIBUTE_NAMES = "service-datastore-id-attributes";

    @Override
    protected void configure() {
        bind(ConnectionFactory.class).toProvider(DefaultConnectionFactoryProvider.class);
        bind(DataStoreLookup.class).to(SmsDataStoreLookup.class);

        bindDataStoreService();
        bind(DataStoreServiceRegister.class).to(LdapDataStoreService.class);

        bind(VolatileActionConsistencyController.class).to(ReentrantVolatileActionConsistencyController.class);
        bind(DataStoreConsistencyController.class).to(ReentrantVolatileActionConsistencyController.class);

        MapBinder.newMapBinder(binder(), String.class, String.class, named(SERVICE_DATA_STORE_ID_ATTRIBUTE_NAMES));

        Multibinder.newSetBinder(binder(), SecretStoreConfigChangeListener.class).addBinding()
                .to(LdapDataStoreService.class);
    }

    /**
     * Protected method to allow overriding by {@code MockDataStoreServiceDataStoreGuiceModule} which is used by
     * {@code MockAM}.
     */
    protected void bindDataStoreService() {
        bind(DataStoreService.class).to(LdapDataStoreService.class);
    }

    @Provides
    @Singleton
    public DataStoreInitializer dataStoreInitializer(DefaultDataStoreInitializer dataStoreInitializer,
            ServiceListeners serviceListeners) {
        return new CachingDataStoreInitializer(dataStoreInitializer, serviceListeners);
    }

    @Provides
    @Named("refresh-data-layer")
    public Runnable getRefreshDataLayerAction() {
        return () -> SMSNotificationManager.getInstance().allObjectsChanged();
    }
}
