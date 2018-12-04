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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.shared.encode.Base64;

/**
 * Abstract class for authentication and registration.
 */
abstract class AbstractWebAuthnNode extends AbstractDecisionNode {

    final Logger logger = LoggerFactory.getLogger("amAuth");

    /** Key to use in the session's AuthType. */
    static final String WEB_AUTHN_AUTH_TYPE = "WebAuthnAuthentication";

    /** Length of a generic secret key (in bytes). */
    private static final int SECRET_BYTE_LENGTH = 32;

    static final String RESOURCE_LOCATION = "org/forgerock/openam/auth/nodes/webauthn/";

    private static final String CHALLENGE = "web-authn-challenge";
    private static final String OUTCOME = "webAuthnOutcome";

    static final String UNSUPPORTED = "unsupported";

    /** Common outcomes between nodes. */
    static final String UNSUPPORTED_OUTCOME_ID = "unsupported"; // browser does not support webauthn
    static final String ERROR_OUTCOME_ID = "error"; // browser reported error (via DOM exception)
    static final String SUCCESS_OUTCOME_ID = "success"; // succeeded
    static final String FAILURE_OUTCOME_ID = "failure"; // failed internal to AM

    private final SecureRandom secureRandom;

    final ClientScriptUtilities clientScriptUtilities;
    final UserWebAuthnDeviceProfileManager webAuthnProfileManager;

    AbstractWebAuthnNode(ClientScriptUtilities clientScriptUtilities,
                         UserWebAuthnDeviceProfileManager webAuthnProfileManager,
                         SecureRandom secureRandom) {
        this.clientScriptUtilities = clientScriptUtilities;
        this.webAuthnProfileManager = webAuthnProfileManager;
        this.secureRandom = secureRandom;
    }

    /**
     * Retrieves the current challenge - either by generating it, or retrieving it from the shared state.
     *
     * This will modify the context's shared state to insert the challenge under a pre-selected key if
     * a challenge has to be generated.
     *
     * @param context authentication context
     * @return raw challenge bytes
     */
    byte[] getChallenge(TreeContext context) {
        byte[] challengeBytes;

        if (context.sharedState.get(CHALLENGE).isNull()) {
            challengeBytes = createRandomBytes();
            String base64String = Base64.encode(challengeBytes);
            context.sharedState.put(CHALLENGE, base64String);
        } else {
            String base64String = context.sharedState.get(CHALLENGE).asString();
            challengeBytes = Base64.decode(base64String);
        }

        return challengeBytes;
    }

    Action getCallbacksForWebAuthnInteraction(String script, TreeContext context,
                                              Callback... additionalCallbacks) throws NodeProcessException {
        ScriptTextOutputCallback registrationCallback = new ScriptTextOutputCallback(script);
        String spinnerScript = clientScriptUtilities.getSpinnerScript(context.request.locales);
        ScriptTextOutputCallback spinnerCallback = new ScriptTextOutputCallback(spinnerScript);
        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback(OUTCOME, "false");
        ImmutableList<Callback> callbacks = ImmutableList.<Callback>builder()
                .add(registrationCallback, spinnerCallback, hiddenValueCallback)
                .add(additionalCallbacks)
                .build();
        return send(callbacks)
                .replaceSharedState(context.sharedState)
                .build();
    }

    private byte[] createRandomBytes() {
        byte[] secretBytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(secretBytes);
        return secretBytes;
    }

    String getDomain(String serverUrl, Optional<String> configRpId) throws NodeProcessException {

        if (configRpId.isPresent()) {
            return configRpId.get();
        }

        try {
            return new URL(serverUrl).getHost();
        } catch (MalformedURLException e) {
            throw new NodeProcessException("Unable to parse host URL.", e);
        }
    }

    WebAuthnDomException parseError(String description) {
        WebAuthnDomException exception = WebAuthnDomException.parse(description);
        logger.warn("WebAuthn Dom Exception: ", exception);
        return exception;
    }

}
