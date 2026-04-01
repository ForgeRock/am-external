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
package org.forgerock.openam.auth.nodes.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.am.identity.domain.Identity;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Unit tests for {@link AuthNodeUserIdentityHelper}.
 */
@ExtendWith(MockitoExtension.class)
class AuthNodeUserIdentityHelperTest {

    @Mock
    private LegacyIdentityService identityService;

    @Mock
    private CoreWrapper coreWrapper;

    @Mock
    private NodeState nodeState;

    @Mock
    private Realm realm;

    @Mock
    private AMIdentity identity;

    @Mock
    private SSOToken adminToken;

    @Mock
    private ServiceConfigManagerFactory configManagerFactory;

    @Mock
    private ServiceConfigManager serviceConfigManager;

    @Mock
    private ServiceConfig serviceConfig;

    @Mock
    private ServiceConfig storeConfig;

    @Mock
    private CoreServicesWrapper coreServiceWrapper;

    @Mock
    private AuthD authD;

    @InjectMocks
    private AuthNodeUserIdentityHelper helper;

    @Test
    void getIdentityWithIdentifiedIdentity() {
        // Given
        IdentifiedIdentity identifiedIdentity = mock(IdentifiedIdentity.class);
        given(identifiedIdentity.getUsername()).willReturn("user1");
        given(identifiedIdentity.getIdentityType()).willReturn(IdType.USER);
        given(coreServiceWrapper.getAuthD()).willReturn(authD);
        given(authD.getIdentity(any(), any(), any())).willReturn(Optional.of(identity));

        // When
        Optional<Identity> result = helper.getIdentity(realm, identifiedIdentity, nodeState);

        // Then
        assertThat(result).contains(identity);
    }

    @Test
    void getIdentityWithNodeState() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(new JsonValue("user1"));
        given(coreServiceWrapper.getAuthD()).willReturn(authD);
        given(authD.getIdentity(any(), any(), any())).willReturn(Optional.of(identity));

        // When
        Optional<Identity> result = helper.getIdentity(realm, null, nodeState);

        // Then
        assertThat(result).contains(identity);
    }

    @Test
    void getIdentityWithNodeStateMissingUsername() throws NodeProcessException {
        // Given
        given(nodeState.get(USERNAME)).willReturn(null);

        // When
        Optional<Identity> result = helper.getIdentity(realm, null, nodeState);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getIdentityWithNodeStateEmptyUsername() throws NodeProcessException {
        // Given
        given(nodeState.get(USERNAME)).willReturn(new JsonValue(""));

        // When
        Optional<Identity> result = helper.getIdentity(realm, null, nodeState);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAMIdentityWithOptionalUniversalId() {
        // Given
        String universalId = "id=user1,ou=user,o=realm";
        given(coreWrapper.getIdentity(universalId)).willReturn(identity);

        // When
        Optional<AMIdentity> result = helper.getAMIdentity(Optional.of(universalId), nodeState);

        // Then
        assertThat(result).contains(identity);
    }

    @Test
    void getAMIdentityWithFallbackToNodeState() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(new JsonValue("user1"));
        given(nodeState.get(REALM)).willReturn(new JsonValue("/realm"));
        given(identityService.getUniversalId("user1", "/realm", IdType.USER))
                .willReturn(Optional.of("id=user1,ou=user,o=realm"));
        given(coreWrapper.getIdentity("id=user1,ou=user,o=realm")).willReturn(identity);

        // When
        Optional<AMIdentity> result = helper.getAMIdentity(Optional.empty(), nodeState);

        // Then
        assertThat(result).contains(identity);
    }

    @Test
    void getAMIdentityWithFallbackToNodeStateFails() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(null);
        given(nodeState.keys()).willReturn(Set.of());

        // When
        Optional<AMIdentity> result = helper.getAMIdentity(Optional.empty(), nodeState);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUniversalIdFromNodeState() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(new JsonValue("user1"));
        given(nodeState.get(REALM)).willReturn(new JsonValue("/realm"));
        given(identityService.getUniversalId("user1", "/realm", IdType.USER))
                .willReturn(Optional.of("universalId"));

        // When
        Optional<String> result = helper.getUniversalId(nodeState);

        // Then
        assertThat(result).contains("universalId");
    }

    @Test
    void getUniversalIdFromNodeStateMissingUsername() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(null);

        // When
        Optional<String> result = helper.getUniversalId(nodeState);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUniversalIdFromNodeStateMissingRealm() {
        // Given
        given(nodeState.get(USERNAME)).willReturn(new JsonValue("user1"));
        given(nodeState.get(REALM)).willReturn(null);

        // When
        Optional<String> result = helper.getUniversalId(nodeState);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUniversalIdFromUsernameAndRealm() {
        // Given
        given(realm.asPath()).willReturn("/realm");
        given(identityService.getUniversalId("user1", "/realm", IdType.USER))
                .willReturn(Optional.of("universalId"));

        // When
        Optional<String> result = helper.getUniversalId("user1", realm);

        // Then
        assertThat(result).contains("universalId");
    }
}
