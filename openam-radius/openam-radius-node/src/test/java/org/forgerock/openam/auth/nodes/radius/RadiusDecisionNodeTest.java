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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.packet.VendorSpecificAttribute;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.modules.radius.client.ChallengeException;
import com.sun.identity.authentication.modules.radius.client.RadiusChallenge;
import com.sun.identity.authentication.modules.radius.client.RejectException;
import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import com.sun.identity.idm.AMIdentity;


@ExtendWith(MockitoExtension.class)
public class RadiusDecisionNodeTest {

    private static final String CHALLENGE_KEY = "radiusChallenge";
    private static final String RADIUS_ATTRIBUTES = "radiusAttributes";
    private static final byte[] TEST_OCTETS = {1, 2, 3, 4};
    private static final Purpose<GenericSecret> SHARED_SECRET_PURPOSE = Purpose.purpose("sharedSecretPurpose",
            GenericSecret.class);

    @Mock
    private RadiusDecisionNode.Config config;
    @Mock
    private RadiusNodeConn radiusNodeConn;
    @Mock
    private PreferredLocales preferredLocales;
    @Mock
    private ResourceBundle bundle;
    @Mock
    private Realm realm;
    @Mock
    private AMAccountLockoutTrees.Factory amAccountLockoutFactory;
    @Mock
    private NodeUserIdentityProvider identityProvider;
    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private SecretCache realmCache;
    @Mock
    private AMAccountLockoutTrees amAccountLockoutTrees;
    @Mock
    private RadiusConnProvider radiusConnProvider;

    @InjectMocks
    private RadiusDecisionNode node;

    private JsonValue sharedState;

    private void mockSecrets() throws IOException {
        given(config.sharedSecretPurpose()).willReturn(Optional.of(SHARED_SECRET_PURPOSE));
        given(radiusConnProvider.createConnection(anySet(), anySet(), anyString(), anyInt(), anyBoolean(), any()))
                .willReturn(radiusNodeConn);
        given(secretReferenceCache.realm(realm)).willReturn(realmCache);
        given(realmCache.active(SHARED_SECRET_PURPOSE))
                .willReturn(SecretReference.constant(GenericSecret.password("new-password".toCharArray())));
    }

    private void setupAccountLockout() {
        given(config.checkForLockout()).willReturn(true);
        given(amAccountLockoutTrees.isLockoutEnabled()).willReturn(true);
        given(amAccountLockoutFactory.create(realm)).willReturn(amAccountLockoutTrees);
    }

