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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeDisplayNode;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.tools.objects.MapFormat;

/**
 * Abstract class for webauthn nodes, shares common features and components across the multiple webauthn-related nodes.
 */
abstract class AbstractWebAuthnNode extends AbstractDecisionNode {

    final Logger logger = LoggerFactory.getLogger(AbstractWebAuthnNode.class);

    /** Key to use in the session's AuthType. */
    static final String WEB_AUTHN_AUTH_TYPE = "WebAuthnAuthentication";

    /** Key to use if storing the webauthn data in shared state. */
    static final String WEB_AUTHN_STATE_DATA = "webauthnData";

    /** Key to use if storing the webauthn attestation type in shared state. */
    static final String WEB_AUTHN_ATTESTATION_TYPE = "webauthnAttestationType";

    /** Key to use if storing the device in shared state. */
    static final String WEB_AUTHN_DEVICE_DATA = "webauthnDeviceData";

    /** Number of recovery codes to generate for webauthn devices by default. */
    static final int NUM_RECOVERY_CODES = 10;

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
    private final RecoveryCodeGenerator recoveryCodeGenerator;

    final ClientScriptUtilities clientScriptUtilities;
    final UserWebAuthnDeviceProfileManager webAuthnProfileManager;

    AbstractWebAuthnNode(ClientScriptUtilities clientScriptUtilities,
                         UserWebAuthnDeviceProfileManager webAuthnProfileManager,
                         SecureRandom secureRandom, RecoveryCodeGenerator recoveryCodeGenerator) {
        this.clientScriptUtilities = clientScriptUtilities;
        this.webAuthnProfileManager = webAuthnProfileManager;
        this.secureRandom = secureRandom;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
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

    Action getCallbacksForWebAuthnInteraction(boolean asScript, String script, Map<String, String> scriptContext,
            TreeContext context, Callback... additionalCallbacks) throws NodeProcessException {
        ImmutableList.Builder<Callback> callbacks = ImmutableList.builder();
        if (asScript) {
            script = MapFormat.format(script, scriptContext);
            callbacks.add(new ScriptTextOutputCallback(script));
            String spinnerScript = clientScriptUtilities.getSpinnerScript(context.request.locales);
            callbacks.add(new ScriptTextOutputCallback(spinnerScript));
        } else {
            scriptContext.put("_type", "WebAuthn");
            scriptContext = new HashMap<>(scriptContext);
            scriptContext.put("challenge", context.sharedState.get(CHALLENGE).asString());
            callbacks.add(new MetadataCallback(JsonValue.json(scriptContext)));
        }
        return send(callbacks.add(new HiddenValueCallback(OUTCOME, "false")).add(additionalCallbacks).build())
                .replaceSharedState(context.sharedState)
                .build();
    }

    private byte[] createRandomBytes() {
        byte[] secretBytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(secretBytes);
        return secretBytes;
    }

    String getDomain(Optional<String> configRpId, List<String> originHeader, String serverUrl)
            throws NodeProcessException {

        if (configRpId.isPresent()) {
            return configRpId.get();
        }

        if (CollectionUtils.isNotEmpty(originHeader)) {
            try {
                return new URL(originHeader.get(0)).getHost();
            } catch (MalformedURLException e) {
                throw new NodeProcessException("Unable to parse origin header URL.", e);
            }
        }

        try {
            return new URL(serverUrl).getHost();
        } catch (MalformedURLException e) {
            throw new NodeProcessException("Unable to parse host URL.", e);
        }

    }

    Set<String> getPermittedOrigins(Set<String> configOrigins, TreeContext context) {
        Set<String> origins;
        if (!configOrigins.isEmpty()) {
            origins = configOrigins;
        } else {
            origins = new HashSet<>(1);
            List<String> originHeader = context.request.headers.get("origin");
            if (CollectionUtils.isNotEmpty(originHeader)) {
                if (!originHeader.get(0).equalsIgnoreCase("null")) {
                    origins.add(originHeader.get(0));
                }
            }
            logger.debug("no origins set in config. Fallback using {}", origins);
        }
        return origins;
    }

    WebAuthnDomException parseError(String description) {
        WebAuthnDomException exception = WebAuthnDomException.parse(description);
        logger.warn("WebAuthn Dom Exception: ", exception);
        return exception;
    }


    void setRecoveryCodesOnDevice(boolean generateRecoveryCodes, WebAuthnDeviceSettings device,
                                          JsonValue transientState) throws CodeException {
        //generate recovery codes
        if (generateRecoveryCodes) {
            logger.debug("creating recovery codes for device");
            List<String> codes = recoveryCodeGenerator.generateCodes(NUM_RECOVERY_CODES, Alphabet.ALPHANUMERIC,
                    false);
            device.setRecoveryCodes(codes);
            transientState.put(RecoveryCodeDisplayNode.RECOVERY_CODE_KEY, codes);
            transientState.put(RecoveryCodeDisplayNode.RECOVERY_CODE_DEVICE_NAME, device.getDeviceName());
        }
    }

}
