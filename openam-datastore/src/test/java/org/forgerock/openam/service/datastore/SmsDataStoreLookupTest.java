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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.SERVICE_NAME;
import static org.forgerock.openam.service.datastore.SmsDataStoreLookup.SERVICE_VERSION;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.service.datastore.SmsDataStoreLookup.DataStoreIdsBuilder;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.DataStoreInitializer;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.SmsWrapperObject;

/**
 * Unit test for {@link SmsDataStoreLookup}.
 *
 * @since 6.5.0
 */
public final class SmsDataStoreLookupTest {

    public static final String DEFAULT_INSTANCE = "default";
    public static final String CONFIG_WITHOUT_ENABLED_ATTRIBUTE = "configwithoutenabledattribute";
    public static final String ENABLED_CONFIG_STORE = "enabled-config-store";
    public static final String DISABLED_CONFIG_STORE = "disabled-config-store";
    public static final DataStoreId DEFAULT_DS_ID = DataStoreId.CONFIG;
    @Mock
    private ConsoleConfigHandler configHandler;
    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private Realm realm;

    private MockedStatic<SmsWrapperObject> smsWrapperObjectMockedStatic;

    private DataStoreLookup dataStoreLookup;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        smsWrapperObjectMockedStatic = Mockito.mockStatic(SmsWrapperObject.class);

        dataStoreLookup = new SmsDataStoreLookup(configHandler, configManagerFactory,
                singletonMap("myService", "myAttribute"), mock(DataStoreInitializer.class));

        DataStoreIdsBuilder builder = new DataStoreIdsBuilder();
        builder.withPolicyDataStoreId(DataStoreId.of("policy-data-store"));
        builder.withApplicationDataStoreId(DataStoreId.of("application-data-store"));

