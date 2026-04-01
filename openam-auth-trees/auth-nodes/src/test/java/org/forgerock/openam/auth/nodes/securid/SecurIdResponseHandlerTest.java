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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.APPROVE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SECURID_NEW_PIN;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.AuthenticationMethodBuilder.authenticationMethod;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.CredentialValidationResultBuilder.credentialValidationResult;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Locale;
import java.util.Map;

import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AttemptResponseCode;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.MethodResponseCode;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurIdResponseHandlerTest {

    private static final String WAITING_MESSAGE = "waiting message";
    private static final String MFA_CHOICE_PROMPT = "mfa choice prompt";

    private NodeState nodeState;

    @Mock
    private AvailableMethodHandler methodHandler;
    @Mock
    private SecurIdNode.Config config;

    @InjectMocks
    private SecurIdResponseHandler responseHandler;

    @BeforeEach
    void setUp() {
        nodeState = createEmptyNodeState();
        lenient().when(config.waitingMessage()).thenReturn(Map.of(Locale.ENGLISH, WAITING_MESSAGE));
        lenient().when(config.mfaChoicePrompt()).thenReturn(Map.of(Locale.ENGLISH, MFA_CHOICE_PROMPT));
    }

    private static NodeState createEmptyNodeState() {
        var node = mock(Node.class);
        given(node.getInputs()).willReturn(new InputState[]{});
        return TreeContext.builder().build().getStateFor(node);
    }

    @Nested
    class Initialize {
        @Test
        void goesToSuccessIfResponseIsSuccess() throws Exception {
            var response = securIdResponse().attemptResponseCode(AttemptResponseCode.SUCCESS).build();

            Action result = responseHandler.handleInitialize(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result.outcome).isEqualTo("SUCCESS");
        }

        @Test
        void handlesAvailableMethodsIfChallengeResponseIsReturned() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.SUCCESS).build())
                    .withAuthenticationMethod(authenticationMethod()
                            .methodId(APPROVE.name()).referenceId("referenceId").build())
                    .build();
            Action action = mock(Action.class);
            given(methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT)).willReturn(action);

            Action result = responseHandler.handleInitialize(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result).isEqualTo(action);
        }
    }

    @Nested
    class Verify {
        @Test
        void goesToSuccessIfResponseIsSuccess() throws Exception {
            var response = securIdResponse().attemptResponseCode(AttemptResponseCode.SUCCESS).build();

            Action result = responseHandler
                    .handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result.outcome).isEqualTo("SUCCESS");
        }

        @Test
        void throwsExceptionIfNoMethodResponseCodeIsAvailable() {
            var response = securIdResponse().attemptResponseCode(AttemptResponseCode.CHALLENGE).build();

            assertThatThrownBy(() -> responseHandler
                    .handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT))
                    .isInstanceOf(NodeProcessException.class);
        }

        @Test
        void handlesAvailableMethodsWhenMethodChallengeResponseIsReturned() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.SUCCESS).build())
                    .withAuthenticationMethod(authenticationMethod()
                            .methodId(APPROVE.name()).referenceId("referenceId").build())
                    .build();
            Action action = mock(Action.class);
            given(methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT)).willReturn(action);

            Action result = responseHandler
                    .handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result).isEqualTo(action);
        }

        @Test
        void addsPushReferenceIdToStateWhenMethodChallengeResponseIsReturned() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.SUCCESS).build())
                    .withAuthenticationMethod(authenticationMethod()
                            .methodId(APPROVE.name()).referenceId("referenceId").build())
                    .build();
            Action action = mock(Action.class);
            given(methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT)).willReturn(action);

            responseHandler.handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(requireNonNull(nodeState.get(PUSH_REFERENCE_ID)).asString()).isEqualTo("referenceId");
        }

        @Test
        void returnsFailureIfFailResponseReceivedFromSecurId() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.FAIL).build())
                    .build();

            Action result = responseHandler
                    .handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result.outcome).isEqualTo("FAILURE");
        }

        @Test
        void handlesAvailableMethodsWhenFailResponseReceivedFromSecurIdNewPinAttempt() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.FAIL).build())
                    .build();
            Action action = mock(Action.class);
            given(methodHandler.handle(response, nodeState, WAITING_MESSAGE, MFA_CHOICE_PROMPT)).willReturn(action);

            Action result = responseHandler
                    .handleVerify(response, nodeState, SECURID_NEW_PIN, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result).isEqualTo(action);
        }

        @Test
        void returnsPushAuthenticationCallbacksIfInProcessResponseReceivedFromSecurId() throws Exception {
            var response = securIdResponse()
                    .attemptResponseCode(AttemptResponseCode.CHALLENGE)
                    .withCredentialValidationResult(credentialValidationResult()
                            .methodResponseCode(MethodResponseCode.IN_PROCESS).build())
                    .withAuthenticationMethod(authenticationMethod()
                            .methodId(APPROVE.name()).referenceId("referenceId").build())
                    .build();

            Action result = responseHandler
                    .handleVerify(response, nodeState, APPROVE, WAITING_MESSAGE, MFA_CHOICE_PROMPT);

            assertThat(result.callbacks).hasSize(2);
            assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
            assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        }
    }
}
