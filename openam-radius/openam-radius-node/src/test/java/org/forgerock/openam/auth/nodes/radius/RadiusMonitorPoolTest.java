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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * Testing removal and creation of RadiusMonitor instances on config change events and startup.
 */
@ExtendWith(MockitoExtension.class)
class RadiusMonitorPoolTest {

    private static final String SERVICE_NAME = "RadiusDecisionNode";
    private static final String ROOT_REALM_PATH = "/";
    private static final String CHILD_REALM_PATH = "/child";

    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private MessageAuthenticatorCalculator messageAuthenticatorCalculator;
    @Mock
    private AnnotatedServiceRegistry annotatedServiceRegistry;
    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private SecretCache secretCache;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private ServiceConfigManager serviceConfigManager;
    @Mock
    private ServiceConfig rootServiceConfig;
    @Mock
    private TimerPool timerPool;
    @Mock
    private Realm realm;

    private RadiusMonitorPool pool;
    private MockedStatic<SystemTimerPool> systemTimerPoolStatic;
    private MockedStatic<Realms> realmsStatic;

    @BeforeEach
    void setUp() throws SMSException, SSOException {
        systemTimerPoolStatic = mockStatic(SystemTimerPool.class);
        realmsStatic = mockStatic(Realms.class);

        given(configManagerFactory.create(SERVICE_NAME)).willReturn(serviceConfigManager);
        systemTimerPoolStatic.when(SystemTimerPool::getTimerPool).thenReturn(timerPool);
        realmsStatic.when(Realms::root).thenReturn(realm);
        given(realm.asPath()).willReturn(ROOT_REALM_PATH);

        given(secretReferenceCache.realm(realm)).willReturn(secretCache);

        pool = new RadiusMonitorPool(configManagerFactory, messageAuthenticatorCalculator,
                annotatedServiceRegistry, secretReferenceCache, realmLookup);
    }

    @AfterEach
    void tearDown() {
        systemTimerPoolStatic.close();
        realmsStatic.close();
    }

    @Test
    void shouldCreateMonitorGivenAddedEvent() throws SMSException, SSOException {
        UUID nodeId = UUID.randomUUID();
        Purpose<GenericSecret> purpose = Purpose.purpose("radiusSecret", GenericSecret.class);
        RadiusDecisionNode.Config config = mock(RadiusDecisionNode.Config.class);

        // given
        setupRadiusConfigMock(config, purpose, 10, 3, true);
        given(annotatedServiceRegistry.getRealmInstance(RadiusDecisionNode.Config.class, realm, nodeId.toString()))
                .willReturn(Optional.of(config));
        given(secretCache.active(purpose))
                .willReturn(SecretReference.constant(GenericSecret.password("shared-one".toCharArray())));

        // when
        pool.organizationConfigChanged(SERVICE_NAME, "1.0", null, null,
                "realm-config/authentication/authenticationtrees/nodes/" + nodeId, ServiceListener.ADDED);

        // then
        assertThat(pool.contains(nodeId)).isTrue();
        assertThat(pool.get(nodeId)).isNotNull();
    }

    @Test
    void shouldRemoveMonitorGivenRemovedEvent() throws SMSException, SSOException {
        UUID nodeId = UUID.randomUUID();
        Purpose<GenericSecret> purpose = Purpose.purpose("radiusSecret", GenericSecret.class);
        RadiusDecisionNode.Config config = mock(RadiusDecisionNode.Config.class);

        // given
        setupRadiusConfigMock(config, purpose, 10, 3, true);
        given(annotatedServiceRegistry.getRealmInstance(RadiusDecisionNode.Config.class, realm, nodeId.toString()))
                .willReturn(Optional.of(config));
        given(secretCache.active(purpose))
                .willReturn(SecretReference.constant(GenericSecret.password("sh-secret".toCharArray())));

        // when
        pool.organizationConfigChanged(SERVICE_NAME, "1.0", null, null,
                "realm-config/authentication/authenticationtrees/nodes/" + nodeId, ServiceListener.ADDED);
        RadiusMonitor monitor = pool.get(nodeId);

        pool.organizationConfigChanged(SERVICE_NAME, "1.0", null, null,
                "realm-config/authentication/authenticationtrees/nodes/" + nodeId, ServiceListener.REMOVED);

        // then
        assertThat(monitor).isNotNull();
        assertThat(pool.contains(nodeId)).isFalse();
    }

