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
 * Copyright 2018-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.ERROR_MESSAGE;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.WEB_AUTHENTICATION_DOM_EXCEPTION;

import java.security.SecureRandom;
import java.util.ArrayList;
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
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
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

    private static final String NO_DEVICE_OUTCOME_ID = "noDevice";
    private static final String RECOVERY_CODE_OUTCOME_ID = "recoveryCode";

    private static final int RECOVERY_PRESSED = 0;
    private static final int RECOVERY_NOT_PRESSED = 100;

    private final Config config;
    private final Realm realm;
    private final AuthenticationFlow authenticationFlow;
    private final AuthDataDecoder authDataDecoder;
    private final LegacyIdentityService identityService;
    private final IdentityStoreFactory identityStoreFactory;
    private final CoreWrapper coreWrapper;

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
         * Specify whether to return the challenge as a script or just as metadata.
         *
         * @return {@literal true} if return as a script.
         */
        @Attribute(order = 60)
        default boolean asScript() {
            return true;
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
     * @param identityService        an {@link LegacyIdentityService} instance.
     * @param identityStoreFactory   an {@link IdentityStoreFactory} instance
     * @param coreWrapper            An instance of the {@code CoreWrapper}.
     */
    @Inject
    public WebAuthnAuthenticationNode(@Assisted Config config, @Assisted Realm realm,
            AuthenticationFlow authenticationFlow,
            ClientScriptUtilities clientScriptUtilities,
            UserWebAuthnDeviceProfileManager webAuthnProfileManager,
            SecureRandom secureRandom, AuthDataDecoder authDataDecoder,
            RecoveryCodeGenerator recoveryCodeGenerator, LegacyIdentityService identityService,
            IdentityStoreFactory identityStoreFactory, CoreWrapper coreWrapper) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom, recoveryCodeGenerator);

        this.config = config;
        this.realm = realm;
        this.authenticationFlow = authenticationFlow;
        this.authDataDecoder = authDataDecoder;
        this.identityService = identityService;
        this.coreWrapper = coreWrapper;
        this.identityStoreFactory = identityStoreFactory;
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
                config.relyingPartyDomain().orElse(null));

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
            logger.debug("processing user response");
            if (result.get().equals(UNSUPPORTED)) {
                logger.warn("User's user agent does not support WebAuthn");
                return Action.goTo(UNSUPPORTED_OUTCOME_ID).build();
            }
            if (result.get().startsWith(ERROR_MESSAGE)) {
                WebAuthnDomException exception = parseError(result.get());
                logger.error("An error was reported by the DOM.");
                return Action.goTo(ERROR_OUTCOME_ID).replaceSharedState(
                        context.sharedState.copy().put(WEB_AUTHENTICATION_DOM_EXCEPTION, exception.toString())).build();
            }

            String rpId = getDomain(config.relyingPartyDomain(), context.request.headers.get("origin"),
                    context.request.serverUrl);

            ClientAuthenticationScriptResponse response =
                    clientScriptUtilities.parseClientAuthenticationResponse(result.get(), config.requiresResidentKey());

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

            logger.debug("registeredDeviceUuid: {}, credentialId: {}, deviceName: {}",
                    registeredDeviceUuid, credentialId, deviceName);

            if (authenticationFlow.accept(realm, response.getClientData(), authData,
                    response.getSignature(), challengeBytes, rpId, device,
                    getPermittedOrigins(config.origins(), context), config.userVerificationRequirement())) {
                logger.debug("returning with success outcome");

                Action.ActionBuilder responseAction = Action.goTo(SUCCESS_OUTCOME_ID);

                //we are responsible for putting the username into the shared state so the appropriate user logs in
                if (config.requiresResidentKey()) {
                    responseAction
                            .replaceSharedState(context.sharedState.copy().put(USERNAME, response.getUserHandle()));
                }

                NodeState nodeState = context.getStateFor(this);
                Action.ActionBuilder actionBuilder = responseAction.addNodeType(context, WEB_AUTHN_AUTH_TYPE);
                Optional<AMIdentity> identity = getAMIdentity(context.universalId, nodeState, identityService,
                        coreWrapper);
                identity.ifPresent(actionBuilder::withIdentifiedIdentity);
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

    private JsonValue getScriptContext(byte[] challengeBytes, List<WebAuthnDeviceSettings> devices,
            String configuredRpId) {
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
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnAuthenticationNode.OutcomeProvider.class.getClassLoader());

            ArrayList<Outcome> outcomes = new ArrayList<>();

            outcomes.add(new Outcome(UNSUPPORTED_OUTCOME_ID, bundle.getString(UNSUPPORTED)));
            outcomes.add(new Outcome(NO_DEVICE_OUTCOME_ID, bundle.getString(NO_DEVICE_OUTCOME_ID)));
            outcomes.add(new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)));
            outcomes.add(new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)));
            outcomes.add(new Outcome(ERROR_OUTCOME_ID, bundle.getString(ERROR_OUTCOME_ID)));

            if (nodeAttributes.isNotNull()) {
                // nodeAttributes is null when the node is created
                if (nodeAttributes.get(IS_RECOVERY_CODE_ALLOWED).required().asBoolean()) {
                    outcomes.add(new Outcome(RECOVERY_CODE_OUTCOME_ID, bundle.getString(RECOVERY_CODE_OUTCOME_ID)));
                }
            }

            return ImmutableList.copyOf(outcomes);
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
