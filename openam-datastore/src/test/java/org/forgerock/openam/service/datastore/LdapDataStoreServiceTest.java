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

import static com.sun.identity.sm.ServiceListener.MODIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.ldap.PersistentSearchChangeType.ADDED;
import static org.forgerock.openam.ldap.PersistentSearchChangeType.REMOVED;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.CONTAINER_CONFIG_NAME;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.DATA_STORE_ENABLED;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.SERVICE_NAME;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.SERVICE_VERSION;
import static org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier.Type.DATA_STORE_ADDED;
import static org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier.Type.DATA_STORE_REMOVED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;
import java.util.Set;

import javax.inject.Provider;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.ldap.ConnectionConfig;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.forgerock.openam.test.rules.LoggerRule;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Action;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Unit test for {@link LdapDataStoreService}.
 *
 * @since 6.5.0
 */
@RunWith(MockitoJUnitRunner.class)
public final class LdapDataStoreServiceTest {

    public static final String DISABLED_CONFIG_STORE_NAME = "disabled-config-store";
    public static final String ENABLED_CONFIG_STORE_NAME = "enabled-config-store";
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
    private Provider<ConnectionFactory> configConnectionFactoryProvider;
    @Mock
    private LdapConnectionFactoryProvider connectionFactoryProvider;
    @Mock
    private ConnectionFactory configConnectionFactory;
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
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DataStoreConfigFactory dataStoreConfigFactory;
    @Mock
    private PurposeMapping mapping;
    @Mock
    private ConnectionConfig connectionConfig;
    @Mock
    private SecretStoreWithMappings secretStore;
    @Mock
    private Multiple<PurposeMapping> multiple;

    @Captor
    private ArgumentCaptor<ShutdownListener> shutdownListenerCaptor;
    @Captor
    private ArgumentCaptor<Action<?>> volatileActionCaptor;

    @Rule
    public LoggerRule loggerRule = new LoggerRule(LdapDataStoreService.class, Level.ERROR);

    private LdapDataStoreService dataStoreService;

    @Before
    public void setup() throws Exception {
        given(configConnectionFactoryProvider.get()).willReturn(configConnectionFactory);
        given(connectionFactoryProvider.createLdapConnectionFactory(notNull())).willReturn(connectionFactory);

        given(serviceManagerFactory.create(eq(SERVICE_NAME), eq(SERVICE_VERSION))).willReturn(serviceConfigManager);
        given(serviceConfigManager.getGlobalConfig("default")).willReturn(globalServiceConfig);
        given(globalServiceConfig.getSubConfig(CONTAINER_CONFIG_NAME)).willReturn(containerServiceConfig);
        given(containerServiceConfig.getSubConfig(ENABLED_CONFIG_STORE_NAME)).willReturn(dataStoreSubConfig);
        given(dataStoreSubConfig.getAttributes()).willReturn(ConfigurationAttributesFactory.create(ImmutableMap
                .<String, Set<String>>builder()
                .put("serverUrls", Collections.singleton("[0]=some.host.name:1389"))
                .put("bindDN", Collections.singleton("cn=Some User"))
                .put("bindPassword", Collections.singleton("secret"))
                .put("useSsl", Collections.singleton("false"))
                .put("useStartTls", Collections.singleton("false"))
                .put("minimumConnectionPool", Collections.singleton("1"))
                .put("maximumConnectionPool", Collections.singleton("10"))
                .put(DATA_STORE_ENABLED, Set.of("true"))
                .build()));

        dataStoreService = new LdapDataStoreService(configConnectionFactoryProvider, connectionFactoryProvider,
                serviceManagerFactory, shutdownManager, Collections.singleton(changeNotifier),
                realmLookup, consistencyController, refreshDataLayer, null, dataStoreConfigFactory);
    }

