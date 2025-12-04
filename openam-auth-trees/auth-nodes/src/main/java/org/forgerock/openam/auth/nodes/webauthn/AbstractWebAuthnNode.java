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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.WEBAUTHN_EXTENSIONS;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeDisplayNode;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
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

    /**
     * Key to use in the session's AuthType.
     */
    static final String WEB_AUTHN_AUTH_TYPE = "WebAuthnAuthentication";

    /**
     * Key to use if storing the webauthn data in shared state.
     */
    static final String WEB_AUTHN_STATE_DATA = "webauthnData";

    /**
     * Key to use if storing the webauthn attestation type in shared state.
     */
    static final String WEB_AUTHN_ATTESTATION_TYPE = "webauthnAttestationType";

    /**
     * Key to use if storing the authenticator AAGUID in shared state.
     */
    static final String WEB_AUTHN_AAGUID = "webauthnDeviceAaguid";

    /**
     * Key to use if storing the webauthn device UUID in shared state.
     */
    static final String WEB_AUTHN_DEVICE_UUID = "webauthnDeviceUuid";

    /**
     * Key to use if storing the webauthn device name in shared state.
     */
    static final String WEB_AUTHN_DEVICE_NAME = "webauthnDeviceName";

    /**
     * Key to use if storing the device in shared state.
     */
    static final String WEB_AUTHN_DEVICE_DATA = "webauthnDeviceData";

    /**
     * Key for information about the assertion for trees to consume.
     */
    static final String WEB_AUTHN_ATTESTATION_INFO = "webauthnAttestationInfo";

    /**
     * Key for information about the assertion for trees to consume.
     */
    static final String WEB_AUTHN_ASSERTION_INFO = "webauthnAssertionInfo";

    /**
     * Key for the authenticator attachment field.
     */
    static final String AUTHENTICATOR_ATTACHMENT = "authenticatorAttachment";

    /**
     * Key to contain all the flags. The keys for the flags were chosen from the
     * <a href="https://www.w3.org/TR/webauthn-3/#authdata-flags">WebAuthn spec</a>.
     */
    static final String FLAGS = "flags";

    /**
     * Key for the user present flag.
     */
    static final String USER_PRESENT = "UP";

    /**
     * Key for the user verified flag.
     */
    static final String USER_VERIFIED = "UV";

    /**
     * Key for the backup eligible flag.
     */
    static final String BACKUP_ELIGIBILITY = "BE";

    /**
     * Key for the backup status flag.
     */
    static final String BACKUP_STATE = "BS";

    /**
     * Key for the attested data flag.
     */
    static final String ATTESTED_CREDENTIAL_DATA_INCLUDED = "AT";

    /**
     * Key for the extension data flag.
     */
    static final String EXTENSION_DATA_INCLUDED = "ED";

    /**
     * Number of recovery codes to generate for webauthn devices by default.
     */
    static final int NUM_RECOVERY_CODES = 10;

    /**
     * Length of a generic secret key (in bytes).
     */
    private static final int SECRET_BYTE_LENGTH = 32;

    /**
     * Maximum saved devices.
     */
    static final String MAX_SAVED_DEVICES = "maxSavedDevices";

    /**
     * Outcome when exceed device limit.
     */
    static final String EXCEED_DEVICE_LIMIT_OUTCOME_ID = "exceedDeviceLimit";


    static final String RESOURCE_LOCATION = "org/forgerock/openam/auth/nodes/webauthn/";

    private static final String CHALLENGE = "web-authn-challenge";
    private static final String OUTCOME = "webAuthnOutcome";

    /**
     * Common outcomes between nodes.
     */
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
     * <p>
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

    Action getCallbacksForWebAuthnInteraction(boolean asScript, String script, JsonValue scriptContext,
            TreeContext context, Callback... additionalCallbacks) throws NodeProcessException {
        ImmutableList.Builder<Callback> callbacks = ImmutableList.builder();
        if (asScript) {
            script = MapFormat.format(script, scriptContext.asMap());
            callbacks.add(new ScriptTextOutputCallback(script));
            String spinnerScript = clientScriptUtilities.getSpinnerScript(context.request.locales);
            callbacks.add(new ScriptTextOutputCallback(spinnerScript));
        } else {
            scriptContext.put("_type", "WebAuthn");
            scriptContext.put("challenge", context.sharedState.get(CHALLENGE).asString());
            scriptContext.put("supportsJsonResponse", true);
            callbacks.add(new MetadataCallback(scriptContext));
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

    /**
     * Retrieves the extensions map from the node state.
     * @param context the tree context to retrieve the state from
     * @return the extensions map
     * @throws NodeProcessException if the extensions are not a map
     */
    protected JsonValue getExtensions(TreeContext context) throws NodeProcessException {
        JsonValue extensions = context.getStateFor(this).get(WEBAUTHN_EXTENSIONS);
        if (extensions == null || extensions.isNull()) {
            return json(object());
        }
        if (!extensions.isMap()) {
            logger.error("Extensions must be a map, but was {}", extensions.getObject());
            throw new NodeProcessException("Extensions must be a map");
        }
        return extensions;
    }

    protected JsonValue getWebAuthnObjectInfo(AuthData authData) {
        AttestationFlags attestationFlags = authData.attestationFlags;
        return json(object(
                field(FLAGS, object(
                        field(USER_PRESENT, attestationFlags.isUserPresent()),
                        field(USER_VERIFIED, attestationFlags.isUserVerified()),
                        field(ATTESTED_CREDENTIAL_DATA_INCLUDED, attestationFlags.isAttestedDataIncluded()),
                        field(EXTENSION_DATA_INCLUDED, attestationFlags.isExtensionDataIncluded()),
                        field(BACKUP_ELIGIBILITY, attestationFlags.isEligibleForBackup()),
                        field(BACKUP_STATE, attestationFlags.backupState())
                ))
        ));
    }

    protected void addAuthenticatorAttachment(JsonValue jsonValue, String authenticatorAttachment) {
        jsonValue.put(AUTHENTICATOR_ATTACHMENT, authenticatorAttachment);
    }
}
