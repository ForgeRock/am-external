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
 * Copyright 2026 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.SPCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultIDPAccountMapperTest {

    @Mock
    private DataStoreProvider dsProvider;

    private DataStoreProvider originalProvider;
    private DefaultIDPAccountMapper accountMapper;
    private MockedStatic<SAML2Utils> mockedSaml2Utils;

    @BeforeEach
    void setUp() {
        IDPCache.formatAttributeHash.clear();
        SPCache.formatAttributeHash.clear();
        mockedSaml2Utils = mockStatic(SAML2Utils.class);
        mockedSaml2Utils.when(SAML2Utils::getDataStoreProvider).thenReturn(dsProvider);
        accountMapper = new DefaultIDPAccountMapper();
        originalProvider = DefaultAccountMapper.dsProvider;
        DefaultAccountMapper.dsProvider = dsProvider;
    }

    @AfterEach
    void tearDown() {
        if (mockedSaml2Utils != null) {
            mockedSaml2Utils.close();
        }
        DefaultAccountMapper.dsProvider = originalProvider;
        IDPCache.formatAttributeHash.clear();
        SPCache.formatAttributeHash.clear();
    }

    @Test
    void shouldUseNameIDValueFromUserProfileFromIDP() throws Exception {
        String realm = "testRealm";
        String hostEntityID = "hostEntity";
        String remoteEntityID = "remoteEntity";
        String userID = "user123";
        String nameIDFormat = "nameid-format";
        String spCacheKey = cacheKey(hostEntityID, remoteEntityID, realm);

        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID, IDP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=idpattr"));
        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, remoteEntityID, SP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of());
        when(dsProvider.getAttribute(userID, "idpattr")).thenReturn(Set.of("idp-nameid"));

        String result = accountMapper.getNameIDValueFromUserProfile(realm, hostEntityID, remoteEntityID,
                userID, nameIDFormat);

        assertEquals("idp-nameid", result);
        assertTrue(SPCache.formatAttributeHash.containsKey(spCacheKey));
        assertEquals("idpattr", SPCache.formatAttributeHash.get(spCacheKey).get("nameid-format"));
    }

    @Test
    void shouldUseNameIDValueFromUserProfileFromSP() throws Exception {
        String realm = "testRealm";
        String hostEntityID = "hostEntity";
        String remoteEntityID = "remoteEntity";
        String userID = "user123";
        String nameIDFormat = "nameid-format";
        String spCacheKey = cacheKey(hostEntityID, remoteEntityID, realm);

        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID, IDP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=idpattr"));
        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, remoteEntityID, SP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=spattr"));
        when(dsProvider.getAttribute(userID, "spattr")).thenReturn(Set.of("sp-nameid"));

        String result = accountMapper.getNameIDValueFromUserProfile(realm, hostEntityID, remoteEntityID,
                userID, nameIDFormat);

        assertEquals("sp-nameid", result);
        assertTrue(SPCache.formatAttributeHash.containsKey(spCacheKey));
        assertEquals("spattr", SPCache.formatAttributeHash.get(spCacheKey).get("nameid-format"));
    }

    @Test
    void shouldUseNameIDValueFromUserProfileFromOwnIDPWhenSharingSP() throws Exception {
        String realm = "testRealm";
        String hostEntityID1 = "hostEntity1";
        String hostEntityID2 = "hostEntity2";
        String remoteEntityID = "remoteEntity";
        String userID = "user123";
        String nameIDFormat = "nameid-format";
        String spCacheKey1 = cacheKey(hostEntityID1, remoteEntityID, realm);
        String spCacheKey2 = cacheKey(hostEntityID2, remoteEntityID, realm);

        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID1, IDP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=idpattr1"));
        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID2, IDP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=idpattr2"));
        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, remoteEntityID, SP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of());
        when(dsProvider.getAttribute(userID, "idpattr1")).thenReturn(Set.of("idp1-nameid"));
        when(dsProvider.getAttribute(userID, "idpattr2")).thenReturn(Set.of("idp2-nameid"));

        String nameid1 = accountMapper.getNameIDValueFromUserProfile(realm, hostEntityID1, remoteEntityID,
                userID, nameIDFormat);
        String nameid2 = accountMapper.getNameIDValueFromUserProfile(realm, hostEntityID2, remoteEntityID,
                userID, nameIDFormat);

        assertEquals("idp1-nameid", nameid1);
        assertEquals("idp2-nameid", nameid2);
        assertTrue(SPCache.formatAttributeHash.containsKey(spCacheKey1));
        assertTrue(SPCache.formatAttributeHash.containsKey(spCacheKey2));
        assertEquals("idpattr1", SPCache.formatAttributeHash.get(spCacheKey1).get("nameid-format"));
        assertEquals("idpattr2", SPCache.formatAttributeHash.get(spCacheKey2).get("nameid-format"));
    }

    @Test
    void shouldUseNameIDValueFromUserProfileFromIDPWhenRemoteEntityIDIsNull() throws Exception {
        String realm = "testRealm";
        String hostEntityID = "hostEntity";
        String userID = "user123";
        String nameIDFormat = "nameid-format";
        String cacheKey = hostEntityID + "|" + realm;

        mockedSaml2Utils.when(() -> SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID, IDP_ROLE,
                SAML2Constants.NAME_ID_FORMAT_MAP)
        ).thenReturn(List.of("nameid-format=idpattr"));
        when(dsProvider.getAttribute(userID, "idpattr")).thenReturn(Set.of("idp-nameid"));

        String result = accountMapper.getNameIDValueFromUserProfile(realm, hostEntityID, userID, nameIDFormat);
        assertEquals("idp-nameid", result);
        assertTrue(IDPCache.formatAttributeHash.containsKey(cacheKey));
        assertEquals("idpattr", IDPCache.formatAttributeHash.get(cacheKey).get("nameid-format"));
    }

    private static String cacheKey(String hostEntityID, String remoteEntityID, String realm) {
        return remoteEntityID + "|" + hostEntityID + "|" + realm;
    }
}
