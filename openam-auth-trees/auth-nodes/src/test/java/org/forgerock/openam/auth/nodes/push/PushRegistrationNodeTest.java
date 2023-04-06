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

package org.forgerock.openam.auth.nodes.push;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.TIMEOUT_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_BG_COLOR;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_GENERATE_RECOVERY_CODES;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_ISSUER;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_TIMEOUT;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_REGISTRATION_TIMEOUT;
import static org.forgerock.openam.auth.nodes.push.PushRegistrationNode.REGISTER_DEVICE_POLL_INTERVAL;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.LocalizationHelper;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.UserAttributeToAccountNameMapping;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationUtilities;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.session.SessionCookies;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.idm.AMIdentity;

public class PushRegistrationNodeTest {

    @Mock
    PushRegistrationConfig config;
    @Mock
    private Realm realm;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private SessionCookies sessionCookies;
    @Mock
    private MessageIdFactory messageIdFactory;
    @Mock
    private PushDeviceProfileHelper deviceProfileHelper;
    @Mock
    private MessageId messageId;
    @Mock
    private ClusterMessageHandler messageHandler;
    @Mock
    AMIdentity userIdentity;
    @Mock
    LegacyIdentityService identityService;
    @Mock
    private LocalizationHelper localizationHelper;
    @Mock
    private MultiFactorRegistrationUtilities multiFactorRegistrationUtilities;

    PushRegistrationHelper pushRegistrationHelper;
    PushRegistrationNode node;

    static final String MESSAGE = "Scan the QR code image below with the ForgeRock Authenticator app to "
            + "register your device with your login";

    static final Map<Locale, String> MAP_SCAN_MESSAGE = new HashMap<>() {
        {
            put(Locale.CANADA, MESSAGE);
        }
    };

