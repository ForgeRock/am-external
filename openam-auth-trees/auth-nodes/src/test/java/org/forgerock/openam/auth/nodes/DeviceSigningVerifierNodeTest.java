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
 * Copyright 2023-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.assertj.core.data.Offset;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.SecretRSASigningHandler;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.DeviceSigningVerifierCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingSettings;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.SigningKey;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Test for Device Binding
 */
public class DeviceSigningVerifierNodeTest {

    public static final String ISS = "com.example.app";
    private ArgumentCaptor<DeviceBindingSettings> deviceArgumentCaptor;


    @Mock
    DeviceSigningVerifierNode.Config config;

    @Mock
    LocaleSelector localeSelector;

    @Mock
    DeviceBindingManager deviceBindingManager;

    @Mock
    CoreWrapper coreWrapper;

    @InjectMocks
    DeviceSigningVerifierNode node;

    @Mock
    AMIdentity amIdentity;

    @Mock
    LegacyIdentityService identityService;



    @BeforeMethod
    public void setup() throws IdRepoException, SSOException {
        node = null;
        openMocks(this);

        given(identityService.getAmIdentity(any(SSOToken.class), any(String.class), eq(IdType.USER), any()))
                .willReturn(amIdentity);

        given(identityService.getUniversalId(any(), (IdType) any(), any()))
                .willReturn(UUID.randomUUID().toString());

        given(identityService.getUniversalId(any(), any(), (IdType) any())).willReturn(Optional.of("bob"));

        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(amIdentity.getUniversalId()).willReturn("bob");
        given(amIdentity.getType()).willReturn(IdType.USER);

        given(config.challenge()).willReturn(true);
        given(config.title()).willReturn(Map.of(Locale.ENGLISH, "title"));
        given(config.subtitle()).willReturn(Map.of(Locale.ENGLISH, "subtitle"));
        given(config.description()).willReturn(Map.of(Locale.ENGLISH, "description"));
        given(config.timeout()).willReturn(60);
        given(config.applicationIds()).willReturn(Set.of(ISS));
        given(config.clientErrorOutcomes()).willReturn(Collections.singletonList("unsupported"));
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));
        deviceArgumentCaptor = ArgumentCaptor.forClass(DeviceBindingSettings.class);
        doNothing().when(deviceBindingManager).saveDeviceProfile(any(), any(), deviceArgumentCaptor.capture());

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        // Client
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();


        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title", "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair, kid, ISS, "bob", challenge, null));

        //When
        result = node.process(getContext(sharedState, transientState, singletonList(callback)));

        // Then
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    public void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        given(identityService.getUniversalId(any(), any(), (IdType) any())).willReturn(Optional.empty());
        given(coreWrapper.getIdentity(anyString())).willReturn(null);

        // When
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Failed to lookup user");
    }

    @Test
    public void testProcessWithNoInput()
            throws NodeProcessException, NoSuchAlgorithmException, DevicePersistenceException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(DeviceSigningVerifierCallback.class);
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getUserId()).isEqualTo("bob");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getTitle()).isEqualTo("title");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getSubtitle()).isEqualTo("subtitle");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getDescription()).isEqualTo("description");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getTimeout()).isEqualTo(60);
    }

    @Test
    public void testProcessWithNoInputUsernameLess() throws NodeProcessException {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(DeviceSigningVerifierCallback.class);
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getUserId()).isNull();
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getTitle()).isEqualTo("title");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getSubtitle()).isEqualTo("subtitle");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getDescription()).isEqualTo("description");
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getTimeout()).isEqualTo(60);
    }

    @Test
    public void testClientErrorOutcome() throws NodeProcessException {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        // Client
        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "userId", null, null, null, 60);
        callback.setClientError("my_custom_outcome");

        //When
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("my_custom_outcome");
        assertThat(result.callbacks).isEmpty();
    }

    private String buildSignedJwt(KeyPair keyPair, String kid, String iss, String sub, String challenge, Date exp)
            throws Exception {
        SigningKey signingKey = new SigningKey(new SecretBuilder()
                .secretKey(keyPair.getPrivate())
                .publicKey(keyPair.getPublic())
                .expiresAt(Instant.MAX)
                .stableId("some-id"));
        if (exp == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 30);
            exp = calendar.getTime();
        }

        return new SignedJwtBuilderImpl(new SecretRSASigningHandler(signingKey))
                .headers().alg(JwsAlgorithm.RS256)
                .kid(kid)
                .done()
                .claims(new JwtClaimsSetBuilder()
                        .sub(sub)
                        .iss(iss)
                        .claim("challenge", challenge)
                        .exp(exp)
                        .build())
                .build();
    }

    @Test
    public void testProcessWithCallbackSuccess() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));
        deviceArgumentCaptor = ArgumentCaptor.forClass(DeviceBindingSettings.class);
        doNothing().when(deviceBindingManager).saveDeviceProfile(any(), any(), deviceArgumentCaptor.capture());

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        // Client
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();


        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title", "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair, kid, ISS, "bob", challenge, null));


        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(deviceArgumentCaptor.getValue().getLastAccessDate()).isCloseTo(Time.currentTimeMillis(),
                Offset.offset(1000L));
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.SUCCESS_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithInvalidIssuer() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        // Client
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();


        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title", "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair, kid, "invalid", "bob", challenge, null));



        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.FAILURE_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithExpiredToken() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        // Client
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();


        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title", "subtitle", "description", 60);
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        callback.setJws(buildSignedJwt(keyPair, kid, ISS, "bob", challenge, Date.from(yesterday)));


        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.FAILURE_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();
    }


    @Test
    public void testProcessWithCallbackInvalidChallenge() throws Exception {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        // Client
        String challenge = "invalid";

        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title",
                "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair, kid, ISS, "bob", challenge, null));

        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.FAILURE_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithCallbackInvalidPublicKey() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = UUID.randomUUID().toString();

        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId(kid)
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString())).willReturn(
                asList(deviceBindingSettings));

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();

        KeyPair keyPair2 = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title",
                "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair2, kid, ISS, "bob", challenge, null));

        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.FAILURE_OUTCOME_ID);
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithCallbackKeyNotFound() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        JWK rsaJwk = RsaJWK.builder((RSAPublicKey) keyPair.getPublic())
                .keyId("Invalid")
                .algorithm(JwsAlgorithm.RS256)
                .build();

        DeviceBindingSettings deviceBindingSettings = new DeviceBindingSettings();
        deviceBindingSettings.setKey(rsaJwk);
        given(deviceBindingManager.getDeviceProfiles(anyString(), anyString()))
                .willReturn(asList(deviceBindingSettings));

        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge()).isNotNull();

        //given
        String challenge = ((DeviceSigningVerifierCallback) result.callbacks.get(0)).getChallenge();
        String kid = UUID.randomUUID().toString();
        DeviceSigningVerifierCallback callback = new DeviceSigningVerifierCallback("challenge",
                "bob", "title", "subtitle", "description", 60);
        callback.setJws(buildSignedJwt(keyPair, kid, ISS, "bob", challenge, null));

        //When
        result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo(DeviceSigningVerifierNode.KEY_NOT_FOUND);
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithNoDeviceRegistered() throws Exception {

        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());
        //Call the process to generate Challenge in the sharedState
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.callbacks).isEmpty();
    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "Failed to lookup user")
    public void testUserNotActive() throws NodeProcessException, IdRepoException, SSOException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());
        given(amIdentity.isActive()).willReturn(false);

        // When
        node.process(getContext(sharedState, transientState, emptyList()));
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
                                   List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(),
                callbacks, Optional.empty());
    }
}
