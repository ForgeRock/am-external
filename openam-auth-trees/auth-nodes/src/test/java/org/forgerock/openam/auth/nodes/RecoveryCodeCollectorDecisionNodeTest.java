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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.OATH_AUTH_TYPE;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.PUSH_AUTH_TYPE;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.WEB_AUTHN_AUTH_TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.Config;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.RecoveryCodeType;
import org.forgerock.openam.core.rest.devices.DeviceProfileManager;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link RecoveryCodeCollectorDecisionNode}.
 */
@ExtendWith(MockitoExtension.class)
class RecoveryCodeCollectorDecisionNodeTest {

    private static final String USER = "badger";
    private static final String REALM_VALUE = "weasel";
    private static final String TEST_RECOVERY_CODE = "ferret";
    private static final String INVALID_RECOVERY_CODE = "otter";

    @Mock
    private Config config;

    @Mock
    private UserOathDeviceProfileManager oathDeviceManager;

    @Mock
    private UserPushDeviceProfileManager pushDeviceManager;

    @Mock
    private UserWebAuthnDeviceProfileManager webAuthnDeviceManager;

    private RecoveryCodeCollectorDecisionNode node;
    private DeviceProfileManager<? extends DeviceSettings> manager;
    private DeviceSettings settings;
    private String deviceType;
    private List<Callback> callbacks;
    private TreeContext treeContext;

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldReturnNameCallbackGivenNoCallbackProvided(RecoveryCodeType codeType) {
        // Given
        setupForType(codeType);
        setupEmptyCallbacks();

        // When
        Action result = node.process(treeContext);

        // Then
        verifyNameCallbackReturned(result);
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldRetrieveRecoveryCodesUsingCorrectDaoGivenNameCallbackWithCode(RecoveryCodeType codeType)
            throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();

        // When
        Action result = node.process(treeContext);

        // Then
        verify(manager).getDeviceProfiles(eq(USER), eq(REALM_VALUE));
        assertThat(result.outcome).isEqualTo("false");
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldReturnTrueGivenCorrectRecoveryCode(RecoveryCodeType codeType) throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();
        mockRecoveryCodes(codeType, TEST_RECOVERY_CODE);

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldReturnFalseGivenIncorrectRecoveryCode(RecoveryCodeType codeType) throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();
        mockRecoveryCodes(codeType, INVALID_RECOVERY_CODE);

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldRemoveCodeFromListGivenCodeIsValid(RecoveryCodeType codeType) throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();
        mockRecoveryCodes(codeType, TEST_RECOVERY_CODE, INVALID_RECOVERY_CODE);
        int recoveryCodesSize = settings.getRecoveryCodes().size();

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(settings.getRecoveryCodes().size()).isLessThan(recoveryCodesSize);
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldSetCorrectNodeTypeGivenValidRecoveryCode(RecoveryCodeType codeType) throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();
        mockRecoveryCodes(codeType, TEST_RECOVERY_CODE);

        // When
        Action result = node.process(treeContext);

        // Then
        String nodeType = result.sessionProperties.get(NODE_TYPE);
        assertThat(nodeType).contains(deviceType);
    }

    @ParameterizedTest
    @EnumSource(RecoveryCodeType.class)
    void shouldNotSetNodeTypeGivenInvalidRecoveryCode(RecoveryCodeType codeType) throws Exception {
        // Given
        setupForType(codeType);
        setupCallbackWithCode();
        mockRecoveryCodes(codeType, INVALID_RECOVERY_CODE);

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.sessionProperties).doesNotContainKey(NODE_TYPE);
    }

    // Helper methods
    private void setupForType(RecoveryCodeType codeType) {
        given(config.recoveryCodeType()).willReturn(codeType);
        node = new RecoveryCodeCollectorDecisionNode(config, pushDeviceManager, oathDeviceManager,
                webAuthnDeviceManager);

        switch (codeType) {
            case OATH:
                manager = oathDeviceManager;
                settings = new OathDeviceSettings();
                deviceType = OATH_AUTH_TYPE;
                break;
            case PUSH:
                manager = pushDeviceManager;
                settings = new PushDeviceSettings();
                deviceType = PUSH_AUTH_TYPE;
                break;
            case WEB_AUTHN:
                manager = webAuthnDeviceManager;
                settings = new WebAuthnDeviceSettings();
                deviceType = WEB_AUTHN_AUTH_TYPE;
                break;
        }
    }

    private void setupEmptyCallbacks() {
        callbacks = Collections.emptyList();
        treeContext = getContext();
    }

    private void setupCallbackWithCode() {
        NameCallback nameCallback = new NameCallback("foo");
        nameCallback.setName(RecoveryCodeCollectorDecisionNodeTest.TEST_RECOVERY_CODE);
        callbacks = singletonList(nameCallback);
        treeContext = getContext();
    }

    private void verifyNameCallbackReturned(Action result) {
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).isNotNull().hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
    }

    private TreeContext getContext() {
        return TreeContext.builder()
                .sharedState(json(object(field(USERNAME, USER), field(REALM, REALM_VALUE))))
                .callbacks(callbacks)
                .build();
    }

    private void mockRecoveryCodes(RecoveryCodeType codeType, String... codes) throws Exception {
        // Set recovery codes if provided
        if (codes != null && codes.length > 0) {
            settings.setRecoveryCodes(Arrays.asList(codes));
        }

        // Use type-specific mocking based on code type
        switch (codeType) {
            case OATH:
                given(oathDeviceManager.getDeviceProfiles(eq(USER), eq(REALM_VALUE)))
                        .willReturn(Collections.singletonList((OathDeviceSettings) settings));
                break;
            case PUSH:
                given(pushDeviceManager.getDeviceProfiles(eq(USER), eq(REALM_VALUE)))
                        .willReturn(Collections.singletonList((PushDeviceSettings) settings));
                break;
            case WEB_AUTHN:
                given(webAuthnDeviceManager.getDeviceProfiles(eq(USER), eq(REALM_VALUE)))
                        .willReturn(Collections.singletonList((WebAuthnDeviceSettings) settings));
                break;
        }
    }
}
