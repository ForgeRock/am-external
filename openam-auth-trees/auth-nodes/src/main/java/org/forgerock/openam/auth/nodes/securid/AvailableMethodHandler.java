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

import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.CONFIRMATION_CALLBACK;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AVAILABLE_METHODS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CHOSEN_METHOD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.NOT_ENROLLED;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import jakarta.inject.Inject;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.secrets.NoSuchSecretException;

import com.google.inject.assistedinject.Assisted;

/**
 * Handles logic based on available methods returned in SecurId response.
 */
class AvailableMethodHandler {

    private final SecurIdVerifyService verifyService;

    @Inject
    AvailableMethodHandler(@Assisted SecurIdVerifyService verifyService) {
        this.verifyService = verifyService;
    }

    Action handle(SecurIdResponse response,
                  NodeState nodeState,
                  String waitingMessage,
                  String mfaChoicePrompt)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        List<SecurIdMethod> availableMethods = response.getAvailableMethods();
        switch (availableMethods.size()) {
        case 0: {
            return Action.goTo(NOT_ENROLLED).build();
        }
        case 1: {
            nodeState.putShared(RESPONSE_MESSAGE_ID, response.context().messageId());
            nodeState.putShared(AUTHN_ATTEMPT_ID, response.context().authnAttemptId());
            nodeState.putShared(CHOSEN_METHOD, availableMethods.get(0).name());
            return Action.send(verifyService
                    .beginVerification(availableMethods.get(0), nodeState, waitingMessage)).build();
        }
        default: {
            nodeState.putShared(RESPONSE_MESSAGE_ID, response.context().messageId());
            nodeState.putShared(AUTHN_ATTEMPT_ID, response.context().authnAttemptId());
            nodeState.putShared(AVAILABLE_METHODS, availableMethods.stream().map(SecurIdMethod::name).toList());
            nodeState.putShared(CONFIRMATION_CB, Stream.of(CONFIRMATION_CALLBACK.getOptions()).toList());
            return Action.send(choiceCallbacks(availableMethods, mfaChoicePrompt)).build();
        }
        }
    }

    private static List<Callback> choiceCallbacks(List<SecurIdMethod> availableMethods, String mfaChoicePrompt) {
        String[] methodNames = availableMethods.stream().map(SecurIdMethod::displayName).toArray(String[]::new);
        return List.of(
                new ChoiceCallback(mfaChoicePrompt, methodNames, 0, false),
                CONFIRMATION_CALLBACK);
    }

    /**
     * Factory to create AvailableMethodHandler instances.
     */
    interface Factory {
        AvailableMethodHandler create(SecurIdVerifyService verifyService);
    }
}
