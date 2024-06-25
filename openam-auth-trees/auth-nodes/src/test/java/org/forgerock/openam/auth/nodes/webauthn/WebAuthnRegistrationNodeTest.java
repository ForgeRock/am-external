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
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.WEB_AUTHN_DEVICE_DATA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
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
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationResponse;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Test for WebAuthnRegistrationNode
 */
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
    IdentityUtils identityUtils;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    UserWebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    @Mock
    UserDeviceSettingsDao userDeviceSettingsDao;

    @Mock
    JWK jwk;

    WebAuthnRegistrationNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        initMocks(this);
        given(identityUtils.getUniversalId(anyString(), anyString(), any()))
                .willReturn(Optional.of(UUID.randomUUID().toString()));
        given(realm.asPath()).willReturn("/");
        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("usernameFromAmIdentity");

        given(config.asScript()).willReturn(false);
        given(config.attestationPreference()).willReturn(AttestationPreference.NONE);
        given(config.userVerificationRequirement()).willReturn(UserVerificationRequirement.PREFERRED);
        given(config.authenticatorAttachment()).willReturn(AuthenticatorAttachment.PLATFORM);
        given(config.requiresResidentKey()).willReturn(false);
        given(config.timeout()).willReturn(100);
        given(config.relyingPartyName()).willReturn("relyingPartyName");
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));
        given(config.acceptedSigningAlgorithms()).willReturn(Collections.singleton(CoseAlgorithm.ES256));
        given(config.excludeCredentials()).willReturn(true);
        given(config.postponeDeviceProfileStorage()).willReturn(true);
        given(config.maxSavedDevices()).willReturn(2);

        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any()))
                .willReturn(Collections.singletonList(generateDevice()));

        node = new WebAuthnRegistrationNode(config, realm, registerFlow, clientScriptUtilities,
                webAuthnDeviceProfileManager, secureRandom, recoveryCodeGenerator,
                webAuthnDeviceJsonUtils, identityUtils, coreWrapper);
    }

    @Test
    public void testCallback() throws NodeProcessException {
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
                .isEqualTo("{\"userVerification\":\"preferred\",\"authenticatorAttachment\":\"platform\"}");
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
    public void testCallbackUsernameAttribute() throws IdRepoException, SSOException,
            NodeProcessException {
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
    public void testCallbackDisplayNameSharedState() throws NodeProcessException {
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
    public void testCallbackUsernameAttributeAndDisplayNameSharedState() throws IdRepoException, SSOException,
            NodeProcessException {
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
    public void testCallbackWithDeviceLimit() throws NodeProcessException, DevicePersistenceException {
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
    public void testDeviceName() throws NodeProcessException, WebAuthnRegistrationException, IOException {
        //given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("id");
        hiddenValueCallback.setValue("dummy::1,2,3,4::dummy::customDeviceName");
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));

        final RsaJWK jwk = RsaJWK.parse("{"
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

        AttestationObject attestationObject = new AttestationObject(null, new AuthData(null,
                null, 0,
                new AttestedCredentialData("dummy".getBytes(), 0, "credentialId".getBytes(),
                        jwk, null), null), null);

        given(registerFlow.accept(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(
                new AttestationResponse(attestationObject, null));

        given(webAuthnDeviceProfileManager.createDeviceProfile(any(), any(), any(), any())).willCallRealMethod();
        given(webAuthnDeviceJsonUtils.toJsonValue(any())).willCallRealMethod();

        //when
        Action result = node.process(treeContext);

        //then
        JsonValue value = result.transientState.get(WEB_AUTHN_DEVICE_DATA);
        assertThat(value.get("deviceName").asString()).isEqualTo("customDeviceName");

    }

    @Test
    public void testWithoutDeviceName() throws NodeProcessException, WebAuthnRegistrationException, IOException {
        //given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object());

        TreeContext treeContext = getContext(sharedState, transientState, emptyList());
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("id");
        hiddenValueCallback.setValue("dummy::1,2,3,4::dummy");
        treeContext = treeContext.copyWithCallbacks(Collections.singletonList(hiddenValueCallback));

        final RsaJWK jwk = RsaJWK.parse("{"
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

        AttestationObject attestationObject = new AttestationObject(null, new AuthData(null,
                null, 0,
                new AttestedCredentialData("dummy".getBytes(), 0, "credentialId".getBytes(),
                        jwk, null), null), null);

        given(registerFlow.accept(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(
                new AttestationResponse(attestationObject, null));

        given(webAuthnDeviceProfileManager.createDeviceProfile(any(), any(), any(), any())).willCallRealMethod();
        given(webAuthnDeviceJsonUtils.toJsonValue(any())).willCallRealMethod();

        //when
        Action result = node.process(treeContext);
        JsonValue value = result.transientState.get(WEB_AUTHN_DEVICE_DATA);

        //then
        assertThat(value.get("deviceName").asString()).isEqualTo("New Security Key");

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