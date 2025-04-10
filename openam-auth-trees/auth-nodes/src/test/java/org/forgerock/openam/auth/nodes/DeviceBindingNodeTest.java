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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.authentication.callbacks.DeviceBindingCallback;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingSettings;
import org.forgerock.openam.core.rest.devices.services.binding.AndroidKeyAttestationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Test for Device Binding.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeviceBindingNodeTest {

    @Mock
    DeviceBindingNode.Config config;

    @Mock
    LocaleSelector localeSelector;

    @Mock
    DeviceBindingManager deviceBindingManager;

    @InjectMocks
    DeviceBindingNode node;

    @Mock
    AMIdentity amIdentity;

    @Mock
    AndroidKeyAttestationService androidKeyAttestationService;

    @Mock
    NodeUserIdentityProvider identityProvider;

    @BeforeEach
    void setup() throws IdRepoException, SSOException {
        given(identityProvider.getUniversalId(any())).willReturn(Optional.of("bob"));

        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(amIdentity.getRealm()).willReturn("/");
        given(amIdentity.getUniversalId()).willReturn("bob");

        given(config.authenticationType()).willReturn(AuthenticationType.BIOMETRIC_ALLOW_FALLBACK);
        given(config.attestation()).willReturn(false);
        given(config.title()).willReturn(Map.of(Locale.ENGLISH, "title"));
        given(config.subtitle()).willReturn(Map.of(Locale.ENGLISH, "subtitle"));
        given(config.description()).willReturn(Map.of(Locale.ENGLISH, "description"));
        given(config.maxSavedDevices()).willReturn(2);
        given(config.timeout()).willReturn(60);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
    }

    @Test
    void testProcessWithNoInput()
            throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(DeviceBindingCallback.class);
        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getAuthenticationType())
                .isEqualTo("BIOMETRIC_ALLOW_FALLBACK");

        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getChallenge()).isNotNull();
        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getTitle()).isEqualTo("title");
        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getSubtitle()).isEqualTo("subtitle");
        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getDescription()).isEqualTo("description");
        assertThat(((DeviceBindingCallback) result.callbacks.get(0)).getUserId()).isEqualTo("bob");
    }

    @Test
    void testUserNotActive() throws NodeProcessException, IdRepoException, SSOException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());
        given(amIdentity.isActive()).willReturn(false);

        // When - Then
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Failed to lookup user");
    }

    @Test
    void testExceedDeviceLimit() throws NodeProcessException, DevicePersistenceException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        //given
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(new DeviceBindingSettings(), new DeviceBindingSettings()));

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceBindingNode.EXCEED_DEVICE_LIMIT_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();

    }

    @Test
    void testClientErrorOutcome() throws NodeProcessException {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        // Client
        DeviceBindingCallback callback = new DeviceBindingCallback("BIOMETRIC_ALLOW_FALLBACK",
                "challenge", "bob", "username", "title",
                "subtitle", "description", 60, false);
        callback.setClientError("my_custom_outcome");

        //When
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("my_custom_outcome");
        assertThat(result.callbacks).isEmpty();
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
                                   List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks,
                Optional.empty());
    }

}
