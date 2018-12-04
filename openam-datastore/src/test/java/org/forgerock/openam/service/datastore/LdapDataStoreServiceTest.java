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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static org.forgerock.openam.ldap.PersistentSearchChangeType.ADDED;
import static org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier.Type.DATA_STORE_ADDED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNotNull;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Set;

import javax.inject.Provider;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreService;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Action;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Unit test for {@link LdapDataStoreService}.
 *
 * @since 6.5.0
 */
public final class LdapDataStoreServiceTest {

    @Mock
    private ServiceConfigManagerFactory serviceManagerFactory;
    @Mock
    private ServiceConfigManager serviceConfigManager;
    @Mock
    private ServiceConfig globalServiceConfig;
    @Mock
    private ServiceConfig containerServiceConfig;
    @Mock
    private ServiceConfig dataStoreSubConfig;

    @Mock
    private ShutdownManager shutdownManager;
    @Mock
    private Provider<ConnectionFactory> defaultConnectionFactoryProvider;
    @Mock
    private LdapConnectionFactoryProvider connectionFactoryProvider;
    @Mock
    private ConnectionFactory defaultConnectionFactory;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private DataStoreServiceChangeNotifier changeNotifier;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private VolatileActionConsistencyController consistencyController;
    @Mock
    private Runnable refreshDataLayer;

    @Captor
    private ArgumentCaptor<ShutdownListener> shutdownListenerCaptor;
    @Captor
    private ArgumentCaptor<Action<?>> volatileActionCaptor;

    private DataStoreService dataStoreService;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        given(defaultConnectionFactoryProvider.get()).willReturn(defaultConnectionFactory);
        given(connectionFactoryProvider.createLdapConnectionFactory(notNull())).willReturn(connectionFactory);

        given(serviceManagerFactory.create(eq("amDataStoreService"), eq("1.0"))).willReturn(serviceConfigManager);
        given(serviceConfigManager.getGlobalConfig("default")).willReturn(globalServiceConfig);
        given(globalServiceConfig.getSubConfig("dataStoreContainer")).willReturn(containerServiceConfig);
        given(containerServiceConfig.getSubConfig("some-data-store")).willReturn(dataStoreSubConfig);
        given(dataStoreSubConfig.getAttributes()).willReturn(ImmutableMap
                .<String, Set<String>>builder()
                .put("serverHostname", Collections.singleton("some.host.name"))
                .put("bindDN", Collections.singleton("cn=Some User"))
                .put("bindPassword", Collections.singleton("secret"))
                .put("serverPort", Collections.singleton("1389"))
                .put("useSsl", Collections.singleton("false"))
                .put("useStartTls", Collections.singleton("false"))
                .put("minimumConnectionPool", Collections.singleton("1"))
                .put("maximumConnectionPool", Collections.singleton("10"))
                .build());

        dataStoreService = new LdapDataStoreService(defaultConnectionFactoryProvider, connectionFactoryProvider,
                serviceManagerFactory, shutdownManager, Collections.singleton(changeNotifier),
                realmLookup, consistencyController, refreshDataLayer, null);
    }

    @Test
    public void whenPassedDefaultDataStoreIdTheDefaultConnectionFactoryReturned() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.DEFAULT).getConnection();

        // Then
        verify(defaultConnectionFactory).getConnection();
    }

    @Test
    public void whenDefaultConnectionRequestedTwiceTheFactoryIsCached() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.DEFAULT).getConnection();
        dataStoreService.getConnectionFactory(DataStoreId.DEFAULT).getConnection();

        // Then
        verify(defaultConnectionFactory, times(2)).getConnection();
        verify(defaultConnectionFactoryProvider, times(1)).get();
    }

    @Test
    public void whenConnectionRequestedConnectionFactoryIsCreated() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.of("some-data-store")).getConnection();

        // Then
        verify(connectionFactory).getConnection();
    }

    @Test
    public void whenConnectionRequestedTwiceConnectionFactoryIsCached() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.of("some-data-store")).getConnection();
        dataStoreService.getConnectionFactory(DataStoreId.of("some-data-store")).getConnection();

        // Then
        verify(connectionFactory, times(2)).getConnection();
        verify(connectionFactoryProvider, times(1)).createLdapConnectionFactory(notNull());
    }

    @Test
    public void whenDataStoreChangeResultingActionsAreRoutedViaTheConsistencyController() {
        // Given
        ServiceListener serviceAsListener = (ServiceListener) dataStoreService;

        // When
        serviceAsListener.globalConfigChanged("amDataStoreService", null, null, "some-data-store", ADDED);

        // Then
        verify(consistencyController).safeExecuteVolatileAction(isNotNull());
    }

    @Test
    public void whenDataStoreChangeTheChangeNotifiersAreCalled() throws Exception {
        // Given
        ServiceListener serviceAsListener = (ServiceListener) dataStoreService;

        // When
        serviceAsListener.globalConfigChanged("amDataStoreService", null, null, "some-data-store", ADDED);
        verify(consistencyController).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        changeNotifierAction.run();

        // Then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("some-data-store"), DATA_STORE_ADDED);
        verify(refreshDataLayer).run();
    }

    @Test(expectedExceptions = DataStoreException.class, expectedExceptionsMessageRegExp = ".*shutting down.*")
    public void whenServiceIsShutdownFurtherRequestsThrowException() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();

        // When
        listener.shutdown();
        dataStoreService.getConnectionFactory(DataStoreId.of("some-data-store"));
    }

    @Test
    public void whenServiceIsShutdownDefaultFactoryIsClosed() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();

        // When
        dataStoreService.getConnectionFactory(DataStoreId.DEFAULT);
        listener.shutdown();

        // Then
        verify(defaultConnectionFactory).close();
    }

    @Test
    public void whenServiceIsShutdownAllConnectionFactoriesAreClosed() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();

        // When
        dataStoreService.getConnectionFactory(DataStoreId.of("some-data-store"));
        listener.shutdown();

        // Then
        verify(connectionFactory).close();
    }

}