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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.service.datastore.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ConfigurationAttributesFactory;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Unit tests for {@link DataStoreSecretIdProvider}.
 */
@ExtendWith(MockitoExtension.class)
public class DataStoreSecretIdProviderTest {
    private static final String SERVICE_NAME = "amDataStoreService";
    private static final String SERVICE_VERSION = "1.0";
    private static final String SUB_CONFIG_NAME = "dataStoreContainer";

    private static final String DEFAULT_INSTANCE = "default";
    private static final String SUB_CONFIG_STORE = "sub-config-store";
    private static final String EXTERNAL_DATASTORE_MTLS_CERT = "am.external.datastore.%s.mtls.cert";
    private static final String EXTERNAL_DATA_STORE = "externalDataStore";
    private static final String TEST_DATASTORE_CERT = "test.datastore.cert";
    private static final String MTLS_ENABLED = "mtlsEnabled";
    private static final String MTLS_SECRET_LABEL = "mtlsSecretLabel";

    @Mock
    private ServiceConfigManagerFactory serviceConfigManagerFactory;

    @InjectMocks
    private DataStoreSecretIdProvider dataStoreSecretIdProvider;

    @Test
    void shouldGetGlobalMultiInstanceSecretIdsGivenADataStoreWithMTLSCertLabel() throws Exception {
        // Given
        Multimap<String, String> testMap = ImmutableMultimap.<String, String>builder()
                .putAll(EXTERNAL_DATA_STORE,
                        Set.of(String.format(EXTERNAL_DATASTORE_MTLS_CERT, TEST_DATASTORE_CERT)))
                .build();

        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(SUB_CONFIG_STORE));

        ServiceConfig subConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(SUB_CONFIG_STORE)).willReturn(subConfig);

        given(subConfig.getAttributes()).willReturn(createMtlsConfigurationAttributes(TEST_DATASTORE_CERT));

        // When
        Multimap<String, String> globalMultiInstanceSecretIds = dataStoreSecretIdProvider
                .getGlobalMultiInstanceSecretIds(null);

        // Then
        assertThat(globalMultiInstanceSecretIds).isEqualTo(testMap);
    }

    @Test
    void shouldGetGlobalMultiInstanceSecretIdsGivenMultipleDataStoresWithMTLSCertLabel() throws Exception {
        // Given
        String secondTestCertLabel = "second.test.datastore.cert";
        String secondSubConfigStore = "second-sub-config-store";

        Multimap<String, String> testMap = ImmutableMultimap.<String, String>builder()
                .putAll(EXTERNAL_DATA_STORE,
                        Set.of(String.format(EXTERNAL_DATASTORE_MTLS_CERT, secondTestCertLabel)))
                .putAll(EXTERNAL_DATA_STORE,
                        Set.of(String.format(EXTERNAL_DATASTORE_MTLS_CERT, TEST_DATASTORE_CERT)))
                .build();

        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames())
                .willReturn(Set.of(SUB_CONFIG_STORE, secondSubConfigStore));

        ServiceConfig subConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(SUB_CONFIG_STORE)).willReturn(subConfig);

        ServiceConfig secondTestConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(secondSubConfigStore)).willReturn(secondTestConfig);

        given(subConfig.getAttributes())
                .willReturn(createMtlsConfigurationAttributes(TEST_DATASTORE_CERT));
        given(secondTestConfig.getAttributes())
                .willReturn(createMtlsConfigurationAttributes(secondTestCertLabel));

        // When
        Multimap<String, String> globalMultiInstanceSecretIds = dataStoreSecretIdProvider
                .getGlobalMultiInstanceSecretIds(null);

        // Then
        assertThat(globalMultiInstanceSecretIds).isEqualTo(testMap);
    }

    @Test
    void shouldNotGetGlobalMultiInstanceSecretIdsGivenADataStoreWithoutMTLSCertLabel() throws Exception {
        // Given
        Multimap<String, String> testMap = ImmutableMultimap.<String, String>builder()
                .build();

        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(SUB_CONFIG_STORE));

        ServiceConfig subConfig = mock(ServiceConfig.class);
        given(containerConfig.getSubConfig(SUB_CONFIG_STORE)).willReturn(subConfig);

        given(subConfig.getAttributes()).willReturn(createDefaultConfigurationAttributes());

        // When
        Multimap<String, String> globalMultiInstanceSecretIds = dataStoreSecretIdProvider
                .getGlobalMultiInstanceSecretIds(null);

        // Then
        assertThat(globalMultiInstanceSecretIds).isEqualTo(testMap);
    }

    @Test
    void shouldFailToGetGlobalMultiInstanceSecretIdsGivenInvalidSubConfig() throws Exception {
        // Given
        Multimap<String, String> testMap = ImmutableMultimap.<String, String>builder()
                .build();

        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willReturn(Set.of(SUB_CONFIG_STORE));

        given(containerConfig.getSubConfig(SUB_CONFIG_STORE)).willThrow(new SMSException());

        // When
        Multimap<String, String> globalMultiInstanceSecretIds = dataStoreSecretIdProvider
                .getGlobalMultiInstanceSecretIds(null);

        // Then
        assertThat(globalMultiInstanceSecretIds).isEqualTo(testMap);
    }

    @Test
    void shouldFailToGetGlobalMultiInstanceSecretIdsGivenInvalidContainerConfig() throws Exception {
        // Given
        Multimap<String, String> testMap = ImmutableMultimap.<String, String>builder()
                .build();

        ServiceConfig containerConfig = setupMockContainerConfig();
        given(containerConfig.getSubConfigNames()).willThrow(new SMSException());

        // When
        Multimap<String, String> globalMultiInstanceSecretIds = dataStoreSecretIdProvider
                .getGlobalMultiInstanceSecretIds(null);

        // Then
        assertThat(globalMultiInstanceSecretIds).isEqualTo(testMap);
    }

    private ServiceConfig setupMockContainerConfig() throws SMSException, SSOException {
        ServiceConfigManager scm = mock(ServiceConfigManager.class);
        given(serviceConfigManagerFactory.create(SERVICE_NAME, SERVICE_VERSION)).willReturn(scm);

        ServiceConfig globalConfig = mock(ServiceConfig.class);
        given(scm.getGlobalConfig(DEFAULT_INSTANCE)).willReturn(globalConfig);

        ServiceConfig containerConfig = mock(ServiceConfig.class);
        given(globalConfig.getSubConfig(SUB_CONFIG_NAME)).willReturn(containerConfig);
        return containerConfig;
    }

    private ConfigurationAttributes createMtlsConfigurationAttributes(String secretLabel) {
        ConfigurationAttributes configurationAttributes = createDefaultConfigurationAttributes();
        configurationAttributes.put(MTLS_ENABLED, Collections.singleton("true"));
        configurationAttributes.put(MTLS_SECRET_LABEL, Collections.singleton(secretLabel));

        return configurationAttributes;
    }

    private ConfigurationAttributes createDefaultConfigurationAttributes() {
        return ConfigurationAttributesFactory.create(ImmutableMap
                .<String, Set<String>>builder()
                .put("serverUrls", Collections.singleton("[0]=some.host.name:1389"))
                .put("bindDN", Collections.singleton("cn=Some User"))
                .put("bindPassword", Collections.singleton("secret"))
                .put("useSsl", Collections.singleton("false"))
                .put("useStartTls", Collections.singleton("false"))
                .put("minimumConnectionPool", Collections.singleton("1"))
                .put("maximumConnectionPool", Collections.singleton("10"))
                .put("dataStoreEnabled", Set.of("false"))
                .build());
    }
}
