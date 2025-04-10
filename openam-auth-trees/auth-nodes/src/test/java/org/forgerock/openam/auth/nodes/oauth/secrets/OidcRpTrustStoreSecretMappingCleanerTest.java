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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oauth.secrets;

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

import java.util.Collections;
import java.util.Optional;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
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
    @InjectMocks
    private OidcRpTrustStoreSecretMappingCleaner cleaner;

    @BeforeEach
    void setup() {
        given(realmLookup.convertRealmDnToRealmPath(REALM)).willReturn(REALM_PATH);
    }


    @Test
    void shouldDoNothingWhenNewConfigIsAdded() {
        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, ADDED);

        //then
        verify(helper, never()).getRealmStores(anyString());
    }

    @Test
    void shouldDoNothingWhenNewConfigIsModified() {
        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, null, MODIFIED);

        //then
        verify(helper, never()).getRealmStores(anyString());
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    void shouldNotDeleteWhenNoKeyStoreIsConfigured() {
        //given
        given(helper.getRealmStores(REALM)).willReturn(Collections.emptySet());

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    void shouldNotDeleteWhenNoMappingsAvailable() {
        //given
        given(secretStore.mappings()).willReturn(null);
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    void shouldNotDeleteWhenNoSubConfigFoundInMappings() throws Exception {
        //given
        given(secretStore.mappings()).willReturn(mappings);
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, never()).deleteMapping(any(), any(), any(), any());
    }

    @Test
    void shouldNotDeleteWhenNoMappingFound() throws Exception {
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
    void shouldDeleteWhenOrphanMappingFound() throws Exception {
        //given
        given(helper.getSecretId(REALM_PATH, "myOidcConfig", secretStore, mappings))
                .willReturn(Optional.of("am.services.oidc.reliant.party.orphan.truststore"));
        given(secretStore.mappings()).willReturn(mappings);
        given(helper.getRealmStores(REALM_PATH)).willReturn(singleton(secretStore));

        //when
        cleaner.organizationConfigChanged(SERVICE_NAME, SERVICE_VERSION, REALM, null, CONFIG_NAME, REMOVED);

        //then
        verify(helper, atLeastOnce()).deleteMapping(any(), any(), any(), any());
    }
}
