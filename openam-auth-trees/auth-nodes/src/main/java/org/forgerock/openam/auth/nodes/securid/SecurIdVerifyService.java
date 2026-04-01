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
import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.CONFIRMATION_CALLBACK;
import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.getPushAuthenticationCallbacks;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.secrets.NoSuchSecretException;

import com.google.inject.assistedinject.Assisted;

/**
 * Service to handle SecurId verification process.
 */
class SecurIdVerifyService {
    private final SecurIdRestClient securIdRestClient;

    @Inject
    SecurIdVerifyService(@Assisted SecurIdRestClient securIdRestClient) {
        this.securIdRestClient = securIdRestClient;
    }

    List<Callback> beginVerification(SecurIdMethod method, NodeState nodeState, String waitingMessage)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        return switch (method.type()) {
        case PASSWORD -> setUpPassword(method, nodeState);
        case VOICE_OR_SMS -> setUpVoiceOrSms(method, nodeState);
        case PUSH -> {
            var referenceId = Optional.ofNullable(nodeState.get(PUSH_REFERENCE_ID))
                    .map(JsonValue::asString)
                    .orElse(null);
            var verifyResponse = securIdRestClient.verify(
                    requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString(),
                    requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString(),
                    getSubjectCredentials(method, referenceId));
            yield getPushAuthenticationCallbacks(nodeState, verifyResponse, method, waitingMessage);
        }
        };
    }

    private static List<SubjectCredentials> getSubjectCredentials(SecurIdMethod method, String referenceId) {
        return List.of(new SubjectCredentials(method.name(), referenceId, null));
    }

    private static List<Callback> setUpPassword(SecurIdMethod method, NodeState nodeState) {
        nodeState.putShared(CONFIRMATION_CB, CONFIRMATION_CALLBACK.getOptions());
        return List.of(new PasswordCallback(method.prompt(), true), CONFIRMATION_CALLBACK);
    }

    private List<Callback> setUpVoiceOrSms(SecurIdMethod method, NodeState nodeState)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        var verifyResponse = securIdRestClient.verify(
                requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString(),
                requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString(),
                getVoiceOrSmsSubjectCredentials(method.name() + "_REQUEST_CODE"));

        nodeState.putShared(CONFIRMATION_CB, CONFIRMATION_CALLBACK.getOptions());
        nodeState.putShared(RESPONSE_MESSAGE_ID, verifyResponse.context().messageId());
        nodeState.putShared(AUTHN_ATTEMPT_ID,  verifyResponse.context().authnAttemptId());

        return List.of(new PasswordCallback(method.prompt(), true), CONFIRMATION_CALLBACK);
    }

    private static List<SubjectCredentials> getVoiceOrSmsSubjectCredentials(String methodId) {
        return List.of(new SubjectCredentials(methodId, null, null));
    }

    /**
     * Factory to create SecurIdVerifyService instances.
     */
    interface Factory {
        SecurIdVerifyService create(SecurIdRestClient securIdRestClient);
    }
}
