/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;

public class DataStoreDecisionNodeTest {
    @Mock
    CoreWrapper coreWrapper;

    @Mock
    AMIdentityRepository identityRepository;

    @InjectMocks
    DataStoreDecisionNode node;

    @Mock
    AMIdentity amIdentity;

    @BeforeMethod
    public void setup() throws Exception {
        node = null;
        initMocks(this);
        given(coreWrapper.convertRealmPathToRealmDn(any())).willReturn("org=name");
        given(coreWrapper.getAMIdentityRepository(any())).willReturn(identityRepository);
        given(amIdentity.isActive()).willReturn(true);
        given(coreWrapper.getIdentity(anyString(), any())).willReturn(amIdentity);
    }

    @Test
    public void testProcessPassesUsernameAndPasswordToIdentityRepository() throws Exception {
        // Given
        given(identityRepository.authenticate(any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        node.process(getContext(sharedState, transientState));

        // Then
        ArgumentCaptor<Callback[]> callbacksCaptor = ArgumentCaptor.forClass(Callback[].class);
        verify(identityRepository).authenticate(callbacksCaptor.capture());
        Callback[] callbacks = callbacksCaptor.getValue();
        assertThat(callbacks.length).isEqualTo(2);
        assertThat(callbacks[0]).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) callbacks[0]).getName()).isEqualTo("bob");
        assertThat(callbacks[1]).isInstanceOf(PasswordCallback.class);
        assertThat(((PasswordCallback) callbacks[1]).getPassword()).isEqualTo("secret".toCharArray());
    }

    @Test
    public void testProcessWithNoCallbacksReturnsTrueIfAuthenticationIsSuccessful() throws Exception {
        // Given
        given(identityRepository.authenticate(any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "secret"));
    }

    @Test
    public void testProcessWithNoCallbacksReturnsFalseIfAuthenticationIsNotSuccessful() throws Exception {
        // Given
        given(identityRepository.authenticate(any(Callback[].class))).willReturn(false);
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object(field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(transientState).isObject().containsExactly(entry(PASSWORD, "secret"));
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return new TreeContext(sharedState, transientState, new Builder().build(), emptyList());
    }
}