        given(configHandler.getConfig(eq("/some/realm"), eq(DataStoreIdsBuilder.class)))
                .willReturn(builder.build(Collections.emptyMap()));
        given(realm.asPath()).willReturn("/some/realm");
    }

    @AfterMethod
    public void teardown() {
        smsWrapperObjectMockedStatic.close();
    }

    @Test
    public void whenLookupCalledWithPolicyServicePolicyDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupRealmId("sunEntitlementService", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("policy-data-store"));
    }

    @Test
    public void whenLookupCalledWithPolicyIndexServicePolicyDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupRealmId("sunEntitlementIndexes", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("policy-data-store"));
    }

    @Test
    public void whenLookupCalledWithAgentServiceApplicationDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupRealmId("AgentService", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("application-data-store"));
    }

    @Test
    public void whenLookupCalledWithUnknownServiceConfigIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupRealmId("unknown-service", realm);

        // Then
        assertThat(id).isEqualTo(DEFAULT_DS_ID);
    }

    @Test
    public void whenLookupCalledWithRealmContainingNoConfigTheConfigIdReturned() {
        // Given
        Realm realm = mock(Realm.class);
        given(realm.asPath()).willReturn("/some/other/realm");

        // When
        DataStoreId id = dataStoreLookup.lookupRealmId("unknown-service", realm);

        // Then
        assertThat(id).isEqualTo(DEFAULT_DS_ID);
    }

    @Test
    public void whenGlobalLookupIsCalledForUnsupportedServiceConfigIsReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupGlobalId("other-service");

        // Then
        assertThat(id).isEqualTo(DEFAULT_DS_ID);
    }

    @Test
    public void whenGlobalLookupIsCalledForSupportedServiceConfigIsReturned() throws Exception {
        // Given
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);
        ServiceConfig sc = mock(ServiceConfig.class);
        given(scm.getGlobalConfig(DEFAULT_INSTANCE)).willReturn(sc);
        given(sc.getAttributeValue("myAttribute")).willReturn(Collections.singleton("my-store"));

        // When
        DataStoreId id = dataStoreLookup.lookupGlobalId("myService");

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("my-store"));
    }

    @Test
    public void whenGlobalLookupAllIsCalledWithoutConfiguredDatastoreOnlyDefaultIsReturned() throws Exception {
        // Given
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);
        ServiceConfig sc = mock(ServiceConfig.class);
        given(scm.getGlobalConfig(DEFAULT_INSTANCE)).willReturn(sc);
        given(sc.getAttributeValue("myAttribute")).willReturn(Collections.emptySet());

        // When
        Set<DataStoreId> ids = dataStoreLookup.lookupGlobalIds();

        // Then
        assertThat(ids).containsOnly(DEFAULT_DS_ID);
    }

    @Test
    public void whenGlobalLookupAllIsCalledWithConfiguredDatastoreOnlyConfigIsReturned() throws Exception {
        // Given
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);
        ServiceConfig sc = mock(ServiceConfig.class);
        given(scm.getGlobalConfig(DEFAULT_INSTANCE)).willReturn(sc);
        given(sc.getAttributeValue("myAttribute")).willReturn(Collections.singleton("my-datastore"));

        // When
        Set<DataStoreId> ids = dataStoreLookup.lookupGlobalIds();

        // Then
        assertThat(ids).containsOnly(DataStoreId.of("my-datastore"));
    }

    @Test
    public void shouldGetEnabledIdsGivenEmbeddedFbcDatastoreAndNoExternalDatastores() throws Exception {
        // Given
        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of());

        smsWrapperObjectMockedStatic.when(SmsWrapperObject::isFbcWithoutEmbeddedEnabled).thenReturn(false);

        // When
        Set<DataStoreId> ids = dataStoreLookup.getEnabledIds();

        // Then
        assertThat(ids).isEqualTo(Set.of(DEFAULT_DS_ID));
    }

    @Test
    public void shouldGetEnabledIds() throws Exception {
        // Given
        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(ENABLED_CONFIG_STORE, DISABLED_CONFIG_STORE));

        ServiceConfig enabledConfig = mock(ServiceConfig.class);
        ServiceConfig disabledConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(ENABLED_CONFIG_STORE)).willReturn(enabledConfig);
        given(containerConfig.getSubConfig(DISABLED_CONFIG_STORE)).willReturn(disabledConfig);

        given(enabledConfig.getAttributes()).willReturn(ConfigurationAttributesFactory
                .create(Map.of(SmsDataStoreLookup.DATA_STORE_ENABLED, Set.of("true"))));
        given(disabledConfig.getAttributes()).willReturn(ConfigurationAttributesFactory
                .create(Map.of(SmsDataStoreLookup.DATA_STORE_ENABLED, Set.of("false"))));

        smsWrapperObjectMockedStatic.when(SmsWrapperObject::isFbcWithoutEmbeddedEnabled).thenReturn(true);

        // When
        Set<DataStoreId> ids = dataStoreLookup.getEnabledIds();

        // Then
        assertThat(ids).isEqualTo(Set.of(DataStoreId.of(ENABLED_CONFIG_STORE)));
    }

    @Test
    public void shouldGetEnabledIdsGivenNoEnabledAttributePresent() throws Exception {
        // Given
        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(CONFIG_WITHOUT_ENABLED_ATTRIBUTE));

        ServiceConfig configWithoutEnabledAttributeConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(CONFIG_WITHOUT_ENABLED_ATTRIBUTE)).willReturn(configWithoutEnabledAttributeConfig);

        given(configWithoutEnabledAttributeConfig.getAttributes()).willReturn(ConfigurationAttributesFactory.create());

        smsWrapperObjectMockedStatic.when(SmsWrapperObject::isFbcWithoutEmbeddedEnabled).thenReturn(true);

        // When
        Set<DataStoreId> ids = dataStoreLookup.getEnabledIds();

        // Then
        assertThat(ids).isEqualTo(Set.of(DataStoreId.of(CONFIG_WITHOUT_ENABLED_ATTRIBUTE)));
    }

    @Test
    public void shouldGetEnabledIdsReturnEmptySetGivenNoDatastoreEnabled() throws Exception {
        // Given
        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(DISABLED_CONFIG_STORE));

        ServiceConfig disabledConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(DISABLED_CONFIG_STORE)).willReturn(disabledConfig);

        given(disabledConfig.getAttributes()).willReturn(ConfigurationAttributesFactory
                .create(Map.of(SmsDataStoreLookup.DATA_STORE_ENABLED, Set.of("false"))));

        smsWrapperObjectMockedStatic.when(SmsWrapperObject::isFbcWithoutEmbeddedEnabled).thenReturn(true);

        // When
        Set<DataStoreId> ids = dataStoreLookup.getEnabledIds();

        // Then
        assertThat(ids).isEqualTo(Set.of());
    }

    @Test
    public void shouldGetEnabledIdsReturnEmptySetGivenNoDatastore() throws Exception {
        // Given
        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of());

        smsWrapperObjectMockedStatic.when(SmsWrapperObject::isFbcWithoutEmbeddedEnabled).thenReturn(true);

        // When
        Set<DataStoreId> ids = dataStoreLookup.getEnabledIds();

        // Then
        assertThat(ids).isEqualTo(Set.of());
    }

    private ServiceConfig setupMockContainerConfig() throws SMSException, SSOException {
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(configManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);

        ServiceConfig globalConfig = mock(ServiceConfig.class);
        given(scm.getGlobalConfig(DEFAULT_INSTANCE)).willReturn(globalConfig);

        ServiceConfig containerConfig = mock(ServiceConfig.class);
        given(globalConfig.getSubConfig(SmsDataStoreLookup.CONTAINER_CONFIG_NAME)).willReturn(containerConfig);
        return containerConfig;
    }
}