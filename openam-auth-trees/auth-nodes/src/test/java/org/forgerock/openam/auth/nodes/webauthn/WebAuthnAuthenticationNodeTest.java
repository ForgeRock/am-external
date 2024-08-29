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
 * Copyright 2020-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Test for WebAuthnAuthenticationNode
 */
public class WebAuthnAuthenticationNodeTest {

    @Mock
    WebAuthnAuthenticationNode.Config config;

    @Mock
    Realm realm;

    @Mock
    AMIdentity amIdentity;

    @Mock
    AuthenticationFlow authenticationFlow;

    ClientScriptUtilities clientScriptUtilities = new ClientScriptUtilities();

    @Mock
    ClientScriptUtilities mockClientScriptUtilities;

    SecureRandom secureRandom = new SecureRandom();

    AuthDataDecoder authDataDecoder = new AuthDataDecoder();

    @Mock
    AuthDataDecoder mockAuthDataDecoder;

    @Mock
    LegacyIdentityService identityService;

    @Mock
    IdentityStoreFactory identityStoreFactory;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    UserWebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    @Mock
    UserDeviceSettingsDao userDeviceSettingsDao;

    @Mock
    JWK jwk;

    WebAuthnAuthenticationNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        initMocks(this);
        given(identityService.getUniversalId(anyString(), anyString(), any()))
                .willReturn(Optional.of(UUID.randomUUID().toString()));
        given(realm.asPath()).willReturn("/");
        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(amIdentity.getType()).willReturn(IdType.USER);

        given(config.asScript()).willReturn(false);
        given(config.isRecoveryCodeAllowed()).willReturn(false);
        given(config.userVerificationRequirement()).willReturn(UserVerificationRequirement.PREFERRED);
        given(config.requiresResidentKey()).willReturn(false);
        given(config.timeout()).willReturn(100);
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));

        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any())).willReturn(
                Collections.singletonList(generateDevice()));

        mockNode(clientScriptUtilities, authDataDecoder);
    }

    private void mockNode(ClientScriptUtilities clientScriptUtilities, AuthDataDecoder authDataDecoder)
            throws DevicePersistenceException {
        node = new WebAuthnAuthenticationNode(config, realm, authenticationFlow, clientScriptUtilities,
                webAuthnDeviceProfileManager, secureRandom, authDataDecoder,
                new RecoveryCodeGenerator(secureRandom), identityService, identityStoreFactory, coreWrapper);

        node = spy(node);
        doReturn(Collections.singletonList(generateDevice())).when(node).getDeviceSettingsFromUsername(any(), any());
    }

    @Test
    public void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());
        AuthData authData = mock(AuthData.class);

        HiddenValueCallback hvc = mock(HiddenValueCallback.class);
        ClientAuthenticationScriptResponse response = mock(ClientAuthenticationScriptResponse.class);
        given(response.getUserHandle()).willReturn("bob");
        given((response.getCredentialId())).willReturn(EncodingUtilities.base64UrlEncode("test"));

        given(hvc.getValue()).willReturn("output");
        given(authenticationFlow.accept(any(), any(), any(AuthData.class), any(), any(), any(), any(), any(), any()))
                .willReturn(true);

        given(mockClientScriptUtilities.parseClientAuthenticationResponse(anyString(), anyBoolean()))
                .willReturn(response);
        given(mockAuthDataDecoder.decode(any())).willReturn(authData);
        mockNode(mockClientScriptUtilities, mockAuthDataDecoder);

        // When
        Action result = node.process(getContext(sharedState, transientState, List.of(hvc)));

        // Then
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    public void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob-2"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.identifiedIdentity.isEmpty());
    }

    @Test
    public void testCallback()
            throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(MetadataCallback.class);
        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        //New Attribute
        assertThat(callback.getOutputValue().get("_relyingPartyId").asString()).isEqualTo("example.com");
        assertThat(callback.getOutputValue().get("_allowCredentials").isList()).isTrue();
        assertThat(callback.getOutputValue().get("_allowCredentials").get(0).get("id").isList()).isTrue();
        assertThat(callback.getOutputValue().get("_action").asString()).isEqualTo("webauthn_authentication");
        //Retain the existing attribute
        assertThat(callback.getOutputValue().get("challenge").asString()).isNotEmpty();
        assertThat(callback.getOutputValue().get("allowCredentials").asString())
                .isEqualTo("allowCredentials: [{ \"type\": \"public-key\", "
                        + "\"id\": new Int8Array([116, 101, 115, 116]).buffer }]");
        assertThat(callback.getOutputValue().get("timeout").asString()).isEqualTo("100000");
        assertThat(callback.getOutputValue().get("userVerification").asString()).isEqualTo("preferred");
        assertThat(callback.getOutputValue().get("relyingPartyId").asString()).isEqualTo("rpId: \"example.com\",");
        assertThat(callback.getOutputValue().get("_type").asString()).isEqualTo("WebAuthn");

        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
    }

    @Test
    public void testCallbackAsScript() throws NodeProcessException {
        // Given
        given(config.asScript()).willReturn(true);
        given(config.isRecoveryCodeAllowed()).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(4);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        ScriptTextOutputCallback callback = (ScriptTextOutputCallback) result.callbacks.get(0);
        assertThat(callback.getMessage()).contains("document.getElementById(\"loginButton_0\").click()",
                "var allowRecoveryCode = 'true' === \"true\"");

        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    public void testCredentialIdNotFound() throws NodeProcessException, DevicePersistenceException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        //Given
        doReturn(Collections.singletonList(generateDevice())).when(node).getDeviceSettingsFromUsername(any(), any());
        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("id");
        //The device returned from user's device does not match with "dummy" credential ID
        String credentialId = EncodingUtilities.base64UrlEncode("dummy");
        hiddenValueCallback.setValue("dummy::1,2,3,4::1,2,3,4::" + credentialId);
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));

        //When
        Action result = node.process(treeContext);

        //Then
        assertThat(result.outcome).isEqualTo("failure");

    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

    private WebAuthnDeviceSettings generateDevice() {
        UserWebAuthnDeviceProfileManager manager = new UserWebAuthnDeviceProfileManager(userDeviceSettingsDao);
        return manager.createDeviceProfile(
                EncodingUtilities.base64UrlEncode("test"), jwk, null, null);

    }

}
