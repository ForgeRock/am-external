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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import static org.forgerock.openam.auth.nodes.oath.OathTokenVerifierNode.RECOVERY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith({MockitoExtension.class})
public class OathTokenVerifierNodeTest {

    @Mock
    private OathTokenVerifierNode.Config config;
    @Mock
    private OathDeviceProfileHelper deviceProfileHelper;

    private OathTokenVerifierNode node;
    @Mock
    private NodeUserIdentityProvider identityProvider;

    @Test
    void processThrowExceptionIfUserNameNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                // Then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Expected username to be set.");
    }

    @Test
    void processShouldReturnCorrectCallbacksDuringFirstPass() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());

        given(config.isRecoveryCodeAllowed()).willReturn(DEFAULT_ALLOW_RECOVERY_CODES);

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void processWhenNoDeviceSettingsThenNotRegisteredOutcome() throws Exception {
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
    void processWhenRecoveryPressedThenRecoveryCodeOutcome() throws Exception {
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
    void processWhenValidOtpProvidedThenSuccessOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());
        given(config.algorithm()).willReturn(OathAlgorithm.HOTP);
        when(nameCallback.getName())
                .thenReturn("564491");

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(ImmutableList.of(nameCallback), sharedState));

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
    }

    @Test
    void processWhenInvalidOtpProvidedThenFailureOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(deviceProfileHelper.getDeviceSettings(anyString(), anyString()))
                .willReturn(getDeviceSettings());
        given(config.algorithm()).willReturn(OathAlgorithm.HOTP);
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
        node = spy(
                new OathTokenVerifierNode(
                        config,
                        deviceProfileHelper,
                        new MultiFactorNodeDelegate(mock(AuthenticatorDeviceServiceFactory.class)),
                        identityProvider
                )
        );
    }

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() throws NodeProcessException {
        // Given
        BoundedOutcomeProvider provider = new OathTokenVerifierNode.OutcomeProvider();

        // When
        List<OutcomeProvider.Outcome> outcomes = provider.getAllOutcomes(new PreferredLocales());

        // Then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                NOT_REGISTERED_OUTCOME_ID,
                RECOVERY_CODE_OUTCOME_ID
        );
    }

    @Test
    void shouldReturnAllOutcomesWhenRecoveryCodeIsAllowed() {
        // Given
        var provider = new OathTokenVerifierNode.OutcomeProvider();
        var attributes = json(object(field("isRecoveryCodeAllowed", true)));

        // When
        List<OutcomeProvider.Outcome> outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // Then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                NOT_REGISTERED_OUTCOME_ID,
                RECOVERY_CODE_OUTCOME_ID
        );
    }

    @Test
    void shouldNotReturnRecoveryCodeOutcomeWhenRecoveryCodeIsNotAllowed() {
        // Given
        var provider = new OathTokenVerifierNode.OutcomeProvider();
        var attributes = json(object(field("isRecoveryCodeAllowed", false)));

        // When
        List<OutcomeProvider.Outcome> outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // Then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                NOT_REGISTERED_OUTCOME_ID
        );
    }

    @Test
    void shouldNotReturnRecoveryCodeOutcomeWhenAttributesAreNull() {
        // Given
        var provider = new OathTokenVerifierNode.OutcomeProvider();
        var attributes = json(null);

        // When
        List<OutcomeProvider.Outcome> outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // Then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                NOT_REGISTERED_OUTCOME_ID
        );
    }
}
