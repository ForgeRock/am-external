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
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.EXCEED_DEVICE_LIMIT_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.EXTENSION_DATA_INCLUDED;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.FLAGS;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.UNSUPPORTED_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.USER_PRESENT;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.USER_VERIFIED;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_ATTESTATION_INFO;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_DEVICE_DATA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationResponse;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlowFactory;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.idm.AMIdentity;

/**
 * Test for WebAuthnRegistrationNode
 */
@ExtendWith(MockitoExtension.class)
public class WebAuthnRegistrationNodeTest {

    @Mock
    WebAuthnRegistrationNode.Config config;

    @Mock
    Realm realm;

    @Mock
    AMIdentity amIdentity;

    @Mock
    RegisterFlow registerFlow;

    ClientScriptUtilities clientScriptUtilities = new ClientScriptUtilities();

    SecureRandom secureRandom = new SecureRandom();

    @Mock
    RecoveryCodeGenerator recoveryCodeGenerator;

    @Mock
    WebAuthnDeviceJsonUtils webAuthnDeviceJsonUtils;

    @Mock
    NodeUserIdentityProvider identityProvider;

    @Mock
    UserWebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    @Mock
    UserDeviceSettingsDao userDeviceSettingsDao;

    @Mock
    JWK jwk;

    @Mock
    WebAuthnOutcomeDeserializer webAuthnOutcomeDeserializer;

    WebAuthnRegistrationNode node;

    @BeforeEach
    void setup() {
        node = null;
        RegisterFlowFactory registerFlowFactory = mock(RegisterFlowFactory.class);
        given(registerFlowFactory.create(any(), any())).willReturn(registerFlow);

        node = new WebAuthnRegistrationNode(config, realm, registerFlowFactory, clientScriptUtilities,
                webAuthnDeviceProfileManager, secureRandom, recoveryCodeGenerator,
                webAuthnDeviceJsonUtils, identityProvider, webAuthnOutcomeDeserializer);
    }

