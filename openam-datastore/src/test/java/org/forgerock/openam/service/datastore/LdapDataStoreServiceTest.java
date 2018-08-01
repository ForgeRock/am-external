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

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


import javax.inject.Provider;

import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.hamcrest.core.IsSame;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.sm.ServiceConfigManager;

public class LdapDataStoreServiceTest {

    @Mock
    Provider<ConnectionFactory> mockFactoryService;

    @Mock
    ConnectionFactory mockConnectionFactory;

    @Mock
    Connection mockConnection;

    @Mock
    DataStoreConfig mockServiceConfig;

    @Mock
    ServiceConfigManagerFactory serviceManagerFactory;

    @Mock
    ServiceConfigManager serviceConfigManager;

    @Mock
    ShutdownManager mockShutdownManager;

    LdapDataStoreService testInstance;

    @BeforeTest
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(serviceManagerFactory.create(anyString(), anyString())).thenReturn(serviceConfigManager);

        testInstance = new LdapDataStoreService(mockFactoryService, serviceManagerFactory, mockShutdownManager);

        when(mockFactoryService.get()).thenReturn(mockConnectionFactory);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);

        when(mockServiceConfig.getHostname()).thenReturn("localhost");
    }

    @Test
    public void testGetDefaultConnection() throws Exception {
        // Given

        // When
        Connection defaultConnection = testInstance.getDefaultConnection();

        // Then
        assertThat(defaultConnection, IsSame.sameInstance(mockConnection));
    }

}