    @Test
    public void whenPassedConfigDataStoreIdTheConfigConnectionFactoryReturned() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.CONFIG).getConnection();

        // Then
        verify(configConnectionFactory).getConnection();
    }

    @Test
    public void whenConfigConnectionRequestedTwiceTheFactoryIsCached() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.CONFIG).getConnection();
        dataStoreService.getConnectionFactory(DataStoreId.CONFIG).getConnection();

        // Then
        verify(configConnectionFactory, times(2)).getConnection();
        verify(configConnectionFactoryProvider, times(1)).get();
    }

    @Test
    public void whenConnectionRequestedConnectionFactoryIsCreated() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME)).getConnection();

        // Then
        verify(connectionFactory).getConnection();
    }

    @Test
    public void whenConnectionRequestedTwiceConnectionFactoryIsCached() throws LdapException {
        // When
        dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME)).getConnection();
        dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME)).getConnection();

        // Then
        verify(connectionFactory, times(2)).getConnection();
        verify(connectionFactoryProvider, times(1)).createLdapConnectionFactory(notNull());
    }

    @Test
    public void whenDataStoreChangeResultingActionsAreRoutedViaTheConsistencyController() {
        // Given
        ServiceListener serviceAsListener = (ServiceListener) dataStoreService;

        // When
        serviceAsListener.globalConfigChanged("amDataStoreService", null, null, ENABLED_CONFIG_STORE_NAME, ADDED);

        // Then
        verify(consistencyController).safeExecuteVolatileAction(isNotNull());
    }

    @Test
    public void whenDataStoreChangeTheChangeNotifiersAreCalled() throws Exception {
        // Given
        ServiceListener serviceAsListener = (ServiceListener) dataStoreService;

        // When
        serviceAsListener.globalConfigChanged("amDataStoreService", null, null, ENABLED_CONFIG_STORE_NAME, ADDED);
        verify(consistencyController).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        changeNotifierAction.run();

        // Then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of(ENABLED_CONFIG_STORE_NAME), DATA_STORE_ADDED);
        verify(refreshDataLayer).run();
    }

    @Test
    public void whenServiceIsShutdownFurtherRequestsThrowException() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();
        listener.shutdown();

        // When - Then
        assertThatThrownBy(() -> dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME)))
                .isInstanceOf(DataStoreException.class)
                .hasMessage("Service is shutting down");
    }

    @Test
    public void whenServiceIsShutdownConfigFactoryIsClosed() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();

        // When
        dataStoreService.getConnectionFactory(DataStoreId.CONFIG);
        listener.shutdown();

        // Then
        verify(configConnectionFactory).close();
    }

    @Test
    public void whenServiceIsShutdownAllConnectionFactoriesAreClosed() {
        // Given
        verify(shutdownManager).addShutdownListener(shutdownListenerCaptor.capture());
        ShutdownListener listener = shutdownListenerCaptor.getValue();

        // When
        dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME));
        listener.shutdown();

        // Then
        verify(connectionFactory).close();
    }

    @Test
    public void shouldNotNotifyGlobalChangesGivenDisabledDataStoreConfigAdded() throws Exception {
        // Given
        setupMockDisabledConfig();

        // When
        ((ServiceListener) dataStoreService).globalConfigChanged("amDataStoreService", null, null,
                DISABLED_CONFIG_STORE_NAME, ADDED);

        // Then
        verify(consistencyController).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        changeNotifierAction.run();
        verify(changeNotifier, times(0)).notifyGlobalChanges(DataStoreId.of(DISABLED_CONFIG_STORE_NAME),
                DATA_STORE_ADDED);
    }

    @Test
    public void shouldNotNotifyGlobalChangesGivenDisabledDataStoreConfigModified() throws Exception {
        // Given
        setupMockDisabledConfig();

        // When
        ((ServiceListener) dataStoreService).globalConfigChanged("amDataStoreService", null, null,
                DISABLED_CONFIG_STORE_NAME, MODIFIED);

        // Then
        verify(consistencyController, times(1)).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        changeNotifierAction.run();
        verify(changeNotifier, times(1)).notifyGlobalChanges(DataStoreId.of(DISABLED_CONFIG_STORE_NAME),
                DATA_STORE_REMOVED);
        verify(changeNotifier, times(0)).notifyGlobalChanges(DataStoreId.of(DISABLED_CONFIG_STORE_NAME),
                DATA_STORE_ADDED);
    }

    @Test
    public void shouldNotifyGlobalChangesGivenDisabledDataStoreConfigRemoved() throws Exception {
        // Given
        setupMockDisabledConfig();

        // When
        ((ServiceListener) dataStoreService).globalConfigChanged("amDataStoreService", null, null,
                DISABLED_CONFIG_STORE_NAME, REMOVED);

        // Then
        verify(consistencyController).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        // The following `assertThatThrownBy()` exists purely for the purpose of avoiding the mocking of the static calls
        // that are present in `org.forgerock.openam.service.datastore.LdapDataStoreService.resetDataStoreToDefault`
        assertThatThrownBy(changeNotifierAction::run).isInstanceOf(NullPointerException.class);
        verify(changeNotifier, times(1)).notifyGlobalChanges(DataStoreId.of(DISABLED_CONFIG_STORE_NAME),
                DATA_STORE_REMOVED);
    }

    @Test
    public void shouldNotifyGlobalChangesGivenEnabledDataStoreConfigModified() throws Exception {
        // Given
        setUpMockEnabledConfig();

        // When
        ((ServiceListener) dataStoreService).globalConfigChanged("amDataStoreService", null, null,
                ENABLED_CONFIG_STORE_NAME, MODIFIED);

        // Then
        verify(consistencyController, times(1)).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();
        changeNotifierAction.run();
        verify(changeNotifier, times(1)).notifyGlobalChanges(DataStoreId.of(ENABLED_CONFIG_STORE_NAME),
                DATA_STORE_REMOVED);
        verify(changeNotifier, times(1)).notifyGlobalChanges(DataStoreId.of(ENABLED_CONFIG_STORE_NAME),
                DATA_STORE_ADDED);
        verify(connectionFactory, times(1)).close();
    }

    @Test
    public void shouldNotifyGlobalChangesGivenEnabledDataStoreConfigRemovedFromCacheAndCleared() throws Exception {
        // Given
        setUpMockEnabledConfig();

        // When
        ((ServiceListener) dataStoreService).globalConfigChanged("amDataStoreService", null, null,
                ENABLED_CONFIG_STORE_NAME, MODIFIED);

        // Then
        verify(consistencyController, times(1)).safeExecuteVolatileAction(volatileActionCaptor.capture());
        Action<?> changeNotifierAction = volatileActionCaptor.getValue();

        /* This test is an edge case where we need to raise another
        enhancement to tidy the removeFromCacheAndClose method */
        changeNotifierAction.run();
        changeNotifierAction.run();
        assertThat(loggerRule.getErrors(ILoggingEvent::getFormattedMessage).size()).isEqualTo(1);
        assertThat(loggerRule.getErrors(ILoggingEvent::getFormattedMessage))
                .containsExactly("Failed to identify and remove the connection " +
                        "factory from the cache, clearing the entire cache");
    }

    @Test
    public void shouldGetConfigGivenDataStoreExistsAndMTLSDisabled() throws SMSException, SSOException {
        //Given
        setUpMockConfig(ENABLED_CONFIG_STORE_NAME, "true", "false");

        //When
        ConnectionConfig config = dataStoreService.getConfig(DataStoreId.of(ENABLED_CONFIG_STORE_NAME));

        //Then
        assertThat(config.isMtlsEnabled()).isFalse();
        assertThat(config.getBindDN()).isEqualTo("cn=Some User");
        assertThat(config.getMtlsSecretId()).isEqualTo("am.external.datastore.test.cert.mtls.cert");
    }

    @Test
    public void shouldFailToGetConfigGivenDataStoreDoesNotExists() {
        //Given
        DataStoreId nonExistentDatastore = DataStoreId.of("NON_EXISTENT_DATASTORE");

        // When Then
        assertThatThrownBy(() ->
                dataStoreService.getConfig(nonExistentDatastore))
                .isExactlyInstanceOf(DataStoreException.class)
                .hasMessage("Unable to retrieve the sub config for the data store");
    }

    @Test
    public void shouldGetConfigGivenDataStoreExistsAndMTLSEnabled() throws SMSException, SSOException {
       //Given
        setUpMockConfig("mtls-enabled-config-store", "true", "true");

        //When
        ConnectionConfig config = dataStoreService.getConfig(DataStoreId.of("mtls-enabled-config-store"));

        //Then
        assertThat(config.isMtlsEnabled()).isTrue();
        assertThat(config.getMtlsSecretId()).isEqualTo("am.external.datastore.test.cert.mtls.cert");
        assertThat(config.getBindPassword()).isEqualTo("secret".toCharArray());
    }

    @Test
    public void shouldFailToGetConnectionFactoryGivenDisabledConfig() throws Exception {
        // Given
        setupMockDisabledConfig();

        // When - Then
        assertThatThrownBy(() -> dataStoreService.getConnectionFactory(
                DataStoreId.of(DISABLED_CONFIG_STORE_NAME)).getConnection())
                .isInstanceOf(DataStoreException.class)
                .hasMessage("Unable to create connection factory for disabled data store");
    }

    private void setupMockDisabledConfig() throws SMSException, SSOException {
        setUpMockConfig(DISABLED_CONFIG_STORE_NAME, "false", "false");
    }

    private void setUpMockEnabledConfig() throws SMSException, SSOException, LdapException {
        setUpMockConfig(ENABLED_CONFIG_STORE_NAME, "true", "false");
        dataStoreService.getConnectionFactory(DataStoreId.of(ENABLED_CONFIG_STORE_NAME)).getConnection();
    }

    private void setUpMockConfig(String storeConfigName, String configEnabled, String mtlsEnabled) throws SMSException, SSOException {
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(serviceManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);

        ServiceConfig globalConfig = mock(ServiceConfig.class);
        given(scm.getGlobalConfig("default")).willReturn(globalConfig);

        ServiceConfig containerConfig = mock(ServiceConfig.class);
        given(globalConfig.getSubConfig(CONTAINER_CONFIG_NAME)).willReturn(containerConfig);
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(storeConfigName));

        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(storeConfigName)).willReturn(serviceConfig);
        ConfigurationAttributes configurationAttributes = ConfigurationAttributesFactory.create(ImmutableMap
                .<String, Set<String>>builder()
                .put("serverUrls", Collections.singleton("[0]=some.host.name:1389"))
                .put("bindDN", Collections.singleton("cn=Some User"))
                .put("bindPassword", Collections.singleton("secret"))
                .put("useSsl", Collections.singleton("false"))
                .put("useStartTls", Collections.singleton("false"))
                .put("minimumConnectionPool", Collections.singleton("1"))
                .put("maximumConnectionPool", Collections.singleton("10"))
                .put(DATA_STORE_ENABLED, Set.of(configEnabled))
                .put("mtlsEnabled", Set.of(mtlsEnabled))
                .put("mtlsSecretLabel", Collections.singleton("test.cert"))
                .build());
        given(serviceConfig.getAttributes()).willReturn(configurationAttributes);
    }

    @Test
    public void assertSecretStoreMappingHasChangedOrgNameNotNullReturnsWithNoAction() {
        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, "ou=config", 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedNullMappingButNotARemoveReturnsWithNoAction() {
        // when
        dataStoreService.secretStoreMappingHasChanged(null, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenGetGlobalConfigThrowsWillReturnWithNoAction()
            throws SMSException, SSOException {
        //given
        given(serviceConfigManager.getGlobalConfig("default")).willThrow(SMSException.class);

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenNoEnabledConfigWillReturnWithNoAction()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "false", "false");

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenNoMtlsEnabledConfigWillReturnWithNoAction()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "false", "true");

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenIsDeleteWithNoMappingsWithRefreshAllMtlsEnabledConnections()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");

        // when
        dataStoreService.secretStoreMappingHasChanged(null, null, 2);

        // then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_REMOVED);
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_ADDED);
        verifyNoMoreInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenMappingIsNotExternalDataStoreMtlsCertificateReturnsWithNoAction()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");
        given(mapping.secretId()).willReturn("another.type.of.secret");

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenMappingIsADifferentExternalDataStoreMtlsCertificateReturnsWithNoAction()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");
        given(mapping.secretId()).willReturn("am.external.datastore.pineapple.mtls.cert");

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreMappingHasChangedGivenMappingIsTheSameRefreshesTheConnection()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");
        given(mapping.secretId()).willReturn("am.external.datastore.test.cert.mtls.cert");

        // when
        dataStoreService.secretStoreMappingHasChanged(mapping, null, 4);

        // then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_REMOVED);
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_ADDED);
        verifyNoMoreInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreHasChangedGivenRealmAddedWillReturnWithoutAction() {

        // when
        dataStoreService.secretStoreHasChanged(secretStore, "ou=config", 1);

        // then
        verifyNoInteractions(changeNotifier);
    }


    @Test
    public void assertSecretStoreHasChangedGivenChangeIsAddedWillReturnWithoutAction() {

        // when
        dataStoreService.secretStoreHasChanged(secretStore, null, 1);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreHasChangedGivenRealmNotNullWillReturnWithNoAction() {

        // when
        dataStoreService.secretStoreHasChanged(secretStore, "ou=config", 2);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreHasChangedGivenSecretStoreNullButNotRemoveWillReturnWithNoAction() {

        // when
        dataStoreService.secretStoreHasChanged(null, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreHasChangedGivenIsRemoveWillRefreshConnection() throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");

        // when
        dataStoreService.secretStoreHasChanged(null, null, 2);

        // then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_REMOVED);
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_ADDED);
        verifyNoMoreInteractions(changeNotifier);
    }

    @Test
    public void assertSecretStoreHasChangedGivenGetWillThrowThenWillReturnWithNoAction()
            throws SMSException, SSOException {
        //given
        given(secretStore.mappings()).willReturn(multiple);
        given(multiple.get(QueryFilter.alwaysTrue())).willThrow(SSOException.class);

        // when
        dataStoreService.secretStoreHasChanged(secretStore, null, 4);

        // then
        verifyNoInteractions(changeNotifier);
    }


    @Test
    public void assertSecretStoreHasChangedGivenSecretStoreHasMatchingMappingWillRefreshConnection()
            throws SMSException, SSOException {
        //given
        setUpMockConfig("data-store", "true", "true");
        given(secretStore.mappings()).willReturn(multiple);
        given(multiple.get(QueryFilter.alwaysTrue())).willReturn(Set.of(mapping));
        given(mapping.secretId()).willReturn("am.external.datastore.test.cert.mtls.cert");

        // when
        dataStoreService.secretStoreHasChanged(secretStore, null, 4);

        // then
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_REMOVED);
        verify(changeNotifier).notifyGlobalChanges(DataStoreId.of("data-store"), DATA_STORE_ADDED);
        verifyNoMoreInteractions(changeNotifier);
    }

}
