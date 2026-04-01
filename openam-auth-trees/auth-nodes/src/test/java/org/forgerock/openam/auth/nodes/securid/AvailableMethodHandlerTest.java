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
package org.forgerock.openam.auth.nodes.securid;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.APPROVE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.TOKEN;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AVAILABLE_METHODS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CHOSEN_METHOD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.AuthenticationMethodBuilder.authenticationMethod;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailableMethodHandlerTest {
    private static final String WAITING_MESSAGE = "waiting message";
    private static final String MFA_CHOICE_PROMPT = "mfa choice prompt";

    private NodeState nodeState;

    @Mock
    private SecurIdVerifyService verifyService;

    @InjectMocks
    private AvailableMethodHandler methodHandler;

    @BeforeEach
    void setUp() {
        nodeState = createEmptyNodeState();
    }

    @Test
    void goesToNotEnrolledIfNoAvailableMfaMethods() throws Exception {
        var response = securIdResponse().build();

        Action action = methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

        assertThat(action.outcome).isEqualTo("NOTENROLLED");
    }

    @Test
    void sendsChallengeCallbacksIfOnlyOneMfaMethodAvailable() throws Exception {
        var method = TOKEN;
        var response = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId(method.name()).build())
                .build();
        List<Callback> challengeCallbacks = List.of(new PasswordCallback("prompt", false));
        given(verifyService.beginVerification(method, nodeState, WAITING_MESSAGE))
                .willReturn(challengeCallbacks);

        Action action = methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks).containsAll(challengeCallbacks);
    }

    @Test
    void setsAppropriateStateParametersWhenOnlyOneMfaMethodAvailable() throws Exception {
        var method = TOKEN;
        var response = securIdResponse()
                .messageId("messageId")
                .authnAttemptId("authnAttemptId")
                .withAuthenticationMethod(authenticationMethod().methodId(method.name()).build())
                .build();
        List<Callback> challengeCallbacks = List.of(new PasswordCallback("prompt", false));
        given(verifyService.beginVerification(method, nodeState, WAITING_MESSAGE))
                .willReturn(challengeCallbacks);

        methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

        assertThat(requireNonNull(nodeState.get(CHOSEN_METHOD)).asString()).isEqualTo(method.name());
        assertThat(requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString()).isEqualTo("messageId");
        assertThat(requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString()).isEqualTo("authnAttemptId");
    }

    @Test
    void sendsChoiceCallbacksIfMultipleMfaMethodsAvailable() throws Exception {
        var method1 = TOKEN;
        var method2 = APPROVE;
        var response = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId(method1.name()).build())
                .withAuthenticationMethod(authenticationMethod().methodId(method2.name()).build())
                .build();

        Action action = methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks).hasSize(2);
        assertThat(action.callbacks.get(0)).isInstanceOf(ChoiceCallback.class);
        assertThat(action.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        var choiceCallback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(choiceCallback.getChoices()).containsExactly(method1.displayName(), method2.displayName());
    }

    @Test
    void setsAppropriateStateParametersWhenMultipleMfaMethodsAvailable() throws Exception {
        var method1 = TOKEN;
        var method2 = APPROVE;
        var response = securIdResponse()
                .messageId("messageId")
                .authnAttemptId("authnAttemptId")
                .withAuthenticationMethod(authenticationMethod().methodId(method1.name()).build())
                .withAuthenticationMethod(authenticationMethod().methodId(method2.name()).build())
                .build();

        methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

        assertThat(requireNonNull(nodeState.get(AVAILABLE_METHODS)).asList())
                .containsExactly(method1.name(), method2.name());
        assertThat(requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString()).isEqualTo("messageId");
        assertThat(requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString()).isEqualTo("authnAttemptId");
        assertThat(requireNonNull(nodeState.get(CONFIRMATION_CB)).asList(String.class))
                .containsExactly("Next", "Cancel");
    }

    private static NodeState createEmptyNodeState() {
        var node = mock(Node.class);
        given(node.getInputs()).willReturn(new InputState[] {});
        return TreeContext.builder().build().getStateFor(node);
    }
}
