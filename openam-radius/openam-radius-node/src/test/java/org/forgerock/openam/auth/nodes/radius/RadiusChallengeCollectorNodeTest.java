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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.radius.common.StateAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.modules.radius.client.RadiusChallenge;

@ExtendWith(MockitoExtension.class)
public class RadiusChallengeCollectorNodeTest {

    private static final String CHALLENGE_KEY = "radiusChallenge";
    private RadiusChallengeCollectorNode node;

    @BeforeEach
    public void setup() {
        node = new RadiusChallengeCollectorNode();
    }

    private JsonValue createChallenge() {
        byte[] testOctets = {1, 2, 3, 4};
        StateAttribute stateAttribute = new StateAttribute(testOctets);
        return new RadiusChallenge(stateAttribute, "Enter OTP", null).toJsonValue();
    }

    private TreeContext createContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks, JsonValue pageNodeState) {
        return TreeContext.builder()
                .identityResource("managed/user")
                .sharedState(sharedState)
                .transientState(transientState)
                .secureState(pageNodeState)
                .callbacks(callbacks)
                .build();
    }

    @Test
    public void shouldSendPasswordCallbackGivenNoResponseProvided() throws NodeProcessException {
        // Given
        JsonValue nodeState = json(object(field(CHALLENGE_KEY, createChallenge())));
        TreeContext context = createContext(json(object()), json(object()), emptyList(), nodeState);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        PasswordCallback pc = (PasswordCallback) result.callbacks.get(0);
        assertThat(pc.getPrompt()).isEqualTo("Enter OTP");
    }

    @Test
    public void shouldStoreResponseInTransientStateGivenValidPasswordCallback() throws NodeProcessException {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("response".toCharArray());
        JsonValue nodeState = json(object(field(CHALLENGE_KEY, createChallenge())));
        TreeContext context = createContext(json(object()), json(object()), singletonList(callback), nodeState);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        JsonValue capturedValue = context.getStateFor(node).get(CHALLENGE_KEY);
        assertThat(capturedValue).isNotNull();
        RadiusChallenge deserializedChallenge = new RadiusChallenge(capturedValue);
        assertThat(deserializedChallenge.getResponse()).isEqualTo("response");
        assertThat(deserializedChallenge.getReplyMessage()).isEqualTo("Enter OTP");
    }

    @Test
    public void shouldSendPasswordCallbackGivenCallbackWithEmptyPassword() throws NodeProcessException {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("".toCharArray());
        JsonValue nodeState = json(object(field(CHALLENGE_KEY, createChallenge())));
        TreeContext context = createContext(json(object()), json(object()), singletonList(callback), nodeState);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        PasswordCallback pc = (PasswordCallback) result.callbacks.get(0);
        assertThat(pc.getPrompt()).isEqualTo("Enter OTP");
    }

    @Test
    public void shouldFailToProcessGivenMissingChallengeInNodeState() {
        // Given
        TreeContext context = createContext(json(object()), json(object()), emptyList(), json(object()));

        // When & Then
        assertThatExceptionOfType(NodeProcessException.class)
                .isThrownBy(() -> node.process(context))
                .withMessage("RADIUS challenge not found in state");
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullPasswordInCallback() {
        // Given
        PasswordCallback mockCallback = mock(PasswordCallback.class);
        given(mockCallback.getPassword()).willReturn(null);
        JsonValue nodeState = json(object(field(CHALLENGE_KEY, createChallenge())));
        TreeContext context = createContext(json(object()), json(object()), singletonList(mockCallback), nodeState);

        // When & Then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> node.process(context));
    }
}
