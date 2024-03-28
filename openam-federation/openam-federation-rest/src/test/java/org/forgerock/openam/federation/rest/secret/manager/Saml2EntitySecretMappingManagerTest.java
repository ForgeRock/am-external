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
 * Copyright 2020-2024 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.Set;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.config.KeyStoreSecretStore;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sun.identity.saml2.meta.SAML2MetaException;

public class Saml2EntitySecretMappingManagerTest {

    private static final String SERVICE_NAME = "sunFMSAML2MetadataService";
    private static final String SERVICE_VERSION = "1.0";
    private static final String REALM = "o=testrealm,ou=services,dc=test,dc=org";
    private static final String REALM_PATH = "/realm";
    @Mock
    private SecretMappingManagerHelper helper;
    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private KeyStoreSecretStore secretStore;
    @Mock
    private Multiple<PurposeMapping> mappings;
    @Mock
    private RealmLookup realmLookup;

    private Saml2EntitySecretMappingManager manager;

    @Before
    public void setup() {
        openMocks(this);
        manager = new Saml2EntitySecretMappingManager(configManagerFactory, helper, realmLookup);
        given(realmLookup.convertRealmDnToRealmPath(REALM)).willReturn(REALM_PATH);
    }

    @Test
    public void shouldDoNothingWhenNewEntityIsAdded() throws Exception {
        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 1);

        //then
        verify(helper, never()).getAllHostedEntitySecretIdIdentifiers(anyString());
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).isUnusedHostedEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetRealmStoresWhenGetSecretIdentifiersThrowException() throws Exception {
        //given
        given(helper.getAllHostedEntitySecretIdIdentifiers(REALM_PATH)).willThrow(SAML2MetaException.class);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).isUnusedHostedEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetUnusedMappingsWhenNoKeyStoreIsConfigured() {
        //given
        given(helper.getRealmStores(REALM_PATH)).willReturn(Collections.emptySet());

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedHostedEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).isUnusedRemoteEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetUnusedMappingsWhenNoMappingsAvailable() {
        //given
        given(secretStore.mappings()).willReturn(null);
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedHostedEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).isUnusedRemoteEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotDeleteWhenNoSubConfigFoundInMappings() throws Exception {
        //given
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(emptySet());
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedHostedEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).isUnusedRemoteEntitySecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotDeleteHostedEntitySecretMappingWhenNoUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "some.secret.mapping";
        given(helper.getAllHostedEntitySecretIdIdentifiers(REALM)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedHostedEntitySecretMapping(secretIds, mappingId)).willReturn(false);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).deleteSecretMapping(mappings, mappingId);
    }

    @Test
    public void shouldNotDeleteRemoteEntitySecretMappingWhenNoUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "some.secret.mapping";
        given(helper.getAllRemoteEntitySecretIdIdentifiers(REALM)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedRemoteEntitySecretMapping(secretIds, mappingId)).willReturn(false);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).deleteSecretMapping(mappings, mappingId);
    }

    @Test
    public void shouldDeleteHostedEntitySecretMappingWhenUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "unused.secret.mapping";
        given(helper.getAllHostedEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedHostedEntitySecretMapping(secretIds, mappingId)).willReturn(true);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, atLeastOnce()).deleteSecretMapping(mappings, mappingId);
    }

    @Test
    public void shouldDeleteHostedEntitySecretMappingWhenOnlyRemoteMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String hostedMappingId = "hosted.secret.mapping";
        String remoteMappingId = "remote.secret.mapping";
        given(helper.getAllHostedEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(helper.getAllRemoteEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(Set.of(hostedMappingId, remoteMappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedHostedEntitySecretMapping(secretIds, hostedMappingId)).willReturn(true);
        given(helper.isUnusedRemoteEntitySecretMapping(secretIds, remoteMappingId)).willReturn(false);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, atLeastOnce()).deleteSecretMapping(mappings, hostedMappingId);
        verify(helper, never()).deleteSecretMapping(mappings, remoteMappingId);
    }

    @Test
    public void shouldDeleteRemoteEntitySecretMappingWhenUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "unused.secret.mapping";
        given(helper.getAllRemoteEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedRemoteEntitySecretMapping(secretIds, mappingId)).willReturn(true);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, atLeastOnce()).deleteSecretMapping(mappings, mappingId);
    }

    @Test
    public void shouldDeleteRemoteEntitySecretMappingWhenOnlyHostedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String hostedMappingId = "hosted.secret.mapping";
        String remoteMappingId = "remote.secret.mapping";
        given(helper.getAllHostedEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(helper.getAllRemoteEntitySecretIdIdentifiers(REALM_PATH)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(Set.of(hostedMappingId, remoteMappingId));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));
        given(helper.isUnusedHostedEntitySecretMapping(secretIds, hostedMappingId)).willReturn(false);
        given(helper.isUnusedRemoteEntitySecretMapping(secretIds, remoteMappingId)).willReturn(true);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).deleteSecretMapping(mappings, hostedMappingId);
        verify(helper, atLeastOnce()).deleteSecretMapping(mappings, remoteMappingId);
    }
}