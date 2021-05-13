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
 * Copyright 2020 ForgeRock AS.
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
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Set;

import org.forgerock.openam.secrets.config.KeyStoreSecretStore;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.meta.SAML2MetaException;

public class Saml2EntitySecretMappingManagerTest {

    private static final String SERVICE_NAME = "sunFMSAML2MetadataService";
    private static final String SERVICE_VERSION = "1.0";
    private static final String REALM = "realm";
    @Mock
    private SecretMappingManagerHelper helper;
    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private KeyStoreSecretStore secretStore;
    @Mock
    private Multiple<PurposeMapping> mappings;

    private Saml2EntitySecretMappingManager manager;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        manager = new Saml2EntitySecretMappingManager(configManagerFactory, helper);
    }

    @Test
    public void shouldDoNothingWhenNewEntityIsAdded() throws Exception {
        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 1);

        //then
        verify(helper, never()).getAllEntitySecretIdIdentifiers(anyString());
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).isUnusedSecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetRealmStoresWhenGetSecretIdentifiersThrowException() throws Exception {
        //given
        given(helper.getAllEntitySecretIdIdentifiers(REALM)).willThrow(SAML2MetaException.class);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).isUnusedSecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetUnusedMappingsWhenNoKeyStoreIsConfigured() {
        //given
        given(helper.getRealmStores(REALM)).willReturn(Collections.emptySet());

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedSecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotGetUnusedMappingsWhenNoMappingsAvailable() {
        //given
        given(secretStore.mappings()).willReturn(null);
        given(helper.getRealmStores(REALM)).willReturn(singleton(secretStore));

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedSecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotDeleteWhenNoSubConfigFoundInMappings() throws Exception {
        //given
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(emptySet());
        given(helper.getRealmStores(REALM)).willReturn(singleton(secretStore));

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).isUnusedSecretMapping(anySet(), anyString());
        verify(helper, never()).deleteSecretMapping(any(), anyString());
    }

    @Test
    public void shouldNotDeleteWhenNoUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "some.secret.mapping";
        given(helper.getAllEntitySecretIdIdentifiers(REALM)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM)).willReturn(singleton(secretStore));
        given(helper.isUnusedSecretMapping(secretIds, mappingId)).willReturn(false);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, never()).deleteSecretMapping(mappings, mappingId);
    }

    @Test
    public void shouldDeleteWhenUnusedMappingFound() throws Exception {
        //given
        Set <String> secretIds = singleton("secretId");
        String mappingId = "unused.secret.mapping";
        given(helper.getAllEntitySecretIdIdentifiers(REALM)).willReturn(secretIds);
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton(mappingId));
        given(helper.getRealmStores(REALM)).willReturn(singleton(secretStore));
        given(helper.isUnusedSecretMapping(secretIds, mappingId)).willReturn(true);

        //when
        manager.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, 2);

        //then
        verify(helper, atLeastOnce()).deleteSecretMapping(mappings, mappingId);
    }
}