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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.service.datastore.SmsDataStoreLookup.DataStoreIdsBuilder;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link SmsDataStoreLookup}.
 *
 * @since 6.5.0
 */
public final class SmsDataStoreLookupTest {

    @Mock
    private ConsoleConfigHandler configHandler;
    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private Realm realm;

    private DataStoreLookup dataStoreLookup;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        dataStoreLookup = new SmsDataStoreLookup(configHandler, configManagerFactory);

        DataStoreIdsBuilder builder = new DataStoreIdsBuilder();
        builder.withPolicyDataStoreId(DataStoreId.of("policy-data-store"));
        builder.withApplicationDataStoreId(DataStoreId.of("application-data-store"));

        given(configHandler.getConfig(eq("/some/realm"), eq(DataStoreIdsBuilder.class)))
                .willReturn(builder.build(Collections.emptyMap()));
        given(realm.asPath()).willReturn("/some/realm");
    }

    @Test
    public void whenLookupCalledWithPolicyServicePolicyDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupId("sunEntitlementService", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("policy-data-store"));
    }

    @Test
    public void whenLookupCalledWithPolicyIndexServicePolicyDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupId("sunEntitlementIndexes", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("policy-data-store"));
    }

    @Test
    public void whenLookupCalledWithAgentServiceApplicationDataStoreIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupId("AgentService", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.of("application-data-store"));
    }

    @Test
    public void whenLookupCalledWithUnknownServiceDefaultIdReturned() {
        // When
        DataStoreId id = dataStoreLookup.lookupId("unknown-service", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.DEFAULT);
    }

    @Test
    public void whenLookupCalledWithRealmContainingNoConfigDefaultIdReturned() {
        // Given
        Realm realm = mock(Realm.class);
        given(realm.asPath()).willReturn("/some/other/realm");

        // When
        DataStoreId id = dataStoreLookup.lookupId("unknown-service", realm);

        // Then
        assertThat(id).isEqualTo(DataStoreId.DEFAULT);
    }

}