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

package org.forgerock.openam.auth.nodes.webauthn;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.util.encode.Base64url;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.idm.IdRepoException;

/**
 * Test for WebAuthnAuthenticationNode
 */
public class WebAuthnAuthenticationNodeTest {

    @Mock
    WebAuthnAuthenticationNode.Config config;

    @Mock
    AuthenticationFlow authenticationFlow;

    ClientScriptUtilities clientScriptUtilities = new ClientScriptUtilities();

    SecureRandom secureRandom = new SecureRandom();

    @Mock
    UserWebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    @Mock
    UserDeviceSettingsDao<WebAuthnDeviceSettings> userDeviceSettingsDao;

    @Mock
    JWK jwk;

    WebAuthnAuthenticationNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        initMocks(this);

        given(config.asScript()).willReturn(false);
        given(config.isRecoveryCodeAllowed()).willReturn(false);
        given(config.userVerificationRequirement()).willReturn(UserVerificationRequirement.PREFERRED);
        given(config.timeout()).willReturn(100);
        given(config.relyingPartyDomain()).willReturn(Optional.of("example.com"));

        given(webAuthnDeviceProfileManager.getDeviceProfiles(any(), any())).willReturn(
                Collections.singletonList(generateDevice()));

        node = new WebAuthnAuthenticationNode(config, authenticationFlow, clientScriptUtilities,
                webAuthnDeviceProfileManager, secureRandom);

        node = Mockito.spy(node);
        doReturn(Collections.singletonList(generateDevice())).when(node).getDeviceSettingsFromUsername(any(), any());
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
        //Retain the existing attribute
        assertThat(callback.getOutputValue().get("challenge").asString()).isNotEmpty();
        assertThat(callback.getOutputValue().get("acceptableCredentials").asString())
                .isEqualTo("{ \"type\": \"public-key\", "
                        + "\"id\": new Int8Array([116, 101, 115, 116]).buffer }");
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

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
                                   List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

    private WebAuthnDeviceSettings generateDevice() {
        UserWebAuthnDeviceProfileManager manager = new UserWebAuthnDeviceProfileManager(userDeviceSettingsDao);
        return manager.createDeviceProfile(
                Base64url.encode("test".getBytes()), jwk, null, null);
    }

}