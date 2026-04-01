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

import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.getPushAuthenticationCallbacks;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SECURID_NEW_PIN;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PUSH;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.FAILURE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.SUCCESS;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.inject.Inject;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AttemptResponseCode;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.MethodResponseCode;
import org.forgerock.secrets.NoSuchSecretException;

import com.google.inject.assistedinject.Assisted;

/**
 * Handles responses from SecurID initialize and verify requests.
 */
class SecurIdResponseHandler {
    private final AvailableMethodHandler methodHandler;

    @Inject
    SecurIdResponseHandler(@Assisted AvailableMethodHandler methodHandler) {
        this.methodHandler = methodHandler;
    }

    Action handleInitialize(SecurIdResponse response,
                            NodeState nodeState,
                            String waitingMessage,
                            String mfaChoicePrompt)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        if (AttemptResponseCode.SUCCESS.equals(response.attemptResponseCode())) {
            return Action.goTo(SUCCESS).build();
        }
        return methodHandler.handle(response, nodeState, waitingMessage, mfaChoicePrompt);
    }

    Action handleVerify(SecurIdResponse response,
                        NodeState nodeState,
                        SecurIdMethod method,
                        String waitingMessage,
                        String mfaChoicePrompt)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        if (AttemptResponseCode.SUCCESS.equals(response.attemptResponseCode())) {
            return Action.goTo(SUCCESS).build();
        }

        var methodResponseCode = getMethodResponseCode(response);
        if (methodResponseCode == null) {
            throw new NodeProcessException("Expected response to contain methodResponseCode");
        }
        return switch (methodResponseCode) {
        case SUCCESS -> {
            if (method.type().equals(PUSH)) {
                nodeState.putShared(PUSH_REFERENCE_ID, response.getReferenceId(method.name()));
            }
            yield methodHandler.handle(response, nodeState, waitingMessage, mfaChoicePrompt);
        }
        case FAIL -> {
            if (method.equals(SECURID_NEW_PIN)) {
                yield methodHandler.handle(response, nodeState, waitingMessage, mfaChoicePrompt);
            }
            yield Action.goTo(FAILURE).build();
        }
        case IN_PROCESS -> {
            var callbacks = getPushAuthenticationCallbacks(nodeState, response, method, waitingMessage);
            yield Action.send(callbacks).build();
        }
        };
    }

    private static MethodResponseCode getMethodResponseCode(SecurIdResponse response) {
        if (response.credentialValidationResults() == null) {
            return null;
        }
        return response.credentialValidationResults().stream()
                .map(SecurIdResponse.CredentialValidationResult::methodResponseCode)
                .findFirst().orElse(null);
    }

    /**
     * Factory to create SecurIdResponseHandler instances.
     */
    interface Factory {
        SecurIdResponseHandler create(AvailableMethodHandler verifyService);
    }
}
