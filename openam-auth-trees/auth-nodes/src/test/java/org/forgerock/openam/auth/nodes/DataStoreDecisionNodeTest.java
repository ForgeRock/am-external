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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.security.PrivilegedAction;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.google.inject.Provider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class DataStoreDecisionNodeTest {
    @Mock
    CoreWrapper coreWrapper;

    @Mock
    IdentityStore identityStore;

    @Mock
    LegacyIdentityService identityService;

    @Mock
    Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider;

    @InjectMocks
    DataStoreDecisionNode node;

    @Mock
    AMIdentity amIdentity;

    @Mock
    SSOToken adminToken;

    @BeforeEach
    void setup() throws Exception {
        given(coreWrapper.convertRealmPathToRealmDn(any())).willReturn("org=name");
        given(coreWrapper.getIdentityRepository(any())).willReturn(identityStore);
        given(amIdentity.isActive()).willReturn(true);
        given(identityService.getAmIdentity(any(SSOToken.class), any(String.class), eq(IdType.USER), any()))
                .willReturn(amIdentity);
        given(adminTokenActionProvider.get()).willReturn(() -> adminToken);
    }

    @Test
    void testProcessAddsIdentifiedIdentityOfUserInStore() throws Exception {
        // Given
        given(identityStore.authenticate(eq(IdType.USER), any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action action = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(action.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = action.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    void givenNoUsernameAssertIdentityNotIdentified() throws Exception {
        // Given
        given(identityStore.authenticate(eq(IdType.USER), any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action action = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(action.identifiedIdentity).isEmpty();

    }

    @Test
    void testProcessDoesNotAddIdentifiedIdentityOfUserNotInStore() throws Exception {
        // Given
        given(identityStore.authenticate(eq(IdType.USER), any(Callback[].class))).willReturn(false);
        JsonValue sharedState = json(object(field(USERNAME, "bob-2")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action action = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(action.identifiedIdentity).isEmpty();
    }

    @Test
    void shouldFailIfPasswordIsBlank() throws Exception {
        // Given
        given(identityStore.authenticate(eq(IdType.USER), any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object(field(PASSWORD, "")));

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat(result.identifiedIdentity).isEmpty();
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(transientState).isObject().containsExactly(entry(PASSWORD, ""));
    }

    @Test
    void testProcessPassesUsernameAndPasswordToIdentityRepository() throws Exception {
        // Given
        given(identityStore.authenticate(any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        node.process(getContext(sharedState, transientState));

        // Then
        ArgumentCaptor<Callback[]> callbacksCaptor = ArgumentCaptor.forClass(Callback[].class);
        verify(identityStore).authenticate(eq(IdType.USER), callbacksCaptor.capture());
        Callback[] callbacks = callbacksCaptor.getValue();
        assertThat(callbacks.length).isEqualTo(2);
        assertThat(callbacks[0]).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) callbacks[0]).getName()).isEqualTo("bob");
        assertThat(callbacks[1]).isInstanceOf(PasswordCallback.class);
        assertThat(((PasswordCallback) callbacks[1]).getPassword()).isEqualTo("secret".toCharArray());
    }

    @Test
    void testProcessWithNoCallbacksReturnsTrueIfAuthenticationIsSuccessful() throws Exception {
        // Given
        given(identityStore.authenticate(eq(IdType.USER), any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "secret"));
    }

    @Test
    void testProcessWithNoCallbacksReturnsFalseIfAuthenticationIsNotSuccessful() throws Exception {
        // Given
        given(identityStore.authenticate(any(Callback[].class))).willReturn(false);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(REALM, "/realm"));
        assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "secret"));
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return new TreeContext(sharedState, transientState, new Builder().build(), emptyList(), Optional.empty());
    }
}
