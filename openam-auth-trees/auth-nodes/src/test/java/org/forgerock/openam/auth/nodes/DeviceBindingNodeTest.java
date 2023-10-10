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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.DeviceBindingCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingSettings;
import org.forgerock.openam.core.rest.devices.services.binding.AndroidKeyAttestationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Test for Device Binding
 */
public class DeviceBindingNodeTest {

    @Mock
    DeviceBindingNode.Config config;

    @Mock
    LocaleSelector localeSelector;

    @Mock
    DeviceBindingManager deviceBindingManager;

    @Mock
    CoreWrapper coreWrapper;

    @InjectMocks
    DeviceBindingNode node;

    @Mock
    AMIdentity amIdentity;

    @Mock
    LegacyIdentityService identityService;

    @Mock
    AndroidKeyAttestationService androidKeyAttestationService;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException {
        node = null;
        openMocks(this);

        given(identityService.getAmIdentity(any(SSOToken.class), any(String.class), eq(IdType.USER), any()))
                .willReturn(amIdentity);

        given(identityService.getUniversalId(any(), (IdType) any(), any()))
                .willReturn(UUID.randomUUID().toString());

        given(identityService.getUniversalId(any(), any(), (IdType) any())).willReturn(Optional.of("bob"));

        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
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
        given(config.clientErrorOutcomes()).willReturn(Collections.singletonList("unsupported"));
        given(androidKeyAttestationService.verify(any(), any(), any())).willReturn(true);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void testProcessWithNoInput()
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

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "Failed to lookup user")
    public void testUserNotActive() throws NodeProcessException, IdRepoException, SSOException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());
        given(amIdentity.isActive()).willReturn(false);

        // When
        node.process(getContext(sharedState, transientState, emptyList()));
    }

    @Test
    public void testExceedDeviceLimit() throws NodeProcessException, DevicePersistenceException {
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
    public void testClientErrorOutcome() throws NodeProcessException {

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