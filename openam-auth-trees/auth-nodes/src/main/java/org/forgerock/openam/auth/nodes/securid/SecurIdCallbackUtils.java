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

import static javax.security.auth.callback.ConfirmationCallback.INFORMATION;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.QRCODE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.utils.qr.GenerationUtils.getQRCodeGenerationJavascriptForAuthenticatorAppRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * Utility class to generate the callbacks used by the SecurId node.
 */
final class SecurIdCallbackUtils {

    static final String CANCEL_BUTTON = "Cancel";
    static final int CANCEL_NOT_SELECTED_INDEX = 100;
    static final ConfirmationCallback CONFIRMATION_CALLBACK = new ConfirmationCallback(INFORMATION,
            new String[] { "Next", CANCEL_BUTTON }, 0);
    private static final ConfirmationCallback CANCEL_CALLBACK = new ConfirmationCallback(INFORMATION,
            new String[] { CANCEL_BUTTON }, 0);
    private static final String TWO_SECONDS_IN_MILLIS = "2000";

    private SecurIdCallbackUtils() {
    }

    static List<Callback> getPushAuthenticationCallbacks(
            NodeState nodeState, SecurIdResponse response, SecurIdMethod method, String waitingMessage)
            throws NodeProcessException {
        nodeState.putShared(RESPONSE_MESSAGE_ID, response.context().messageId());
        nodeState.putShared(AUTHN_ATTEMPT_ID, response.context().authnAttemptId());
        nodeState.putShared(PUSH_REFERENCE_ID, response.getReferenceId(method.name()));
        nodeState.putShared(CONFIRMATION_CB, Arrays.stream(CANCEL_CALLBACK.getOptions()).toList());

        List<Callback> callbacks = new ArrayList<>();
        if (method == QRCODE) {
            String url = getQrUrl(response);
            callbacks.add(generateQrCallback(url));
        }
        callbacks.add(new PollingWaitCallback(TWO_SECONDS_IN_MILLIS, waitingMessage));
        callbacks.add(CANCEL_CALLBACK);
        CANCEL_CALLBACK.setSelectedIndex(CANCEL_NOT_SELECTED_INDEX);
        return callbacks;
    }

    private static String getQrUrl(SecurIdResponse response) {
        return response.credentialValidationResults().get(0).authnAttributes().get(0).value();
    }

    private static Callback generateQrCallback(String url) {
        return new ScriptTextOutputCallback(
                getQRCodeGenerationJavascriptForAuthenticatorAppRegistration("callback_0", url, false));
    }
}