    @BeforeMethod
    public void setup() {
        initMocks(this);

        when(realm.asPath()).thenReturn("/");
        when(deviceProfileHelper.isDeviceSettingsStored(any())).thenReturn(false);
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

        given(messageIdFactory.create(DefaultMessageTypes.REGISTER))
                .willReturn(messageId);
        given(messageId.toString())
                .willReturn("REGISTER:8ae29738-52db-0ca4-caec-63c213360c341558531434231");
        given(messageId.getMessageType())
                .willReturn(DefaultMessageTypes.REGISTER);
        given(pushNotificationService.getMessageHandlers(any()))
                .willReturn(ImmutableMap.of(DefaultMessageTypes.REGISTER, messageHandler));
        given(deviceProfileHelper.createDeviceSettings(DEFAULT_ISSUER))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.createChallenge())
                .willReturn("KV1QiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2Ii=");
        given(deviceProfileHelper.encodeDeviceSettings(any()))
                .willReturn("ekd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9ab");
        doReturn(Collections.emptyMap())
                .when(pushRegistrationHelper).buildURIParameters(any(), anyString(), anyString(), any(), any());
        doReturn(mock(ScriptTextOutputCallback.class))
                .when(pushRegistrationHelper).createQRCodeCallback(any(), any(), anyString(), anyString());
        doReturn(mock(HiddenValueCallback.class))
                .when(pushRegistrationHelper).createHiddenCallback(any(), any(), anyString(), anyString());
        doReturn(mock(PollingWaitCallback.class))
                .when(pushRegistrationHelper).createPollingWaitCallback();
        doReturn(userIdentity)
                .when(node).getIdentity(any());
        doNothing()
                .when(pushRegistrationHelper).updateMessageDispatcher(any(), any(), any());

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(4);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(PollingWaitCallback.class);
        assertThat(result.sharedState.get(PUSH_DEVICE_PROFILE_KEY).asString()).isNotEmpty();
        assertThat(result.sharedState.get(PUSH_CHALLENGE_KEY).asString()).isNotEmpty();
        assertThat(result.sharedState.get(PUSH_REGISTRATION_TIMEOUT).asInteger())
                .isEqualTo(REGISTER_DEVICE_POLL_INTERVAL);
        assertThat(result.sharedState.get(MESSAGE_ID_KEY).asString()).isNotEmpty();
        String message = ((TextOutputCallback) result.callbacks.get(0)).getMessage();
        assertThat(message).contains(MESSAGE);
    }

    @Test
    public void processWhenStateIsNullReturnTimeoutOutcome()
            throws NodeProcessException, PushNotificationException, CoreTokenException {
        // Given
        whenNodeConfigHasDefaultValues();

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(messageId.getMessageType()).willReturn(DefaultMessageTypes.REGISTER);
        given(pushNotificationService.getMessageHandlers(any())).willReturn(
                ImmutableMap.of(DefaultMessageTypes.REGISTER, messageHandler));
        doReturn(null).when(pushRegistrationHelper).getMessageState(any());
        doReturn(userIdentity)
                .when(node).getIdentity(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(PollingWaitCallback.class)), getPushRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(TIMEOUT_OUTCOME_ID);
        assertThat(result.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_CHALLENGE_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(result.transientState).isNull();
    }

    @Test
    public void processWhenStateSuccessReturnSuccessOutcomeAndRecoveryCodes()
            throws CoreTokenException, NodeProcessException, PushNotificationException {
        // Given
        whenNodeConfigHasDefaultValues(true);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doReturn(MessageState.SUCCESS).when(pushRegistrationHelper).getMessageState(any());
        doReturn(json(object())).when(pushRegistrationHelper).deleteMessage(any());
        doNothing().when(pushRegistrationHelper).setUserToNotSkippable(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(PollingWaitCallback.class)), getPushRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_CHALLENGE_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(result.transientState.get(RECOVERY_CODE_KEY).asList())
                .containsExactlyInAnyOrder("z0WKEw0Wc8", "Ios4LnA2Qn");
        assertThat(result.transientState.get(RECOVERY_CODE_DEVICE_NAME).asString())
                .isEqualTo("Push Device");
    }

    @Test
    public void processWhenStateSuccessAndRecoveryCodesDisabledReturnSuccessOutcome()
            throws CoreTokenException, NodeProcessException, PushNotificationException {
        // Given
        whenNodeConfigHasDefaultValues(false);

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(false)))
                .willReturn(Collections.emptyList());
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doReturn(MessageState.SUCCESS).when(pushRegistrationHelper).getMessageState(any());
        doReturn(json(object())).when(pushRegistrationHelper).deleteMessage(any());
        doNothing().when(pushRegistrationHelper).setUserToNotSkippable(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(PollingWaitCallback.class)), getPushRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_CHALLENGE_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(result.transientState).isNull();
    }

    @Test
    public void processWhenStateDeniedReturnFailureOutcome()
            throws CoreTokenException, NodeProcessException, PushNotificationException {
        // Given
        whenNodeConfigHasDefaultValues();

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doReturn(MessageState.DENIED).when(pushRegistrationHelper).getMessageState(any());
        doReturn(json(object())).when(pushRegistrationHelper).deleteMessage(any());

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(PollingWaitCallback.class)), getPushRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE_OUTCOME_ID);
        assertThat(result.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_CHALLENGE_KEY)).isFalse();
        assertThat(result.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(result.transientState).isNull();
    }

    @Test
    public void processWhenStateUnknownReturnCallbacks()
            throws CoreTokenException, NodeProcessException, PushNotificationException {
        // Given
        whenNodeConfigHasDefaultValues();

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any())).
                willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Collections.emptyList());
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doReturn(MessageState.UNKNOWN).when(pushRegistrationHelper).getMessageState(any());
        doReturn(json(object())).when(pushRegistrationHelper).deleteMessage(any());
        doReturn(mock(MessageId.class)).when(pushRegistrationHelper).getMessageId(any());
        doReturn(Collections.emptyMap())
                .when(pushRegistrationHelper).buildURIParameters(any(), anyString(), anyString(), any(), any());
        doReturn(mock(ScriptTextOutputCallback.class))
                .when(pushRegistrationHelper).createQRCodeCallback(any(), any(), anyString(), anyString());
        doReturn(mock(HiddenValueCallback.class))
                .when(pushRegistrationHelper).createHiddenCallback(any(), any(), anyString(), anyString());
        doReturn(mock(PollingWaitCallback.class))
                .when(pushRegistrationHelper).createPollingWaitCallback();

        // When
        Action result = node.process(
                getContext(ImmutableList.of(mock(PollingWaitCallback.class)), getPushRegistrationSharedState())
        );

        // Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(PollingWaitCallback.class);
    }

    private JsonValue getPushRegistrationSharedState() {
        return json(
                object(
                    field(MESSAGE_ID_KEY, "REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220"),
                    field(PUSH_CHALLENGE_KEY, "KV1QiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2Ii="),
                    field(PUSH_DEVICE_PROFILE_KEY, "ekd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9ab="),
                    field(PUSH_REGISTRATION_TIMEOUT, REGISTER_DEVICE_POLL_INTERVAL)
                )
        );
    }

    private PushDeviceSettings getDeviceSettings() {
        PushDeviceSettings settings = new PushDeviceSettings();

        settings.setSharedSecret("olVsCC00XtifveplR0fI7ZeE3r0i3ei+lERaPESSoPg=");
        settings.setDeviceName("Push Device");

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
                UserAttributeToAccountNameMapping.USERNAME,
                DEFAULT_TIMEOUT,
                DEFAULT_BG_COLOR,
                "",
                DEFAULT_GENERATE_RECOVERY_CODES);
    }

    private void whenNodeConfigHasDefaultValues(boolean generateRecoveryCodes) {
        whenNodeConfigHasDefaultValues(
                DEFAULT_ISSUER,
                UserAttributeToAccountNameMapping.USERNAME,
                DEFAULT_TIMEOUT,
                DEFAULT_BG_COLOR,
                "",
                generateRecoveryCodes
        );
    }

    private void whenNodeConfigHasDefaultValues(String issuer,
                                                UserAttributeToAccountNameMapping accountName,
                                                int timeout,
                                                String bgColor,
                                                String imgUrl,
                                                boolean generateRecoveryCodes) {
        config = mock(PushRegistrationConfig.class);
        given(config.issuer()).willReturn(issuer);
        given(config.accountName()).willReturn(accountName);
        given(config.timeout()).willReturn(timeout);
        given(config.bgColor()).willReturn(bgColor);
        given(config.imgUrl()).willReturn(imgUrl);
        given(config.generateRecoveryCodes()).willReturn(generateRecoveryCodes);
        given(config.scanQRCodeMessage()).willReturn(MAP_SCAN_MESSAGE);

        localizationHelper = mock(LocalizationHelper.class);
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), anyString())).willReturn(MESSAGE);

        MultiFactorNodeDelegate multiFactorNodeDelegate = new MultiFactorNodeDelegate(
                mock(AuthenticatorDeviceServiceFactory.class)
        );

        pushRegistrationHelper = spy(
               new PushRegistrationHelper(
                       realm,
                       pushNotificationService,
                       sessionCookies,
                       messageIdFactory,
                       deviceProfileHelper,
                       multiFactorNodeDelegate,
                       localizationHelper,
                       multiFactorRegistrationUtilities)
        );

        node = spy(
                new PushRegistrationNode(
                        config,
                        realm,
                        coreWrapper,
                        multiFactorNodeDelegate,
                        identityService,
                        pushRegistrationHelper
                )
        );

    }

}
