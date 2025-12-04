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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.WEB_AUTHENTICATION_DOM_EXCEPTION;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Strings;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.AMIdentity;

/**
 * A web authentication, authentication node. Uses client side javascript to interact with the browser and negotiate
 * authentication according to the webauthn spec. If the credentials, public key encrypted challenge response and
 * other details are valid, the outcome of the node is 'true'.
 */
@Node.Metadata(outcomeProvider = WebAuthnAuthenticationNode.OutcomeProvider.class,
        configClass = WebAuthnAuthenticationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class WebAuthnAuthenticationNode extends AbstractWebAuthnNode {

    private static final String BUNDLE = WebAuthnAuthenticationNode.class.getName();
    private static final String AUTH_SCRIPT = RESOURCE_LOCATION + "webauthn-client-auth-script.js";

    private static final String IS_RECOVERY_CODE_ALLOWED = "isRecoveryCodeAllowed";
    private static final String DETECT_SIGN_COUNT_MISMATCH = "detectSignCountMismatch";

    /**
     * The outcome when the user's device processing has succeeded, but a sign count mismatch was detected.
     */
    public static final String SIGN_COUNT_MISMATCH_OUTCOME_ID = "successSignCountMismatch";
    static final String NO_DEVICE_OUTCOME_ID = "noDevice";
    static final String RECOVERY_CODE_OUTCOME_ID = "recoveryCode";

    private static final int RECOVERY_PRESSED = 0;
    private static final int RECOVERY_NOT_PRESSED = 100;

    private final Config config;
    private final Realm realm;
    private final AuthenticationFlow authenticationFlow;
    private final AuthDataDecoder authDataDecoder;
    private final IdentityStoreFactory identityStoreFactory;
    private final NodeUserIdentityProvider identityProvider;
    private final WebAuthnOutcomeDeserializer deserializer;

    private String registeredDeviceUuid;
    private String deviceName;
    private String credentialId;
    private String userHandle;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Sets the domain. If left empty, AM will attempt a best-guess.
         *
         * @return the relying party domain.
         */
        @Attribute(order = 10)
        default Optional<String> relyingPartyDomain() {
            return Optional.empty();
        }

        /**
         * Sets the origins from which this node accepts requests.
         *
         * @return the set of valid origins
         */
        @Attribute(order = 15)
        default Set<String> origins() {
            return Collections.emptySet();
        }

        /**
         * If true, requires the user to be verified in the auth flow.
         *
         * @return is verification required.
         */
        @Attribute(order = 20)
        default UserVerificationRequirement userVerificationRequirement() {
            return UserVerificationRequirement.PREFERRED;
        }

        /**
         * If true, allows rendering of a button to use a recovery code.
         *
         * @return is the recovery code button shown.
         */
        @Attribute(order = 30)
        default boolean isRecoveryCodeAllowed() {
            return false;
        }

        /**
         * Specifies the timeout of the authentication node.
         *
         * @return the length of time to wait for a device to authenticate.
         */
        @Attribute(order = 40)
        default int timeout() {
            return 60;
        }

        /**
         * Specify that the authenticators used during registration must support resident credentials
         * (storing the username on the device) and will therefore supply the username to the node.
         *
         * @return {@literal true} if resident key support is required.
         */
        @Attribute(order = 50)
        default boolean requiresResidentKey() {
            return false;
        }

        /**
         * WebAuthn2 equivalent to requiresResidentKey. Not currently configurable on the legacy node.
         *
         * @return {@link ResidentKeyRequirement} REQUIRED or DISCOURAGED
         */
        default ResidentKeyRequirement residentKeyRequirement() {
            return requiresResidentKey() ? ResidentKeyRequirement.REQUIRED : ResidentKeyRequirement.DISCOURAGED;
        }

        /**
         * Specify whether to return the challenge as a script or just as metadata.
         *
         * @return {@literal true} if return as a script.
         */
        @Attribute(order = 60)
        default boolean asScript() {
            return true;
        }

        /**
         * Specify whether to detect signature counter mismatch in the provided device attestation.
         *
         * @return {@literal true} if signature counter mismatch detection is required.
         */
        @Attribute(order = 70)
        default boolean detectSignCountMismatch() {
            return false;
        }

    }

    /**
     * The constructor.
     *
     * @param config                 the node's config.
     * @param realm                  the realm.
     * @param authenticationFlow     the authentication flow as executable steps.
     * @param clientScriptUtilities  utilities for handling the client side script.
     * @param webAuthnProfileManager management of user's devices
     * @param secureRandom           instance of the secure random generator
     * @param authDataDecoder        instance of the auth data decoder
     * @param recoveryCodeGenerator  instance of the recovery code generator.
     * @param identityStoreFactory   an {@link IdentityStoreFactory} instance
     * @param identityProvider       the identity provider
     * @param deserializer           instance of the assertion object deserializer
     */
    @Inject
    public WebAuthnAuthenticationNode(@Assisted Config config, @Assisted Realm realm,
            AuthenticationFlow authenticationFlow,
            ClientScriptUtilities clientScriptUtilities,
            UserWebAuthnDeviceProfileManager webAuthnProfileManager,
            SecureRandom secureRandom, AuthDataDecoder authDataDecoder,
            RecoveryCodeGenerator recoveryCodeGenerator, IdentityStoreFactory identityStoreFactory,
            NodeUserIdentityProvider identityProvider, WebAuthnOutcomeDeserializer deserializer) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom, recoveryCodeGenerator);

        this.config = config;
        this.realm = realm;
        this.authenticationFlow = authenticationFlow;
        this.authDataDecoder = authDataDecoder;
        this.identityStoreFactory = identityStoreFactory;
        this.identityProvider = identityProvider;
        this.deserializer = deserializer;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("WebAuthnAuthenticationNode started");
        byte[] challengeBytes = getChallenge(context);
        List<WebAuthnDeviceSettings> devices = null;

        if (!config.requiresResidentKey()) {
            try {
                userHandle = context.sharedState.get(USERNAME).asString();
                devices = getDeviceSettingsFromUsername(userHandle, context.sharedState.get(REALM).asString());
                if (CollectionUtils.isEmpty(devices)) {
                    return Action.goTo(NO_DEVICE_OUTCOME_ID).build();
                }
            } catch (DevicePersistenceException e) {
                logger.error("Unable to retrieve user's device profile information.", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
        }

        String authScript = clientScriptUtilities.getScriptAsString(AUTH_SCRIPT);
        JsonValue scriptContext = getScriptContext(challengeBytes, devices,
                config.relyingPartyDomain().orElse(null),
                getExtensions(context));

        if (context.getCallback(ConfirmationCallback.class)
                .filter(callback -> callback.getSelectedIndex() == RECOVERY_PRESSED)
                .isPresent()) {
            logger.debug("returning with recovery code outcome");
            return Action.goTo(RECOVERY_CODE_OUTCOME_ID).build();
        }

        Optional<String> result = context.getCallback(HiddenValueCallback.class)
                .map(HiddenValueCallback::getValue)
                .filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));

        if (result.isPresent()) {
            WebAuthnOutcome outcome = deserializer.deserialize(result.get());
            logger.debug("processing user response");
            if (!outcome.isSupported()) {
                logger.warn("User's user agent does not support WebAuthn");
                return Action.goTo(UNSUPPORTED_OUTCOME_ID).build();
            }
            if (outcome.isError()) {
                WebAuthnDomException exception = outcome.error().get();
                logger.error("An error was reported by the DOM.");
                return Action.goTo(ERROR_OUTCOME_ID).replaceSharedState(
                        context.sharedState.copy().put(WEB_AUTHENTICATION_DOM_EXCEPTION, exception.toString())).build();
            }

            String rpId = getDomain(config.relyingPartyDomain(), context.request.headers.get("origin"),
                    context.request.serverUrl);

            ClientAuthenticationScriptResponse response = clientScriptUtilities.parseClientAuthenticationResponse(
                    outcome.legacyData(), config.requiresResidentKey());

            if (config.requiresResidentKey()) {
                userHandle = response.getUserHandle();
                try {
                    devices = getDeviceSettingsFromUsername(userHandle, context.sharedState.get(REALM).asString());
                    if (devices.isEmpty()) {
                        return Action.goTo(NO_DEVICE_OUTCOME_ID).build();
                    }
                } catch (DevicePersistenceException e) {
                    logger.error("Unable to retrieve user's device profile information.", e);
                    return Action.goTo(FAILURE_OUTCOME_ID).build();
                }
            }

            WebAuthnDeviceSettings device = getEntry(response.getCredentialId(), devices);
            if (device == null) {
                logger.warn("No registered device found for credentialId: {}.", credentialId);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

            registeredDeviceUuid = device.getUUID();
            credentialId = device.getCredentialId();
            deviceName = device.getDeviceName();

            AuthData authData;
            try {
                authData = authDataDecoder.decode(response.getAuthenticatorData());
            } catch (DecodingException e) {
                logger.error("Unable to decode provided auth data.", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

            context.getStateFor(this)
                    .putShared(WEB_AUTHN_DEVICE_UUID, registeredDeviceUuid)
                    .putShared(WEB_AUTHN_DEVICE_NAME, deviceName);

            logger.debug("registeredDeviceUuid: {}, credentialId: {}, deviceName: {}",
                    registeredDeviceUuid, credentialId, deviceName);

            if (authenticationFlow.accept(realm, response.getClientData(), authData,
                    response.getSignature(), challengeBytes, rpId, device,
                    getPermittedOrigins(config.origins(), context), config.userVerificationRequirement())) {
                logger.debug("returning with success outcome");

                Action.ActionBuilder responseAction = Action.goTo(SUCCESS_OUTCOME_ID);

                // SignCount of zero indicates signature counter is not supported
                // Section 7.2.21 https://www.w3.org/TR/webauthn/#sctn-verifying-assertion
                if (config.detectSignCountMismatch() && authData.signCount != 0
                        && authData.signCount <= device.getSignCount()) {
                    responseAction = Action.goTo(SIGN_COUNT_MISMATCH_OUTCOME_ID);
                }

                //we are responsible for putting the username into the shared state so the appropriate user logs in
                if (config.requiresResidentKey()) {
                    responseAction
                            .replaceSharedState(context.sharedState.copy().put(USERNAME, response.getUserHandle()));
                }

                try {
                    updateLastAccessDateAndSignCount(device.getUUID(), devices, authData.signCount);
                    webAuthnProfileManager.saveDeviceProfile(userHandle,
                            context.sharedState.get(REALM).asString(), devices);
                } catch (DevicePersistenceException e) {
                    logger.warn("Setting last access date to the device failed", e);
                }

                NodeState nodeState = context.getStateFor(this);
                Action.ActionBuilder actionBuilder = responseAction.addNodeType(context, WEB_AUTHN_AUTH_TYPE);
                Optional<AMIdentity> identity = identityProvider.getAMIdentity(context.universalId, nodeState);
                identity.ifPresent(actionBuilder::withIdentifiedIdentity);

                JsonValue webAuthnObjectInfo = getWebAuthnObjectInfo(authData);

                outcome.authenticatorAttachment().ifPresent(authenticatorAttachment ->
                        addAuthenticatorAttachment(webAuthnObjectInfo, authenticatorAttachment));

                nodeState.putTransient(WEB_AUTHN_ASSERTION_INFO, webAuthnObjectInfo);

                return actionBuilder.withUniversalId(identity.map(AMIdentity::getUniversalId)).build();
            } else {
                logger.debug("returning with failure outcome");
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
        } else {
            logger.debug("sending callbacks to user for completion");
            Callback[] additionalCallbacks = getAdditionalCallbacks(context.request.locales);
            return getCallbacksForWebAuthnInteraction(config.asScript(), authScript, scriptContext, context,
                    additionalCallbacks);
        }
    }

    /**
     * Retrieve the device settings from user.
     *
     * @param username The username.
     * @param realm    The Realm.
     * @return Device Settings which associated with the user.
     * @throws DevicePersistenceException Failed to retrieve user devices.
     */
    @VisibleForTesting
    protected List<WebAuthnDeviceSettings> getDeviceSettingsFromUsername(String username, String realm)
            throws DevicePersistenceException {
        List<WebAuthnDeviceSettings> devices;
        logger.debug("getting user data and device data");
        IdentityStore identityStore = identityStoreFactory.create(realm);
        AMIdentity user = identityStore.getUserUsingAuthenticationUserAliases(username);
        if (user == null) {
            throw new DevicePersistenceException("getIdentity: Unable to find user " + username);
        } else {
            devices = webAuthnProfileManager.getDeviceProfiles(user.getName(), realm);
        }
        return devices;
    }

    private void updateLastAccessDateAndSignCount(String deviceUUID, List<WebAuthnDeviceSettings> devices,
            int signCount) {
        for (WebAuthnDeviceSettings device : devices) {
            if (device.getUUID().equals(deviceUUID)) {
                device.setLastAccessDate(Time.currentTimeMillis());
                device.setSignCount(signCount);
            }
        }
    }

    private JsonValue getScriptContext(byte[] challengeBytes, List<WebAuthnDeviceSettings> devices,
            String configuredRpId, JsonValue extensions) {
        JsonValue jsonValue = json(object());
        jsonValue.put("_action", "webauthn_authentication");
        jsonValue.put("challenge", Arrays.toString(challengeBytes));
        jsonValue.put("allowCredentials", getAllowCredentials(devices));
        jsonValue.put("_allowCredentials", getAllowCredentialsAsJson(devices));
        jsonValue.put("timeout", String.valueOf(config.timeout() * 1000));
        jsonValue.put("userVerification", config.userVerificationRequirement().getValue());
        if (config.asScript() && config.isRecoveryCodeAllowed()) {
            jsonValue.put("allowRecoveryCode", String.valueOf(config.isRecoveryCodeAllowed()));
        }

        StringBuilder sb = new StringBuilder();
        if (configuredRpId != null) {
            sb.append("rpId: \"").append(configuredRpId).append("\",");
        } else {
            configuredRpId = "";
        }
        jsonValue.put("relyingPartyId", sb.toString());
        jsonValue.put("_relyingPartyId", configuredRpId);
        jsonValue.put("extensions", extensions);

        return jsonValue;
    }

    private JsonValue getAllowCredentialsAsJson(List<WebAuthnDeviceSettings> devices) {
        return CollectionUtils.isEmpty(devices) ? json(array())
                : clientScriptUtilities.getDevicesAsJson(devices);
    }

    private String getAllowCredentials(List<WebAuthnDeviceSettings> devices) {
        return CollectionUtils.isEmpty(devices) ? ""
                : "allowCredentials: " + array(clientScriptUtilities.getDevicesAsJavaScript(devices)).toString();
    }

    private Callback[] getAdditionalCallbacks(PreferredLocales locales) {

        if (config.isRecoveryCodeAllowed()) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnAuthenticationNode.OutcomeProvider.class.getClassLoader());

            ConfirmationCallback confirmationCallback =
                    new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                            new String[]{bundle.getString("recoveryCodeButton")}, RECOVERY_PRESSED);
            confirmationCallback.setSelectedIndex(RECOVERY_NOT_PRESSED);

            return new Callback[]{confirmationCallback};
        }

        return new Callback[0];
    }

    private WebAuthnDeviceSettings getEntry(String credentialId, List<WebAuthnDeviceSettings> webAuthData)
            throws NodeProcessException {
        for (WebAuthnDeviceSettings entry : webAuthData) {
            if (entry.getCredentialId().equals(credentialId)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements BoundedOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return getAllOutcomes(locales).stream()
                    .filter(outcome -> {
                        if (RECOVERY_CODE_OUTCOME_ID.equals(outcome.id)) {
                            return nodeAttributes.get(IS_RECOVERY_CODE_ALLOWED).defaultTo(false).asBoolean();
                        }
                        if (SIGN_COUNT_MISMATCH_OUTCOME_ID.equals(outcome.id)) {
                            return nodeAttributes.get(DETECT_SIGN_COUNT_MISMATCH).defaultTo(false).asBoolean();
                        }
                        return true;
                    }).toList();
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnAuthenticationNode.OutcomeProvider.class.getClassLoader());
            return List.of(
                    new Outcome(UNSUPPORTED_OUTCOME_ID, bundle.getString(UNSUPPORTED_OUTCOME_ID)),
                    new Outcome(NO_DEVICE_OUTCOME_ID, bundle.getString(NO_DEVICE_OUTCOME_ID)),
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)),
                    new Outcome(ERROR_OUTCOME_ID, bundle.getString(ERROR_OUTCOME_ID)),
                    new Outcome(RECOVERY_CODE_OUTCOME_ID, bundle.getString(RECOVERY_CODE_OUTCOME_ID)),
                    new Outcome(SIGN_COUNT_MISMATCH_OUTCOME_ID, bundle.getString(SIGN_COUNT_MISMATCH_OUTCOME_ID)));
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(
                field("registeredDeviceUuid", registeredDeviceUuid),
                field("credentialId", credentialId),
                field("deviceName", deviceName),
                field("userHandle", userHandle)));
    }

}
