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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.QRCODE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PASSWORD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PUSH;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.VOICE_OR_SMS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.AuthenticationMethodBuilder.authenticationMethod;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.CredentialValidationResultBuilder.credentialValidationResult;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

@ExtendWith(MockitoExtension.class)
class SecurIdVerifyServiceTest {
    private static final String PREVIOUS_MESSAGE_ID = "previous-message-id";
    private static final String NEW_MESSAGE_ID = "new-message-id";
    private static final String PREVIOUS_ATTEMPT_ID = "previous-attempt-id";
    private static final String NEW_ATTEMPT_ID = "new-attempt-id";
    private static final String PREVIOUS_REFERENCE_ID = "previous-reference-id";
    private static final String NEW_REFERENCE_ID = "new-reference-id";
    private static final String QR_CODE_URL = "qr.code.url";

    @Mock
    private SecurIdRestClient restClient;
    @Mock
    private NodeState nodeState;

    @InjectMocks
    private SecurIdVerifyService verifyService;

    @ParameterizedTest
    @MethodSource("passwordMethods")
    void getsCallbacksForPasswordMethods(SecurIdMethod method) throws Exception {
        List<Callback> callbacks = verifyService.beginVerification(method, nodeState, null);

        assertThat(callbacks).hasSize(2);
        assertThat(callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        var passwordCallback = (PasswordCallback) callbacks.get(0);
        assertThat(passwordCallback.getPrompt()).isEqualTo(method.prompt());
    }

    @ParameterizedTest
    @MethodSource("passwordMethods")
    void setsStateParametersForPasswordMethods(SecurIdMethod method) throws Exception {
        verifyService.beginVerification(method, nodeState, null);

        verify(nodeState).putShared(CONFIRMATION_CB, new String[] { "Next", "Cancel" });
    }

    private static Stream<Arguments> passwordMethods() {
        return Stream.of(SecurIdMethod.values())
                .filter(method -> method.type().equals(PASSWORD))
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("voiceOrSmsMethods")
    void getsCallbacksForVoiceOrSmsMethods(SecurIdMethod method) throws Exception {
        given(nodeState.get(RESPONSE_MESSAGE_ID)).willReturn(json(PREVIOUS_MESSAGE_ID));
        given(nodeState.get(AUTHN_ATTEMPT_ID)).willReturn(json(PREVIOUS_ATTEMPT_ID));
        given(restClient.verify(PREVIOUS_MESSAGE_ID, PREVIOUS_ATTEMPT_ID,
                List.of(new SubjectCredentials(method.name() + "_REQUEST_CODE", null, null))))
                .willReturn(securIdResponse().messageId(NEW_MESSAGE_ID).authnAttemptId(NEW_ATTEMPT_ID).build());

        List<Callback> callbacks = verifyService.beginVerification(method, nodeState, null);

        assertThat(callbacks).hasSize(2);
        assertThat(callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        var passwordCallback = (PasswordCallback) callbacks.get(0);
        assertThat(passwordCallback.getPrompt()).isEqualTo(method.prompt());
        verify(nodeState).putShared(CONFIRMATION_CB, new String[] { "Next", "Cancel" });
        verify(nodeState).putShared(RESPONSE_MESSAGE_ID, NEW_MESSAGE_ID);
        verify(nodeState).putShared(AUTHN_ATTEMPT_ID, NEW_ATTEMPT_ID);
    }

    @ParameterizedTest
    @MethodSource("voiceOrSmsMethods")
    void setsStateParametersForVoiceOrSmsMethods(SecurIdMethod method) throws Exception {
        given(nodeState.get(RESPONSE_MESSAGE_ID)).willReturn(json(PREVIOUS_MESSAGE_ID));
        given(nodeState.get(AUTHN_ATTEMPT_ID)).willReturn(json(PREVIOUS_ATTEMPT_ID));
        given(restClient.verify(PREVIOUS_MESSAGE_ID, PREVIOUS_ATTEMPT_ID,
                List.of(new SubjectCredentials(method.name() + "_REQUEST_CODE", null, null))))
                .willReturn(securIdResponse().messageId(NEW_MESSAGE_ID).authnAttemptId(NEW_ATTEMPT_ID).build());

        verifyService.beginVerification(method, nodeState, null);

        verify(nodeState).putShared(CONFIRMATION_CB, new String[] { "Next", "Cancel" });
        verify(nodeState).putShared(RESPONSE_MESSAGE_ID, NEW_MESSAGE_ID);
        verify(nodeState).putShared(AUTHN_ATTEMPT_ID, NEW_ATTEMPT_ID);
    }

    private static Stream<Arguments> voiceOrSmsMethods() {
        return Stream.of(SecurIdMethod.values())
                .filter(method -> method.type().equals(VOICE_OR_SMS))
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("pushMethods")
    void getsCallbacksForPushMethods(SecurIdMethod method) throws Exception {
        given(nodeState.get(RESPONSE_MESSAGE_ID)).willReturn(json(PREVIOUS_MESSAGE_ID));
        given(nodeState.get(AUTHN_ATTEMPT_ID)).willReturn(json(PREVIOUS_ATTEMPT_ID));
        given(nodeState.get(PUSH_REFERENCE_ID)).willReturn(json(PREVIOUS_REFERENCE_ID));
        given(restClient.verify(PREVIOUS_MESSAGE_ID, PREVIOUS_ATTEMPT_ID,
                List.of(new SubjectCredentials(method.name(), PREVIOUS_REFERENCE_ID, null))))
                .willReturn(securIdResponse()
                        .messageId(NEW_MESSAGE_ID)
                        .authnAttemptId(NEW_ATTEMPT_ID)
                        .withCredentialValidationResult(credentialValidationResult()
                                .withQrCodeUrl(QR_CODE_URL).build())
                        .withAuthenticationMethod(authenticationMethod()
                                .methodId(method.name()).referenceId(NEW_REFERENCE_ID).build())
                        .build());

        List<Callback> callbacks = verifyService.beginVerification(method, nodeState, null);

        if (method.equals(QRCODE)) {
            verifyQrCodeCallbacks(callbacks);
        } else {
            verifyPushCallbacks(callbacks);
        }
    }

    private static void verifyPushCallbacks(List<Callback> callbacks) {
        assertThat(callbacks).hasSize(2);
        assertThat(callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    private static void verifyQrCodeCallbacks(List<Callback> callbacks) {
        assertThat(callbacks).hasSize(3);
        assertThat(callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(callbacks.get(1)).isInstanceOf(PollingWaitCallback.class);
        assertThat(callbacks.get(2)).isInstanceOf(ConfirmationCallback.class);
        var scriptCallback = (ScriptTextOutputCallback) callbacks.get(0);
        assertThat(scriptCallback.getMessage()).contains(QR_CODE_URL);
    }

    @ParameterizedTest
    @MethodSource("pushMethods")
    void setsStateParametersForPushMethods(SecurIdMethod method) throws Exception {
        given(nodeState.get(RESPONSE_MESSAGE_ID)).willReturn(json(PREVIOUS_MESSAGE_ID));
        given(nodeState.get(AUTHN_ATTEMPT_ID)).willReturn(json(PREVIOUS_ATTEMPT_ID));
        given(nodeState.get(PUSH_REFERENCE_ID)).willReturn(json(PREVIOUS_REFERENCE_ID));
        given(restClient.verify(PREVIOUS_MESSAGE_ID, PREVIOUS_ATTEMPT_ID,
                List.of(new SubjectCredentials(method.name(), PREVIOUS_REFERENCE_ID, null))))
                .willReturn(securIdResponse()
                        .messageId(NEW_MESSAGE_ID)
                        .authnAttemptId(NEW_ATTEMPT_ID)
                        .withCredentialValidationResult(credentialValidationResult()
                                .withQrCodeUrl(QR_CODE_URL).build())
                        .withAuthenticationMethod(authenticationMethod()
                                .methodId(method.name()).referenceId(NEW_REFERENCE_ID).build())
                        .build());

        verifyService.beginVerification(method, nodeState, null);

        verify(nodeState).putShared(RESPONSE_MESSAGE_ID, NEW_MESSAGE_ID);
        verify(nodeState).putShared(AUTHN_ATTEMPT_ID, NEW_ATTEMPT_ID);
        verify(nodeState).putShared(PUSH_REFERENCE_ID, NEW_REFERENCE_ID);
        verify(nodeState).putShared(CONFIRMATION_CB, List.of("Cancel"));
    }

    private static Stream<Arguments> pushMethods() {
        return Stream.of(SecurIdMethod.values())
                .filter(method -> method.type().equals(PUSH))
                .map(Arguments::of);
    }
}
