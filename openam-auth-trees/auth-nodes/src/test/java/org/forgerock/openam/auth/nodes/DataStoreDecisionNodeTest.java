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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
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
        given(coreWrapper.convertRealmNameToOrgName(any())).willReturn("org=name");
        given(coreWrapper.getAMIdentityRepository(any())).willReturn(identityRepository);
        given(amIdentity.isActive()).willReturn(true);
        given(coreWrapper.getIdentity(any(String.class), any())).willReturn(amIdentity);
    }

    @Test
    public void testProcessPassesUsernameAndPasswordToIdentityRepository() throws Exception {
        // Given
        given(identityRepository.authenticate(any(Callback[].class))).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(PASSWORD, "secret")));

        // When
        node.process(getContext(sharedState));

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
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(PASSWORD, "secret"));
    }

    @Test
    public void testProcessWithNoCallbacksReturnsFalseIfAuthenticationIsNotSuccessful() throws Exception {
        // Given
        given(identityRepository.authenticate(any(Callback[].class))).willReturn(false);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(PASSWORD, "secret")));

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().containsExactly(entry(USERNAME, "bob"));
        assertThat(sharedState).isObject().containsExactly(entry(USERNAME, "bob"), entry(PASSWORD, "secret"));
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList());
    }
}
