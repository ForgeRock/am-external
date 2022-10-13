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
 * Copyright 2020-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oath;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
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
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_CHECKSUM;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MIN_SHARED_SECRET_LENGTH;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
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
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.LocalizationHelper;
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
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.idm.AMIdentity;

public class OathRegistrationNodeTest {

    @Mock
    OathRegistrationNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private OathDeviceProfileHelper deviceProfileHelper;
    @Mock
    AMIdentity userIdentity;
    @Mock
    IdentityUtils identityUtils;
    @Mock
    private LocalizationHelper localizationHelper;

    OathRegistrationNode node;

    static final String MESSAGE = "Scan the QR code image below with the ForgeRock Authenticator app to "
            + "register your device with your login";

    static final Map<Locale, String> MAP_SCAN_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, MESSAGE);
    }};

    static final Locale DEFAULT_LOCALE = Locale.CANADA;

    @BeforeMethod
    public void setup() {
        initMocks(this);

        when(deviceProfileHelper.isDeviceSettingsStored(any())).thenReturn(false);
        when(coreWrapper.getIdentity(anyString(), (Realm) any())).thenReturn(userIdentity);
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
    public void processShouldStartRegistration() throws Exception {
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
                .when(node).createQRCodeCallback(any(), any());
        doReturn(mock(HiddenValueCallback.class))
                .when(node).createHiddenCallback(any(), any());
        doReturn(userIdentity)
                .when(node).getIdentity(any());
        doReturn("forgerock")
                .when(node).getBase32Secret(anyString());

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(4);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.sharedState.get(OATH_DEVICE_PROFILE_KEY).asString()).isNotEmpty();
        String message = ((TextOutputCallback) result.callbacks.get(0)).getMessage();
        assertThat(message).contains(MESSAGE);
    }

    @Test
    public void processSuccessfulRegistrationReturnSuccessOutcomeAndRecoveryCodes()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(true);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(node).setUserToNotSkippable(any(), anyString());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)), getOathRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState).isNull();
        assertThat(result.transientState.get(RECOVERY_CODE_KEY).asList())
                .containsExactlyInAnyOrder("z0WKEw0Wc8", "Ios4LnA2Qn");
        assertThat(result.transientState.get(RECOVERY_CODE_DEVICE_NAME).asString())
                .isEqualTo("Oath Device");
    }

    @Test
    public void processSuccessfulRegistrationWhenRecoveryCodesDisabledReturnSuccessOutcome()
            throws NodeProcessException {
        // Given
        whenNodeConfigHasDefaultValues(false);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(node).setUserToNotSkippable(any(), anyString());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(ConfirmationCallback.class)), getOathRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.transientState).isNull();
    }

    private JsonValue getOathRegistrationSharedState() {
        return json(
                object(
                    field(USERNAME, "rod"),
                    field(REALM, "root"),
                    field(OATH_DEVICE_PROFILE_KEY, "ekd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9ab=")
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
                DEFAULT_GENERATE_RECOVERY_CODES);
    }

    private void whenNodeConfigHasDefaultValues(boolean generateRecoveryCodes) {
        whenNodeConfigHasDefaultValues(
                DEFAULT_ISSUER,
                OathRegistrationNode.UserAttributeToAccountNameMapping.USERNAME,
                DEFAULT_BG_COLOR,
                "",
                generateRecoveryCodes
        );
    }

    private void whenNodeConfigHasDefaultValues(String issuer,
                                                OathRegistrationNode.UserAttributeToAccountNameMapping accountName,
                                                String bgColor,
                                                String imgUrl,
                                                boolean generateRecoveryCodes) {
        config = mock(OathRegistrationNode.Config.class);
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

        localizationHelper = mock(LocalizationHelper.class);
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), anyString())).willReturn(MESSAGE);

        node = spy(
                new OathRegistrationNode(
                        config,
                        realm,
                        coreWrapper,
                        deviceProfileHelper,
                        new MultiFactorNodeDelegate(mock(AuthenticatorDeviceServiceFactory.class)),
                        identityUtils,
                        localizationHelper
                )
        );
    }

}
