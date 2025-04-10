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

package org.forgerock.openam.auth.nodes.webauthn;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.WEBAUTHN_EXTENSIONS;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.ATTESTED_CREDENTIAL_DATA_INCLUDED;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.AUTHENTICATOR_ATTACHMENT;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.BACKUP_ELIGIBILITY;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.BACKUP_STATE;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.ERROR_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.EXTENSION_DATA_INCLUDED;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.FLAGS;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.UNSUPPORTED_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.USER_PRESENT;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.USER_VERIFIED;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_ASSERTION_INFO;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_DEVICE_UUID;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode.NO_DEVICE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode.RECOVERY_CODE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode.SIGN_COUNT_MISMATCH_OUTCOME_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;

/**
 * Test for WebAuthnAuthenticationNode
 */
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class WebAuthnAuthenticationNodeTest {

    @Mock
    WebAuthnAuthenticationNode.Config config;

    @Mock
    Realm realm;

    @Mock
    AMIdentity amIdentity;

    @Mock
    AuthenticationFlow authenticationFlow;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    ClientScriptUtilities mockClientScriptUtilities;

    SecureRandom secureRandom = new SecureRandom();

    @Mock
    AuthDataDecoder mockAuthDataDecoder;

    @Mock
    IdentityStoreFactory identityStoreFactory;

    @Mock
    UserWebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    @Mock
    UserDeviceSettingsDao userDeviceSettingsDao;

    @Mock
    WebAuthnOutcomeDeserializer webAuthnOutcomeDeserializer;

    @Mock
    JWK jwk;

    WebAuthnAuthenticationNode node;
    @Mock
    NodeUserIdentityProvider identityProvider;

    @BeforeEach
    void setup() throws DevicePersistenceException {
        node = null;
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
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

        given(webAuthnOutcomeDeserializer.deserialize("object"))
                .willReturn(new WebAuthnOutcome("dummy", Optional.of("platform")));

        mockNode();
    }

    private void mockNode()
            throws DevicePersistenceException {
        node = new WebAuthnAuthenticationNode(config, realm, authenticationFlow, mockClientScriptUtilities,
                webAuthnDeviceProfileManager, secureRandom, mockAuthDataDecoder,
                new RecoveryCodeGenerator(secureRandom), identityStoreFactory, identityProvider,
                webAuthnOutcomeDeserializer);

        node = spy(node);
        doReturn(Collections.singletonList(generateDevice())).when(node).getDeviceSettingsFromUsername(any(), any());
    }

    @Test
    void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());
        AttestationFlags flags = mock(AttestationFlags.class);
        AuthData authData = new AuthData(null, flags, 0, null, null);

        HiddenValueCallback hvc = new HiddenValueCallback("webAuthnOutcome", "output");
        ClientAuthenticationScriptResponse response = mock(ClientAuthenticationScriptResponse.class);
        given(response.getUserHandle()).willReturn("bob");
        given((response.getCredentialId())).willReturn(EncodingUtilities.base64UrlEncode("test"));

        given(authenticationFlow.accept(any(), any(), any(AuthData.class), any(), any(), any(), any(), any(), any()))
                .willReturn(true);

        doReturn(response).when(mockClientScriptUtilities).parseClientAuthenticationResponse(anyString(), anyBoolean());
        given(mockAuthDataDecoder.decode(any())).willReturn(authData);
        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willAnswer(invocation -> new WebAuthnOutcome(invocation.getArgument(0), Optional.empty()));

        // When
        Action result = node.process(getContext(sharedState, transientState, List.of(hvc)));

        // Then
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);

        assertThat(sharedState.isDefined(WEB_AUTHN_DEVICE_UUID)).isTrue();
        assertThat(sharedState.isDefined(WEB_AUTHN_DEVICE_NAME)).isTrue();
        assertThat(sharedState.get(WEB_AUTHN_DEVICE_UUID).asString()).isNotNull();
        assertThat(sharedState.get(WEB_AUTHN_DEVICE_NAME).asString()).isEqualTo("daedalus");
        assertThat(transientState.isDefined(WEB_AUTHN_ASSERTION_INFO)).isTrue();
    }

    @Test
    void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob-2"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.identifiedIdentity.isEmpty()).isTrue();
    }

    @Test
    void testProcessAddsAssertionInfoWhenWebAuthnObjectIsValid() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());
        AttestationFlags flags = mock(AttestationFlags.class);
        given(flags.isUserPresent()).willReturn(true);
        AuthData authData = new AuthData(null, flags, 0, null, null);

        HiddenValueCallback outcome = new HiddenValueCallback("webAuthnOutcome", "output");
        ClientAuthenticationScriptResponse response = mock(ClientAuthenticationScriptResponse.class);
        given((response.getCredentialId())).willReturn(EncodingUtilities.base64UrlEncode("test"));

        given(authenticationFlow.accept(any(), any(), any(AuthData.class), any(), any(), any(), any(), any(), any()))
                .willReturn(true);

        doReturn(response).when(mockClientScriptUtilities).parseClientAuthenticationResponse(anyString(), anyBoolean());
        given(mockAuthDataDecoder.decode(any())).willReturn(authData);
        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willAnswer(invocation -> new WebAuthnOutcome(invocation.getArgument(0), Optional.of("platform")));

        // When
        node.process(getContext(sharedState, transientState, List.of(outcome)));

        // Then
        assertThat(transientState.isDefined(WEB_AUTHN_ASSERTION_INFO)).isTrue();

        JsonValue jsonValue = transientState.get(WEB_AUTHN_ASSERTION_INFO);

        assertThat(jsonValue.get(AUTHENTICATOR_ATTACHMENT).asString()).isEqualTo("platform");
        JsonValue jsonFlags = jsonValue.get(FLAGS);
        assertThat(jsonFlags.get(USER_PRESENT).asBoolean()).isTrue();
        assertThat(jsonFlags.get(USER_VERIFIED).asBoolean()).isFalse();
        assertThat(jsonFlags.get(ATTESTED_CREDENTIAL_DATA_INCLUDED).asBoolean()).isFalse();
        assertThat(jsonFlags.get(EXTENSION_DATA_INCLUDED).asBoolean()).isFalse();
        assertThat(jsonFlags.get(BACKUP_ELIGIBILITY).asBoolean()).isFalse();
        assertThat(jsonFlags.get(BACKUP_STATE).asBoolean()).isFalse();
    }

    @Test
    void testCallback()
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
        assertThat(callback.getOutputValue().get("extensions").asMap()).isEmpty();

        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
    }

    @Test
    void testCallbackAsScript() throws NodeProcessException {
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

    // OPENAM-23262: This is a quick and dirty test case to catch the JQuery syntax error of an extra comma in
    // the options script object.
    @Test
    void testCallbackAsScriptEmptyItems() throws NodeProcessException {
        // Given
        given(config.asScript()).willReturn(true);
        given(config.isRecoveryCodeAllowed()).willReturn(false);
        given(config.requiresResidentKey()).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        String message = ((ScriptTextOutputCallback) result.callbacks.get(0)).getMessage();
        String options = message.substring(message.indexOf("options = {"), message.indexOf("};") + 1)
                .substring(10).replaceAll(" ", "").replaceAll("\n", "");
        assertThat(options.contains(",,")).isFalse();
    }

    // AME-30253: This is another quick and dirty test case to catch the syntax error of missing commas between the
    // expected object attributes.
    @Test
    void testCallbackAsScriptNoMissingCommas() throws NodeProcessException {
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
        String message = ((ScriptTextOutputCallback) result.callbacks.get(0)).getMessage();
        String options = message.substring(message.indexOf("options = {"), message.indexOf("};") + 1)
                .substring(10).replaceAll(" ", "").replaceAll("\n", "");

        assertThat(options.contains("{rpId")).isTrue();
        assertThat(options.contains(",challenge")).isTrue();
        assertThat(options.contains(",timeout")).isTrue();
        assertThat(options.contains(",userVerification")).isTrue();
        assertThat(options.contains(",extensions")).isTrue();
        assertThat(options.contains(",allowCredentials")).isTrue();
    }

    @Test
    void testCredentialIdNotFound() throws NodeProcessException, DevicePersistenceException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        //Given
        doReturn(Collections.singletonList(generateDevice())).when(node).getDeviceSettingsFromUsername(any(), any());
        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("webAuthnOutcome");
        //The device returned from user's device does not match with "dummy" credential ID
        String credentialId = EncodingUtilities.base64UrlEncode("dummy");
        hiddenValueCallback.setValue("dummy::1,2,3,4::1,2,3,4::" + credentialId);
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));
        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willAnswer(invocation -> new WebAuthnOutcome(invocation.getArgument(0), Optional.empty()));

        //When
        Action result = node.process(treeContext);

        //Then
        assertThat(result.outcome).isEqualTo("failure");

    }

    @Test
    void testPassesThroughExtensionsFromSharedState() throws Exception {
        JsonValue sharedState = json(object(field(WEBAUTHN_EXTENSIONS, object(field("exts", true)))));
        JsonValue transientState = json(object());
        TreeContext context = getContext(sharedState, transientState, emptyList());

        Action result = node.process(context);

        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("extensions").asMap()).hasSize(1).containsEntry("exts", true);
    }

    @Test
    void testPassesThroughExtensionsFromTransientState() throws Exception {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object(field(WEBAUTHN_EXTENSIONS, object(field("exts", true)))));
        TreeContext context = getContext(sharedState, transientState, emptyList());

        Action result = node.process(context);

        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("extensions").asMap()).hasSize(1).containsEntry("exts", true);
    }

    @Test
    void testEnforcesExtensionsAreAMap() {
        JsonValue sharedState = json(object(field(WEBAUTHN_EXTENSIONS, "{\"exts\": true}")));
        JsonValue transientState = json(object());
        TreeContext context = getContext(sharedState, transientState, emptyList());

        assertThatThrownBy(() -> node.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Extensions must be a map");
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

    private WebAuthnDeviceSettings generateDevice() {
        UserWebAuthnDeviceProfileManager manager = new UserWebAuthnDeviceProfileManager(userDeviceSettingsDao);
        return manager.createDeviceProfile(
                EncodingUtilities.base64UrlEncode("test"), jwk, null, "daedalus", 1);

    }

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() {
        // given
        WebAuthnAuthenticationNode.OutcomeProvider outcomeProvider = new WebAuthnAuthenticationNode.OutcomeProvider();

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id)).containsExactly(
                UNSUPPORTED_OUTCOME_ID,
                NO_DEVICE_OUTCOME_ID,
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                ERROR_OUTCOME_ID,
                RECOVERY_CODE_OUTCOME_ID,
                SIGN_COUNT_MISMATCH_OUTCOME_ID
        );
    }

    @Test
    void shouldReturnAllOutcomesWhenRecoveryCodeIsAllowed() {
        // given
        WebAuthnAuthenticationNode.OutcomeProvider outcomeProvider = new WebAuthnAuthenticationNode.OutcomeProvider();
        var attributes = json(object(field("isRecoveryCodeAllowed", true)));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id)).containsExactly(
                UNSUPPORTED_OUTCOME_ID,
                "noDevice",
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                ERROR_OUTCOME_ID,
                "recoveryCode"
        );
    }

    @Test
    void shouldNotReturnRecoveryCodeWhenRecoveryCodeIsNotAllowed() {
        // given
        WebAuthnAuthenticationNode.OutcomeProvider outcomeProvider = new WebAuthnAuthenticationNode.OutcomeProvider();
        var attributes = json(object(field("isRecoveryCodeAllowed", false)));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id)).containsExactly(
                UNSUPPORTED_OUTCOME_ID,
                "noDevice",
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                ERROR_OUTCOME_ID
        );
    }

    @Test
    void shouldNotReturnRecoveryCodeWhenNodeAttributesIsNull() {
        // given
        WebAuthnAuthenticationNode.OutcomeProvider outcomeProvider = new WebAuthnAuthenticationNode.OutcomeProvider();
        var attributes = json(null);

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id)).containsExactly(
                UNSUPPORTED_OUTCOME_ID,
                "noDevice",
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                ERROR_OUTCOME_ID
        );
    }

}
