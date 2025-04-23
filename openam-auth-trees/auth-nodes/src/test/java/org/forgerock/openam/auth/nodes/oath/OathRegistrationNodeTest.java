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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_BG_COLOR;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_GENERATE_RECOVERY_CODES;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_ISSUER;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.SCAN_QR_CODE_MSG_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_CHECKSUM;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MIN_SHARED_SECRET_LENGTH;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_ENABLE_RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathRegistrationHelper.NEXT_LABEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationUtilities;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.google.common.collect.ImmutableList;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.idm.AMIdentity;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class OathRegistrationNodeTest {

    @Mock
    OathRegistrationConfig config;
    @Mock
    private Realm realm;
    @Mock
    private OathDeviceProfileHelper deviceProfileHelper;
    @Mock
    AMIdentity userIdentity;
    @Mock
    private LocalizedMessageProvider localizationHelper;
    @Mock
    private MultiFactorRegistrationUtilities multiFactorRegistrationUtilities;
    @Mock
    private NodeUserIdentityProvider identityProvider;

    OathRegistrationHelper oathRegistrationHelper;
    OathRegistrationNode node;

    static final String MESSAGE = "Scan the QR code image below with the ForgeRock Authenticator app to "
            + "register your device with your login";

    static final Map<Locale, String> MAP_SCAN_MESSAGE = new HashMap<>() {
        {
            put(Locale.CANADA, MESSAGE);
        }
    };

    @BeforeEach
    void setup() {
        when(deviceProfileHelper.isDeviceSettingsStored(any())).thenReturn(false);
    }

    @Test
    void processThrowExceptionIfUserNameNotPresentInSharedState() {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When / Then
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Expected username to be set.");
    }

    @Test
    void processShouldStartRegistration() throws Exception {
        // Given
        whenNodeConfigHasDefaultValues();

        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );

        given(deviceProfileHelper.createDeviceSettings(anyInt(), anyBoolean(), anyInt()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.encodeDeviceSettings(any()))
                .willReturn("ekd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9ab");
        doReturn(mock(ScriptTextOutputCallback.class))
                .when(oathRegistrationHelper).createQRCodeCallback(any(), any(), any(), anyString(), anyString());
        doReturn(mock(HiddenValueCallback.class))
                .when(oathRegistrationHelper).createHiddenCallback(any(), any(), any(), anyString(), anyString());
        doReturn(userIdentity)
                .when(node).getIdentity(any());
        doReturn("userId")
                .when(userIdentity).getName();
        doReturn("forgerock")
                .when(oathRegistrationHelper).getBase32Secret(anyString());

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(4);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNotEmpty();
        assertThat(result.sharedState.get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()).isTrue();
        String message = ((TextOutputCallback) result.callbacks.get(0)).getMessage();
        assertThat(message).contains(MESSAGE);
    }

    @Test
    void processSuccessfulRegistrationReturnSuccessOutcomeAndRecoveryCodes()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(true, false);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(oathRegistrationHelper).setUserToNotSkippable(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)),
                getOathRegistrationSharedState(true))
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNull();
        assertThat(result.sharedState.get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()).isNull();
        assertThat(result.transientState.get(RECOVERY_CODE_KEY).asList())
                .containsExactlyInAnyOrder("z0WKEw0Wc8", "Ios4LnA2Qn");
        assertThat(result.transientState.get(RECOVERY_CODE_DEVICE_NAME).asString())
                .isEqualTo("Oath Device");
    }

    @Test
    void processSuccessfulRegistrationReturnSuccessOutcomeWithoutSaveDeviceProfile()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(true, true);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)), getOathRegistrationSharedState(true))
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState).isNotNull();
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNotEmpty();
        assertThat(result.sharedState.get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()).isTrue();
        assertThat(result.transientState).isNull();
    }

    @Test
    void processSuccessfulRegistrationWhenRecoveryCodesDisabledReturnSuccessOutcome()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(false, false);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(oathRegistrationHelper).setUserToNotSkippable(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)),
                getOathRegistrationSharedState(true))
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNull();
        assertThat(result.sharedState.get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()).isNull();
        assertThat(result.transientState).isNull();
    }

    @Test
    void processSuccessfulRegistrationWhenRecoveryCodesDisabledAndPostponeDeviceEnabled()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(false, true);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)),
                getOathRegistrationSharedState(false))
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState).isNotNull();
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNotEmpty();
        assertThat(result.sharedState.get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()).isFalse();
        assertThat(result.transientState).isNull();
    }

    private JsonValue getOathRegistrationSharedState(boolean recoveryCode) {
        return json(
                object(
                        field(USERNAME, "rod"),
                        field(REALM, "root"),
                        field(OATH_DEVICE_PROFILE_KEY, "ekd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9ab="),
                        field(OATH_ENABLE_RECOVERY_CODE_KEY, recoveryCode)
                )
        );
    }

    private OathDeviceSettings getDeviceSettings() {
        OathDeviceSettings settings = new OathDeviceSettings();
        settings.setSharedSecret("olVsCC00XtifveplR0fI7ZeE3r0i3ei+lERaPESSoPg=");
        settings.setDeviceName("Oath Device");
        return settings;
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

    private void whenNodeConfigHasDefaultValues() {
        whenNodeConfigHasDefaultValues(DEFAULT_ISSUER,
                OathRegistrationNode.UserAttributeToAccountNameMapping.USERNAME,
                DEFAULT_BG_COLOR,
                "",
                DEFAULT_GENERATE_RECOVERY_CODES,
                false);
    }

    private void whenNodeConfigHasDefaultValues(boolean generateRecoveryCodes,
            boolean postponeDeviceProfileStorage) {
        whenNodeConfigHasDefaultValues(
                DEFAULT_ISSUER,
                OathRegistrationNode.UserAttributeToAccountNameMapping.USERNAME,
                DEFAULT_BG_COLOR,
                "",
                generateRecoveryCodes,
                postponeDeviceProfileStorage
        );
    }

    private void whenNodeConfigHasDefaultValues(String issuer,
            OathRegistrationNode.UserAttributeToAccountNameMapping accountName,
            String bgColor,
            String imgUrl,
            boolean generateRecoveryCodes,
            boolean postponeDeviceProfileStorage) {
        config = mock(OathRegistrationConfig.class);
        given(config.issuer()).willReturn(issuer);
        given(config.accountName()).willReturn(accountName);
        given(config.bgColor()).willReturn(bgColor);
        given(config.imgUrl()).willReturn(imgUrl);
        given(config.generateRecoveryCodes()).willReturn(generateRecoveryCodes);
        given(config.algorithm()).willReturn(OathAlgorithm.TOTP);
        given(config.minSharedSecretLength()).willReturn(DEFAULT_MIN_SHARED_SECRET_LENGTH);
        given(config.passwordLength()).willReturn(NumberOfDigits.SIX_DIGITS);
        given(config.totpTimeInterval()).willReturn(DEFAULT_TOTP_INTERVAL);
        given(config.addChecksum()).willReturn(DEFAULT_CHECKSUM);
        given(config.totpHashAlgorithm()).willReturn(HashAlgorithm.HMAC_SHA1);
        given(config.scanQRCodeMessage()).willReturn(MAP_SCAN_MESSAGE);
        given(config.postponeDeviceProfileStorage()).willReturn(postponeDeviceProfileStorage);

        localizationHelper = mock(LocalizedMessageProvider.class);
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), eq(SCAN_QR_CODE_MSG_KEY)))
                .willReturn(MESSAGE);
        given(localizationHelper.getLocalizedMessageWithDefault(any(), any(), any(), anyString(), eq(NEXT_LABEL)))
                .willReturn(NEXT_LABEL);

        MultiFactorNodeDelegate multiFactorNodeDelegate = new MultiFactorNodeDelegate(
                mock(AuthenticatorDeviceServiceFactory.class)
        );

        oathRegistrationHelper = spy(
                new OathRegistrationHelper(
                        realm,
                        deviceProfileHelper,
                        multiFactorNodeDelegate,
                        r -> localizationHelper,
                        multiFactorRegistrationUtilities)
        );

        node = spy(
                new OathRegistrationNode(
                        config,
                        multiFactorNodeDelegate,
                        oathRegistrationHelper,
                        identityProvider
                )
        );
    }
}
