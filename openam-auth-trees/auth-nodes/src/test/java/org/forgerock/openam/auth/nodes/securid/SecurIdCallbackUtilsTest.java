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
import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.getPushAuthenticationCallbacks;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.APPROVE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.QRCODE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.AuthenticationMethodBuilder.authenticationMethod;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.CredentialValidationResultBuilder.credentialValidationResult;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

@ExtendWith(MockitoExtension.class)
class SecurIdCallbackUtilsTest {

    private NodeState nodeState;

    @BeforeEach
    void setUp() {
        nodeState = createEmptyNodeState();
    }

    private static NodeState createEmptyNodeState() {
        TreeContext treeContext = TreeContext.builder().build();
        var node = mock(Node.class);
        given(node.getInputs()).willReturn(new InputState[] {});
        return treeContext.getStateFor(node);
    }

    @Test
    void returnsPushAuthenticationCallbacks() throws Exception {
        var response = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId("APPROVE").referenceId("someId").build())
                .build();
        String waitingMessage = "waiting message";

        var callbacks = getPushAuthenticationCallbacks(nodeState, response, APPROVE, waitingMessage);

        assertThat(callbacks).hasSize(2);
        assertThat(callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        var pollingWaitCallback = (PollingWaitCallback) callbacks.get(0);
        assertThat(pollingWaitCallback.getMessage()).isEqualTo(waitingMessage);
        assertThat(pollingWaitCallback.getWaitTime()).isEqualTo("2000");
        assertThat(callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        var cancelCallback = (ConfirmationCallback) callbacks.get(1);
        assertThat(cancelCallback.getOptions()).containsExactly("Cancel");
        assertThat(cancelCallback.getSelectedIndex()).isEqualTo(100);
    }

    @Test
    void addsQrCallbackIfApplicable() throws Exception {
        var response = securIdResponse()
                .withCredentialValidationResult(credentialValidationResult().withQrCodeUrl("QrUrl").build())
                .withAuthenticationMethod(authenticationMethod().methodId("QRCODE").referenceId("someId").build())
                .build();

        var callbacks = getPushAuthenticationCallbacks(nodeState, response, QRCODE, "");

        assertThat(callbacks).hasSize(3);
        assertThat(callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        var scriptTextOutputCallback = (ScriptTextOutputCallback) callbacks.get(0);
        assertThat(scriptTextOutputCallback.getMessage()).contains("text: 'QrUrl'");
    }

    @Test
    void setsStateParametersCorrespondingToProvidedAuthenticationMethod() throws Exception {
        var response = securIdResponse()
                .messageId("messageId")
                .authnAttemptId("attemptId")
                .withAuthenticationMethod(authenticationMethod().methodId("SECURID").referenceId("someId").build())
                .withAuthenticationMethod(authenticationMethod()
                        .methodId("APPROVE").referenceId("pushReferenceId").build())
                .build();

        getPushAuthenticationCallbacks(nodeState, response, APPROVE, "");

        assertThat(requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString()).isEqualTo("messageId");
        assertThat(requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString()).isEqualTo("attemptId");
        assertThat(requireNonNull(nodeState.get(PUSH_REFERENCE_ID)).asString()).isEqualTo("pushReferenceId");
        assertThat(requireNonNull(nodeState.get(CONFIRMATION_CB)).asList()).containsExactly("Cancel");
    }
}
