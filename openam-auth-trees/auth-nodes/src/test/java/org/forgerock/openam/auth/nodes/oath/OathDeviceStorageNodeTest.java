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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_ENABLE_RECOVERY_CODE_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sun.identity.idm.AMIdentity;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class OathDeviceStorageNodeTest {

    @Mock
    private Realm realm;
    @Mock
    private OathDeviceProfileHelper deviceProfileHelper;
    @Mock
    private AMIdentity userIdentity;

    private OathDeviceStorageNode node;
    @Mock
    private NodeUserIdentityProvider identityProvider;

    @Test
    void processReturnFailureOutcomeIfDeviceDataNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        initNode();

        // When
        Action result = node.process(getContext(sharedState, transientState));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE_OUTCOME_ID);
    }

    @Test
    void processSaveDeviceReturnSuccessOutcomeAndRecoveryCodes()
            throws NodeProcessException {
        // Given
        initNode();

        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(node).setUserToNotSkippable(any(), anyString());

        // When
        Action result = node.process(
                getContext(getOathDeviceProfileFromSharedState(true))
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
    void processSuccessfulRegistrationWhenRecoveryCodesDisabled()
            throws NodeProcessException {
        // Given
        initNode();

        when(deviceProfileHelper.isDeviceSettingsStored(any())).thenReturn(false);
        given(deviceProfileHelper.getDeviceProfileFromSharedState(any(), any()))
                .willReturn(getDeviceSettings());
        given(deviceProfileHelper.saveDeviceSettings(any(), any(), any(), eq(true)))
                .willReturn(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn"));
        doReturn(mock(AMIdentity.class)).when(node).getIdentity(any());
        doNothing().when(node).setUserToNotSkippable(any(), anyString());

        // When
        Action result = node.process(
                getContext(getOathDeviceProfileFromSharedState(false))
        );

        // Then
        assertThat(result.outcome).isEqualTo(SUCCESS_OUTCOME_ID);
        assertThat(result.sharedState).isNull();
        assertThat(result.transientState).isNull();
    }

    private JsonValue getOathDeviceProfileFromSharedState(boolean recoveryCode) {
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
                sharedState, new ExternalRequestContext.Builder().build(),
                emptyList(), Optional.empty()
        );
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return new TreeContext(
                sharedState, transientState, new ExternalRequestContext.Builder().build(),
                emptyList(), Optional.empty()
        );
    }

    private void initNode() {
        node = spy(
                new OathDeviceStorageNode(
                        realm,
                        deviceProfileHelper,
                        new MultiFactorNodeDelegate(mock(AuthenticatorDeviceServiceFactory.class)),
                        identityProvider
                )
        );
    }

}
