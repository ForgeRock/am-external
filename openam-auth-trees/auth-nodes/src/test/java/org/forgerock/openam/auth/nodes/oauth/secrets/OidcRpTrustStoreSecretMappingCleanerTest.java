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
 * Copyright 2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oauth.secrets;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.forgerock.openam.ldap.PersistentSearchChangeType.ADDED;
import static org.forgerock.openam.ldap.PersistentSearchChangeType.MODIFIED;
import static org.forgerock.openam.ldap.PersistentSearchChangeType.REMOVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.Optional;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OidcRpTrustStoreSecretMappingCleanerTest {

    private static final String REALM = "o=testrealm,ou=services,dc=test,dc=org";
    private static final String REALM_PATH = "/realm";
    private static final String SERVICE_NAME = "SocialIdentityProviders";
    private static final String SERVICE_VERSION = "1.0";
    private static final String CONFIG_NAME = "/oidc/myOidcConfig";
    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private SecretStoreWithMappings secretStore;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private OidcRpTrustStoreSecretMappingCleaner.Helper helper;
    @Mock
    private Multiple<PurposeMapping> mappings;
    private OidcRpTrustStoreSecretMappingCleaner cleaner;

    @BeforeMethod
    public void setup() {
        openMocks(this);
        cleaner = new OidcRpTrustStoreSecretMappingCleaner(configManagerFactory, realmLookup, helper);
        given(realmLookup.convertRealmDnToRealmPath(REALM)).willReturn(REALM_PATH);
    }


    @Test
    public void shouldDoNothingWhenNewConfigIsAdded() {
        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, ADDED);

        //then
        verify(helper, never()).getRealmStores(anyString());
    }

    @Test
    public void shouldDoNothingWhenNewConfigIsModified() {
        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, MODIFIED);

        //then
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeleteWhenNoKeyStoreIsConfigured() {
        //given
        given(helper.getRealmStores(REALM)).willReturn(Collections.emptySet());

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeleteWhenNoMappingsAvailable() {
        //given
        given(secretStore.mappings()).willReturn(null);
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeleteWhenNoSubConfigFoundInMappings() throws Exception {
        //given
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(emptySet());
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeleteWhenNoMappingFound() throws Exception {
        //given
        given(helper.getSecretId(REALM, CONFIG_NAME, secretStore, mappings)).willReturn(Optional.of("secretId"));
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton("some.secret.mapping.id"));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    public void shouldDeleteWhenOrphanMappingFound() throws Exception {
        //given
        given(helper.getSecretId(REALM_PATH, "myOidcConfig", secretStore, mappings))
                .willReturn(Optional.of("am.services.oidc.reliant.party.orphan.truststore"));
        given(secretStore.mappings()).willReturn(mappings);
        given(mappings.idSet()).willReturn(singleton("am.services.oidc.reliant.party.orphan.truststore"));
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, atLeastOnce()).deleteMapping(any(), any(), any(), any());
    }
}