    private TreeContext createContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks, JsonValue pageNodeState) {
        return TreeContext.builder()
                .identityResource("managed/user")
                .sharedState(sharedState)
                .transientState(transientState)
                .secureState(pageNodeState)
                .requestContext(new ExternalRequestContext.Builder().locales(preferredLocales).build())
                .callbacks(callbacks)
                .build();
    }

    @Test
    public void shouldReturnAcceptOutcome() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("acceptOutcome");
        verify(radiusNodeConn).disconnect();
    }

    @Test
    public void shouldReturnRejectOutcome() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();
        willThrow(new RejectException(new AccessReject())).given(radiusNodeConn).authenticate(anyString(), anyString());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("rejectOutcome");
    }

    @Test
    public void shouldReturnChallengeOutcome() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();
        StateAttribute mockStateAttribute = mock(StateAttribute.class);
        given(mockStateAttribute.getOctets()).willReturn(TEST_OCTETS);

        ChallengeException challenge = mock(ChallengeException.class);
        given(challenge.getStateAttribute()).willReturn(mockStateAttribute);
        given(challenge.getReplyMessage()).willReturn("message");
        willThrow(challenge).given(radiusNodeConn).authenticate(anyString(), anyString());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("challengeOutcome");
        JsonValue challengeInState = context.getStateFor(node).get(CHALLENGE_KEY);
        Assertions.assertNotNull(challengeInState);
        RadiusChallenge deserializedChallenge = new RadiusChallenge(challengeInState);
        assertThat(deserializedChallenge.getReplyMessage()).isEqualTo("message");
        assertThat(deserializedChallenge.getStateAttribute().getOctets()).isEqualTo(TEST_OCTETS);
    }

    @Test
    public void shouldReturnErrorOutcomeGivenIOException() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();
        willThrow(new IOException()).given(radiusNodeConn).authenticate(anyString(), anyString());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenNoSuchAlgorithmException() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();
        willThrow(new NoSuchAlgorithmException()).given(radiusNodeConn).authenticate(anyString(), anyString());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenMissingSharedSecretPurpose() throws Exception {
        // Given
        given(config.sharedSecretPurpose()).willReturn(Optional.empty());
        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
        assertThat(result.errorMessage).isEqualTo("Secret key could not be retrieved");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenMissingSecretLabel() throws Exception {
        // Given
        given(config.sharedSecretPurpose()).willReturn(Optional.of(SHARED_SECRET_PURPOSE));
        given(secretReferenceCache.realm(realm)).willReturn(realmCache);
        given(realmCache.active(SHARED_SECRET_PURPOSE))
                .willThrow(new IllegalStateException("Missing secret label"));
        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
        assertThat(result.errorMessage).isEqualTo("Secret key could not be retrieved");
    }

    @Test
    public void shouldReturnAcceptOutcomeGivenChallengeResponse() throws Exception {
        // Given
        mockSecrets();
        StateAttribute stateAttribute = new StateAttribute(TEST_OCTETS);
        RadiusChallenge challengeWithResponse = new RadiusChallenge(stateAttribute, "message", "response");
        JsonValue nodeState = json(object(
                field(USERNAME, "user"),
                field(PASSWORD, "password"),
                field(CHALLENGE_KEY, challengeWithResponse.toJsonValue())));
        TreeContext context = createContext(json(object()), json(object()), emptyList(), nodeState);

        // When
        Action result = node.process(context);

        // Then
        verify(radiusNodeConn).replyChallenge(anyString(), anyString(), any(RadiusChallenge.class));
        assertThat(result.outcome).isEqualTo("acceptOutcome");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenMissingUsernameOrPassword() throws NodeProcessException {
        // Given - Create a dummy ResourceBundle implementation
        // Just mocking ResourceBundle caused NPE in ResourceBundle.getString method
        ResourceBundle dummyBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                if ("missingUsernameOrPassword".equals(key)) {
                    return "Username or password is missing from the authentication state.";
                }
                return null;
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(Collections.singletonList("missingUsernameOrPassword"));
            }
        };

        given(preferredLocales.getBundleInPreferredLocale(any(), any()))
                .willReturn(dummyBundle);

        TreeContext context = createContext(json(object()), json(object()), emptyList(), json(object()));

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
        assertThat(result.errorMessage).isEqualTo("Username or password is missing from the authentication state.");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenUserIdentityNotFound() throws Exception {
        // Given
        setupAccountLockout();
        setupMockIdentityProvider(null);

        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
    }

    @Test
    public void shouldReturnErrorOutcomeGivenAccountLockedAndLockoutEnabled() throws Exception {
        // Given
        setupAccountLockout();
        AMIdentity userIdentity = mock(AMIdentity.class);
        setupMockIdentityProvider(userIdentity);
        given(amAccountLockoutTrees.isAccountLocked(userIdentity)).willReturn(true);
        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("errorOutcome");
    }

    @Test
    public void shouldReturnAcceptOutcomeGivenAccountNotLockedAndLockoutEnabled() throws Exception {
        // Given
        mockSecrets();
        setupAccountLockout();
        AMIdentity userIdentity = mock(AMIdentity.class);
        setupMockIdentityProvider(userIdentity);
        given(amAccountLockoutTrees.isAccountLocked(userIdentity)).willReturn(false);
        TreeContext context = setupTreeContext();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("acceptOutcome");
    }

    @Test
    public void shouldAddVSAToNodeStateGivenPresent() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();

        AttributeSet radiusAttributes = new AttributeSet();
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(14122, "https://www.example.com/logout");
        radiusAttributes.addAttribute(vsa);
        given(radiusNodeConn.authenticate(anyString(), anyString())).willReturn(radiusAttributes);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("acceptOutcome");
        JsonValue radiusAttrsInState = sharedState.get(RADIUS_ATTRIBUTES);
        assertThat(radiusAttrsInState.isNotNull()).isTrue();
        assertThat(radiusAttrsInState.asList()).hasSize(1);
        assertThat(radiusAttributes.getAttributeAt(0)).isEqualTo(vsa);
    }

    @Test
    public void shouldNotAddVSAToNodeStateGivenNotPresent() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();

        AttributeSet radiusAttributes = new AttributeSet();
        // Add a non-vendor-specific attribute
        radiusAttributes.addAttribute(new StateAttribute(TEST_OCTETS));
        given(radiusNodeConn.authenticate(anyString(), anyString())).willReturn(radiusAttributes);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("acceptOutcome");
        assertThat(sharedState.get(RADIUS_ATTRIBUTES).isNull()).isTrue();
    }

    @Test
    public void shouldAddMultipleVSAToNodeStateGivenMultiplePresent() throws Exception {
        // Given
        mockSecrets();
        TreeContext context = setupTreeContext();

        AttributeSet radiusAttributes = new AttributeSet();
        VendorSpecificAttribute vsa1 = new VendorSpecificAttribute(14122, "http://www.example.com/logout");
        VendorSpecificAttribute vsa2 = new VendorSpecificAttribute(9, "test");
        radiusAttributes.addAttribute(vsa1);
        radiusAttributes.addAttribute(vsa2);
        // Add a non-VSA attribute
        radiusAttributes.addAttribute(new StateAttribute(TEST_OCTETS));
        given(radiusNodeConn.authenticate(anyString(), anyString())).willReturn(radiusAttributes);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("acceptOutcome");
        JsonValue radiusAttrsInState = sharedState.get(RADIUS_ATTRIBUTES);
        assertThat(radiusAttrsInState.isNotNull()).isTrue();
        assertThat(radiusAttrsInState.asList()).hasSize(2);
        assertThat(radiusAttributes.getAttributeAt(0)).isEqualTo(vsa1);
        assertThat(radiusAttributes.getAttributeAt(1)).isEqualTo(vsa2);
    }

    private TreeContext setupTreeContext() {
        JsonValue nodeState = json(object(field(USERNAME, "user"), field(PASSWORD, "password")));
        sharedState = json(object());
        return createContext(sharedState, json(object()), emptyList(), nodeState);
    }

    private void setupMockIdentityProvider(AMIdentity amIdentity) {
        given(identityProvider.getUniversalId("user", realm)).willReturn(Optional.of("uid"));
        given(identityProvider.getAMIdentity(eq(Optional.of("uid")), any()))
                .willReturn(Optional.ofNullable(amIdentity));
    }
}