    @Test
    void shouldNotCreateMonitorGivenMissingSecretLabel() throws SMSException, SSOException {
        UUID nodeId = UUID.randomUUID();
        Purpose<GenericSecret> purpose = Purpose.purpose("radiusSecret", GenericSecret.class);
        RadiusDecisionNode.Config config = mock(RadiusDecisionNode.Config.class);

        // given
        given(config.sharedSecretPurpose()).willReturn(Optional.of(purpose));
        given(annotatedServiceRegistry.getRealmInstance(RadiusDecisionNode.Config.class, realm, nodeId.toString()))
                .willReturn(Optional.of(config));
        given(secretCache.active(purpose)).willThrow(new IllegalStateException("Missing secret label"));

        // when
        pool.organizationConfigChanged(SERVICE_NAME, "1.0", null, null,
                "realm-config/authentication/authenticationtrees/nodes/" + nodeId, ServiceListener.ADDED);

        // then
        assertThat(pool.contains(nodeId)).isFalse();
    }

    @Test
    void shouldInitializeMonitorsGivenExistingConfigs() throws SMSException, SSOException, RealmLookupException {
        // Given
        UUID rootNodeId = UUID.randomUUID();
        UUID childNodeId = UUID.randomUUID();
        Purpose<GenericSecret> rootPurpose = Purpose.purpose("rootSecret", GenericSecret.class);
        Purpose<GenericSecret> childPurpose = Purpose.purpose("childSecret", GenericSecret.class);
        RadiusDecisionNode.Config rootConfig = mock(RadiusDecisionNode.Config.class);
        RadiusDecisionNode.Config childConfig = mock(RadiusDecisionNode.Config.class);
        Realm childRealm = mock(Realm.class);
        SecretCache childSecretCache = mock(SecretCache.class);
        ServiceConfig childServiceConfig = mock(ServiceConfig.class);

        // Setup root realm config
        given(serviceConfigManager.getOrganizationConfig(ROOT_REALM_PATH, null)).willReturn(rootServiceConfig);
        given(rootServiceConfig.exists()).willReturn(true);
        given(rootServiceConfig.getSubConfigNames()).willReturn(Set.of(rootNodeId.toString(), "invalid"));
        given(annotatedServiceRegistry.getRealmInstance(RadiusDecisionNode.Config.class, realm,
                rootNodeId.toString())).willReturn(Optional.of(rootConfig));
        setupRadiusConfigMock(rootConfig, rootPurpose, 10, 3, false);
        given(secretCache.active(rootPurpose))
                .willReturn(SecretReference.constant(GenericSecret.password("root-secret".toCharArray())));

        // Setup child realm config
        given(realmLookup.getRealmNames()).willReturn(Set.of(ROOT_REALM_PATH, CHILD_REALM_PATH));
        given(realmLookup.lookup(CHILD_REALM_PATH)).willReturn(childRealm);
        given(childRealm.asPath()).willReturn(CHILD_REALM_PATH);
        given(secretReferenceCache.realm(childRealm)).willReturn(childSecretCache);
        given(serviceConfigManager.getOrganizationConfig(CHILD_REALM_PATH, null)).willReturn(childServiceConfig);
        given(childServiceConfig.exists()).willReturn(true);
        given(childServiceConfig.getSubConfigNames()).willReturn(Set.of(childNodeId.toString()));
        given(annotatedServiceRegistry.getRealmInstance(RadiusDecisionNode.Config.class, childRealm,
                childNodeId.toString())).willReturn(Optional.of(childConfig));
        setupRadiusConfigMock(childConfig, childPurpose, 5, 6, true);
        given(childSecretCache.active(childPurpose))
                .willReturn(SecretReference.constant(GenericSecret.password("child-secret".toCharArray())));

        // When
        RadiusMonitorPool testPool = new RadiusMonitorPool(configManagerFactory, messageAuthenticatorCalculator,
                annotatedServiceRegistry, secretReferenceCache, realmLookup);

        // Then
        assertThat(testPool.contains(rootNodeId)).isTrue();
        assertThat(testPool.contains(childNodeId)).isTrue();
        assertThat(testPool.size()).isEqualTo(2);
    }

    /**
     * Helper method to setup common RadiusDecisionNode.Config mock behavior.
     *
     * @param config                       the mock config to setup
     * @param purpose                      the shared secret purpose
     * @param timeout                      the timeout value
     * @param healthCheckInterval          the health check interval
     * @param requireMessageAuthenticator  whether message authenticator is required
     */
    private void setupRadiusConfigMock(RadiusDecisionNode.Config config, Purpose<GenericSecret> purpose,
            int timeout, int healthCheckInterval, boolean requireMessageAuthenticator) {
        given(config.sharedSecretPurpose()).willReturn(Optional.ofNullable(purpose));
        given(config.timeout()).willReturn(timeout);
        given(config.healthCheckInterval()).willReturn(healthCheckInterval);
        given(config.requireMessageAuthenticator()).willReturn(requireMessageAuthenticator);
    }
}
