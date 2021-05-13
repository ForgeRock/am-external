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

package org.forgerock.openam.auth.nodes.oath;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.NOT_REGISTERED_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.RECOVERY_CODE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_ALLOW_RECOVERY_CODES;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_HOTP_WINDOW_SIZE;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MAXIMUM_ALLOWED_CLOCK_DRIFT;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_TIME_STEPS;
import static org.forgerock.openam.auth.nodes.oath.OathTokenVerifierNode.RECOVERY;
import static org.forgerock.openam.auth.nodes.oath.OathTokenVerifierNode.SUBMIT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class OathTokenVerifierNodeTest {

    @Mock
    OathTokenVerifierNode.Config config;
    @Mock
    private OathDeviceProfileHelper deviceProfileHelper;
    @Mock
    private Realm realm;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    IdentityUtils identityUtils;

    OathTokenVerifierNode node;

    @BeforeMethod
    public void setup() {
        initMocks(this);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowExceptionIfUserNameNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        // throw exception
    }

    @Test
    public void processShouldReturnCorrectCallbacksDuringFirstPass() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    public void processWhenNoDeviceSettingsThenNotRegisteredOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.outcome).isEqualTo(NOT_REGISTERED_OUTCOME_ID);
    }

    @Test
    public void processWhenRecoveryPressedThenRecoveryCodeOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());
        when(confirmationCallback.getSelectedIndex())
                .thenReturn(RECOVERY);

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(ImmutableList.of(confirmationCallback), sharedState));

        // Then
        assertThat(result.outcome).isEqualTo(RECOVERY_CODE_OUTCOME_ID);
    }

    @Test
    public void processWhenValidOtpProvidedThenSuccessOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());
        when(confirmationCallback.getSelectedIndex())
                .thenReturn(SUBMIT);
        when(nameCallback.getName())
                .thenReturn("564491");

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(ImmutableList.of(nameCallback), sharedState));

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
    }

    @Test
    public void processWhenInvalidOtpProvidedThenFailureOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());
        when(confirmationCallback.getSelectedIndex())
                .thenReturn(SUBMIT);
        when(nameCallback.getName())
                .thenReturn("000000");

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(ImmutableList.of(nameCallback), sharedState));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE_OUTCOME_ID);
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(
                sharedState, new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty()
        );
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(
                sharedState, new ExternalRequestContext.Builder().build(), callbacks, Optional.empty()
        );
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState, transientState, new ExternalRequestContext.Builder().build(),
                callbacks, Optional.empty()
        );
    }

    private OathDeviceSettings getDeviceSettings() {
        OathDeviceSettings settings = new OathDeviceSettings();

        settings.setSharedSecret("abcd");
        settings.setDeviceName("Oath Device");

        return settings;
    }

    private void whenNodeConfigHasDefaultValues() {
        config = mock(OathTokenVerifierNode.Config.class);
        given(config.hotpWindowSize()).willReturn(DEFAULT_HOTP_WINDOW_SIZE);
        given(config.maximumAllowedClockDrift()).willReturn(DEFAULT_MAXIMUM_ALLOWED_CLOCK_DRIFT);
        given(config.isRecoveryCodeAllowed()).willReturn(DEFAULT_ALLOW_RECOVERY_CODES);
        given(config.totpTimeSteps()).willReturn(DEFAULT_TOTP_TIME_STEPS);
        given(config.algorithm()).willReturn(OathAlgorithm.HOTP);
        given(config.totpTimeInterval()).willReturn(DEFAULT_TOTP_INTERVAL);
        given(config.totpHashAlgorithm()).willReturn(HashAlgorithm.HMAC_SHA1);

        node = spy(
                new OathTokenVerifierNode(
                        config,
                        realm,
                        coreWrapper,
                        deviceProfileHelper,
                        new MultiFactorNodeDelegate(mock(AuthenticatorDeviceServiceFactory.class)),
                        identityUtils
                )
        );
    }
}