    private void commonStubbings() throws Exception {
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
        given(amIdentity.getName()).willReturn("usernameFromAmIdentity");
        given(config.asScript()).willReturn(false);
        given(config.attestationPreference()).willReturn(AttestationPreference.NONE);
        given(config.userVerificationRequirement()).willReturn(UserVerificationRequirement.PREFERRED);
        given(config.authenticatorAttachment()).willReturn(AuthenticatorAttachment.PLATFORM);
        given(config.requiresResidentKey()).willReturn(false);
        given(config.residentKeyRequirement()).willReturn(ResidentKeyRequirement.DISCOURAGED);
        given(config.timeout()).willReturn(100);
        given(config.relyingPartyName()).willReturn("relyingPartyName");
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.acceptedSigningAlgorithms()).willReturn(Collections.singleton(CoseAlgorithm.ES256));
        given(config.excludeCredentials()).willReturn(true);
        given(config.postponeDeviceProfileStorage()).willReturn(true);

        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any()))
                .willReturn(Collections.singletonList(generateDevice()));
    }

    @Test
    void testCallback() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "usernameFromSharedState"), field(REALM, "root")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(MetadataCallback.class);
        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("_action").asString()).isEqualTo("webauthn_registration");
        assertThat(callback.getOutputValue().get("_relyingPartyId").asString()).isEqualTo("example.com");
        assertThat(callback.getOutputValue().get("_authenticatorSelection").get("userVerification")
                .asString()).isEqualTo("preferred");

        assertThat(callback.getOutputValue().get("_authenticatorSelection").get("authenticatorAttachment")
                .asString()).isEqualTo("platform");
        assertThat(callback.getOutputValue().get("_pubKeyCredParams").isList()).isTrue();
        assertThat(callback.getOutputValue().get("_pubKeyCredParams").get(0).get("type")
                .asString()).isEqualTo("public-key");
        assertThat(callback.getOutputValue().get("_pubKeyCredParams").get(0).get("alg").isNumber()).isTrue();
        assertThat(callback.getOutputValue().get("_pubKeyCredParams").get(0).get("alg").asInteger()).isEqualTo(-7);
        assertThat(callback.getOutputValue().get("_excludeCredentials").isList()).isTrue();
        assertThat(callback.getOutputValue().get("_excludeCredentials").get(0).get("type")
                .asString()).isEqualTo("public-key");
        assertThat(callback.getOutputValue().get("_excludeCredentials").get(0).get("id").isList()).isTrue();

        //Original attributes
        assertThat(callback.getOutputValue().get("challenge").asString()).isNotEmpty();
        assertThat(callback.getOutputValue().get("attestationPreference").asString()).isEqualTo("none");
        assertThat(callback.getOutputValue().get("userName").asString()).isEqualTo("usernameFromAmIdentity");
        assertThat(callback.getOutputValue().get("userId").asString()).isNotEmpty();
        assertThat(callback.getOutputValue().get("relyingPartyName").asString()).isEqualTo("relyingPartyName");
        assertThat(callback.getOutputValue().get("authenticatorSelection").asString())
                .isEqualTo("{\"userVerification\":\"preferred\",\"authenticatorAttachment\":\"platform\""
                        + ",\"residentKey\":\"discouraged\"}");
        assertThat(callback.getOutputValue().get("pubKeyCredParams").asString())
                .isEqualTo("[ { \"type\": \"public-key\", \"alg\": -7 } ]");
        assertThat(callback.getOutputValue().get("timeout").asString()).isEqualTo("100000");
        assertThat(callback.getOutputValue().get("excludeCredentials").asString())
                .isEqualTo("{ \"type\": \"public-key\", \"id\": new Int8Array([116, 101, 115, 116]).buffer }");
        assertThat(callback.getOutputValue().get("displayName").asString()).isEqualTo("usernameFromAmIdentity");
        assertThat(callback.getOutputValue().get("relyingPartyId").asString()).isEqualTo("id: \"example.com\",");
        assertThat(callback.getOutputValue().get("_type").asString()).isEqualTo("WebAuthn");

        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
    }

    @Test
    void testCallbackUsernameAttribute() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "usernameFromSharedState"), field(REALM, "root")));
        JsonValue transientState = json(object());
        given(amIdentity.getAttribute(eq("_username"))).willReturn(Set.of("usernameFromAttribute"));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);

        // Then
        assertThat(callback.getOutputValue().get("userName").asString()).isEqualTo("usernameFromAttribute");
        assertThat(callback.getOutputValue().get("displayName").asString()).isEqualTo("usernameFromAttribute");
    }

    @Test
    void testCallbackDisplayNameSharedState() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "usernameFromSharedState"), field(REALM, "root")));
        JsonValue transientState = json(object());
        given(config.displayNameSharedState()).willReturn(Optional.of(USERNAME));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);

        // Then
        assertThat(callback.getOutputValue().get("userName").asString()).isEqualTo("usernameFromAmIdentity");
        assertThat(callback.getOutputValue().get("displayName").asString()).isEqualTo("usernameFromSharedState");
    }

    @Test
    void testCallbackUsernameAttributeAndDisplayNameSharedState() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "usernameFromSharedState"), field(REALM, "root")));
        JsonValue transientState = json(object());
        given(config.displayNameSharedState()).willReturn(Optional.of(USERNAME));
        given(amIdentity.getAttribute(eq("_username"))).willReturn(Set.of("usernameFromAttribute"));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);

        // Then
        assertThat(callback.getOutputValue().get("userName").asString()).isEqualTo("usernameFromAttribute");
        assertThat(callback.getOutputValue().get("displayName").asString()).isEqualTo("usernameFromSharedState");
    }

    @Test
    void testCallbackWithDeviceLimit() throws Exception {
        given(config.maxSavedDevices()).willReturn(2);
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
        given(amIdentity.getName()).willReturn("usernameFromAmIdentity");
        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any()))
                .willReturn(Collections.singletonList(generateDevice()));
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        given(config.postponeDeviceProfileStorage()).willReturn(false);
        given(config.maxSavedDevices()).willReturn(2);
        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any())).willReturn(
                Arrays.asList(generateDevice(), generateDevice()));

        //When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("exceedDeviceLimit");
    }

    @Test
    void testDeviceName() throws Exception {
        //given
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.postponeDeviceProfileStorage()).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("webAuthnOutcome");
        hiddenValueCallback.setValue("dummy::1,2,3,4::dummy::customDeviceName");
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));

        final RsaJWK jwk = createRsaJwk();
        AttestationObject attestationObject = new AttestationObject(null, new AuthData(null,
                getAttestationFlags(), 0,
                new AttestedCredentialData(new Aaguid(UUID.randomUUID()), 0, "credentialId".getBytes(),
                        jwk, null), null), null);

        given(registerFlow.accept(any(), any(), any(), any(), any())).willReturn(
                new AttestationResponse(attestationObject, null));

        given(webAuthnDeviceProfileManager.createDeviceProfile(any(), any(), any(), any(), anyInt()))
                .willCallRealMethod();
        given(webAuthnDeviceJsonUtils.toJsonValue(any())).willCallRealMethod();
        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willAnswer(invocation -> new WebAuthnOutcome(invocation.getArgument(0), Optional.empty()));

        //when
        Action result = node.process(treeContext);

        //then
        JsonValue value = result.transientState.get(WEB_AUTHN_DEVICE_DATA);
        assertThat(value.get("deviceName").asString()).isEqualTo("customDeviceName");
        assertThat(result.transientState.isDefined(WEB_AUTHN_ATTESTATION_INFO)).isTrue();
    }

    @Test
    void testWithoutDeviceName() throws Exception {
        //given
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.postponeDeviceProfileStorage()).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("webAuthnOutcome");
        hiddenValueCallback.setValue("dummy::1,2,3,4::dummy");
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));

        final RsaJWK jwk = createRsaJwk();

        AttestationObject attestationObject = new AttestationObject(null, new AuthData(null,
                getAttestationFlags(), 0,
                new AttestedCredentialData(new Aaguid(UUID.randomUUID()), 0, "credentialId".getBytes(),
                        jwk, null), null), null);

        given(registerFlow.accept(any(), any(), any(), any(), any())).willReturn(
                new AttestationResponse(attestationObject, null));

        given(webAuthnDeviceProfileManager.createDeviceProfile(any(), any(), any(), any(), anyInt()))
                .willCallRealMethod();
        given(webAuthnDeviceJsonUtils.toJsonValue(any())).willCallRealMethod();
        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willAnswer(invocation -> new WebAuthnOutcome(invocation.getArgument(0), Optional.empty()));

        //when
        Action result = node.process(treeContext);
        JsonValue value = result.transientState.get(WEB_AUTHN_DEVICE_DATA);

        //then
        assertThat(value.get("deviceName").asString()).isEqualTo("New Security Key");
    }

    @Test
    public void assertWebAuthnObjectProcessed() throws Exception {
        //given
        AttestationFlags flags = getAttestationFlags();
        String expectedAuthAttachment = "platform";

        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.postponeDeviceProfileStorage()).willReturn(true);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("webAuthnOutcome", json(object(
                field("legacyData", "dummy::1,2,3,4::dummy::customDeviceName"),
                field("authenticatorAttachment", "platform"))).toString());
        HiddenValueCallback hiddenValueCallback2 = new HiddenValueCallback("webAuthnObject", "objectData");

        // No object, JSON contains stuff, JSON missing specific field, just a string.

        treeContext = treeContext.copyWithCallbacks(Arrays.asList(hiddenValueCallback, hiddenValueCallback2));

        final RsaJWK jwk = createRsaJwk();
        AttestationObject attestationObject = new AttestationObject(null, new AuthData(null,
                flags, 0,
                new AttestedCredentialData(new Aaguid(UUID.randomUUID()), 0, "credentialId".getBytes(),
                        jwk, null), null), null);

        given(registerFlow.accept(any(), any(), any(), any(), any())).willReturn(
                new AttestationResponse(attestationObject, null));

        given(webAuthnOutcomeDeserializer.deserialize(any()))
                .willReturn(new WebAuthnOutcome("dummy::1,2,3,4::dummy::customDeviceName", Optional.of("platform")));

        given(webAuthnDeviceProfileManager.createDeviceProfile(any(), any(), any(), any(), anyInt()))
                .willCallRealMethod();
        given(webAuthnDeviceJsonUtils.toJsonValue(any())).willCallRealMethod();

        //when
        Action result = node.process(treeContext);

        //then
        assertThat(result.transientState.isDefined(WEB_AUTHN_ATTESTATION_INFO)).isTrue();
        JsonValue jsonValue = result.transientState.get(WEB_AUTHN_ATTESTATION_INFO);
        assertThat(jsonValue.get(AUTHENTICATOR_ATTACHMENT).asString())
                .isEqualTo(expectedAuthAttachment);
        JsonValue jsonFlags = jsonValue.get(FLAGS);
        assertThat(jsonFlags.get(USER_PRESENT).asBoolean()).isTrue();
        assertThat(jsonFlags.get(USER_VERIFIED).asBoolean()).isFalse();
        assertThat(jsonFlags.get(ATTESTED_CREDENTIAL_DATA_INCLUDED).asBoolean()).isTrue();
        assertThat(jsonFlags.get(EXTENSION_DATA_INCLUDED).asBoolean()).isFalse();
        assertThat(jsonFlags.get(BACKUP_ELIGIBILITY).asBoolean()).isFalse();
        assertThat(jsonFlags.get(BACKUP_STATE).asBoolean()).isTrue();
    }

    private static AttestationFlags getAttestationFlags() {
        BitSet validFlags = new BitSet(7);
        validFlags.set(0);
        validFlags.set(4);
        validFlags.set(6);
        AttestationFlags flags = new AttestationFlags(validFlags);
        return flags;
    }

    private static RsaJWK createRsaJwk() {
        return RsaJWK.parse("{"
                + "      \"kty\": \"RSA\","
                + "      \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
                + "            VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn6"
                + "            4tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FD"
                + "            W2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n9"
                + "            1CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINH"
                + "            aQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
                + "      \"e\": \"AQAB\","
                + "      \"alg\": \"RS256\","
                + "      \"kid\": \"2011-04-29\""
                + "     }");
    }

    @Test
    void testPassesThroughExtensionsFromSharedState() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(WEBAUTHN_EXTENSIONS, object(field("exts", true))),
                field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());
        TreeContext context = getContext(sharedState, transientState, emptyList());

        Action result = node.process(context);

        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("extensions").asMap()).hasSize(1).containsEntry("exts", true);
    }

    @Test
    void testPassesThroughExtensionsFromTransientState() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object(field(WEBAUTHN_EXTENSIONS, object(field("exts", true)))));
        TreeContext context = getContext(sharedState, transientState, emptyList());

        Action result = node.process(context);

        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("extensions").asMap()).hasSize(1).containsEntry("exts", true);
    }

    @Test
    void testDefaultsToEmptyMap() throws Exception {
        commonStubbings();
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());
        TreeContext context = getContext(sharedState, transientState, emptyList());

        Action result = node.process(context);

        MetadataCallback callback = (MetadataCallback) result.callbacks.get(0);
        assertThat(callback.getOutputValue().get("extensions").asMap()).isEmpty();
    }

    @Test
    void testEnforcesExtensionsAreAMap() {
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
        given(config.asScript()).willReturn(false);
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.postponeDeviceProfileStorage()).willReturn(true);
        JsonValue sharedState = json(object(field(WEBAUTHN_EXTENSIONS, "{\"exts\": true}"),
                field(USERNAME, "bob"), field(REALM, "root")));
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
                EncodingUtilities.base64UrlEncode("test"), jwk, null, null, 1);

    }

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() {
        // given
        WebAuthnRegistrationNode.OutcomeProvider outcomeProvider = new WebAuthnRegistrationNode.OutcomeProvider();

        // when
        var outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id))
                .containsExactly(UNSUPPORTED_OUTCOME_ID,
                        SUCCESS_OUTCOME_ID,
                        FAILURE_OUTCOME_ID,
                        ERROR_OUTCOME_ID,
                        EXCEED_DEVICE_LIMIT_OUTCOME_ID);
    }

    @Test
    void shouldReturnAllOutcomesWhenPostponeDeviceStorageIsDisabledAndMaxSavedDevicesIsGreaterThanZero()
            throws Exception {
        // given
        WebAuthnRegistrationNode.OutcomeProvider outcomeProvider = new WebAuthnRegistrationNode.OutcomeProvider();
        var attributes = json(object(field("postponeDeviceProfileStorage", false), field("maxSavedDevices", 2)));

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id))
                .containsExactly(UNSUPPORTED_OUTCOME_ID,
                        SUCCESS_OUTCOME_ID,
                        FAILURE_OUTCOME_ID,
                        ERROR_OUTCOME_ID,
                        EXCEED_DEVICE_LIMIT_OUTCOME_ID);
    }


    @Test
    void shouldNotReturnExceedDeviceLimitWhenPostponeDeviceStorageIsDisabledAndMaxSavedDevicesIsZero()
            throws Exception {
        // given
        WebAuthnRegistrationNode.OutcomeProvider outcomeProvider = new WebAuthnRegistrationNode.OutcomeProvider();
        var attributes = json(object(field("postponeDeviceProfileStorage", false), field("maxSavedDevices", 0)));

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id))
                .containsExactly(UNSUPPORTED_OUTCOME_ID,
                        SUCCESS_OUTCOME_ID,
                        FAILURE_OUTCOME_ID,
                        ERROR_OUTCOME_ID);
    }

    @Test
    void shouldNotReturnExceedDeviceLimitWhenPostponeDeviceStorageIsEnabledAndMaxSavedDevicesIsGreaterThanZero()
            throws Exception {
        // given
        WebAuthnRegistrationNode.OutcomeProvider outcomeProvider = new WebAuthnRegistrationNode.OutcomeProvider();
        var attributes = json(object(field("postponeDeviceProfileStorage", true), field("maxSavedDevices", 2)));

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id))
                .containsExactly(UNSUPPORTED_OUTCOME_ID,
                        SUCCESS_OUTCOME_ID,
                        FAILURE_OUTCOME_ID,
                        ERROR_OUTCOME_ID);
    }

    @Test
    void shouldNotReturnExceedDeviceLimitOutcomeWhenGetOutcomesAndJsonIsNull() throws Exception {
        // given
        WebAuthnRegistrationNode.OutcomeProvider outcomeProvider = new WebAuthnRegistrationNode.OutcomeProvider();
        var attributes = json(null);

        // when
        var outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id))
                .containsExactly(UNSUPPORTED_OUTCOME_ID,
                        SUCCESS_OUTCOME_ID,
                        FAILURE_OUTCOME_ID,
                        ERROR_OUTCOME_ID);
    }
}
