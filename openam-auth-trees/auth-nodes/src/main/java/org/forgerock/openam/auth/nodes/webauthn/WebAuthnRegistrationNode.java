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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.ERROR_MESSAGE;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.WEB_AUTHENTICATION_DOM_EXCEPTION;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationResponse;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.sm.validation.SecretIdValidator;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A web authentication registration node. Uses client side javascript to interact with the browser and negotiate
 * Registration according to the webauthn spec. Results in credentials, public key and other data which can be used
 * to authenticate.
 */
@Node.Metadata(outcomeProvider = WebAuthnRegistrationNode.OutcomeProvider.class,
        configClass = WebAuthnRegistrationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class WebAuthnRegistrationNode extends AbstractWebAuthnNode {

    private static final String BUNDLE = WebAuthnRegistrationNode.class.getName();
    private static final String REGISTRATION_SCRIPT = RESOURCE_LOCATION + "webauthn-client-registration-script.js";

    private static final String DEFAULT_DEVICE_NAME_KEY = "newDeviceName";

    private final Config config;
    private final Realm realm;
    private RegisterFlow registerFlow;
    private final WebAuthnDeviceJsonUtils webAuthnDeviceJsonUtils;
    private final IdentityUtils identityUtils;
    private final CoreWrapper coreWrapper;

    private String registeredDeviceUuid;
    private String registeredAaguid;
    private AttestationType attestationType;
    private String deviceName;
    private String credentialId;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The relying party display name. NOT to be confused the RpID (relying party id) or the valid origins.
         *
         * @return the display friendly name of the RP.
         */
        @Attribute(order = 10, validators = RequiredValueValidator.class)
        default String relyingPartyName() {
            return "ForgeRock";
        }

        /**
         * Sets the relying party domain.
         *
         * @return the relying party domain.
         */
        @Attribute(order = 20)
        Optional<String> relyingPartyDomain();

        /**
         * Sets the origins from which this node accepts requests.
         *
         * @return the set of valid origins
         */
        @Attribute(order = 25)
        default Set<String> origins() {
            return Collections.emptySet();
        }

        /**
         * If required, successful registration requires user verification.
         *
         * @return is user verification required.
         */
        @Attribute(order = 30)
        default UserVerificationRequirement userVerificationRequirement() {
            return UserVerificationRequirement.PREFERRED;
        }

        /**
         * Preferred mode of attestation.
         *
         * @return The attestation preference.
         */
        @Attribute(order = 40)
        default AttestationPreference attestationPreference() {
            return AttestationPreference.NONE;
        }

        /**
         * The set of accepted signing algorithms.
         *
         * @return the set of signing algorithms.
         */
        @Attribute(order = 50)
        default Set<CoseAlgorithm> acceptedSigningAlgorithms() {
            Set<CoseAlgorithm> algorithms = new HashSet<>();
            algorithms.add(CoseAlgorithm.ES256);
            algorithms.add(CoseAlgorithm.RS256);
            return algorithms;
        }

        /**
         * Specifies the authenticator attachment type.
         *
         * @return the authenticator attachment type.
         */
        @Attribute(order = 60)
        default AuthenticatorAttachment authenticatorAttachment() {
            return AuthenticatorAttachment.UNSPECIFIED;
        }

        /**
         * Specify the alias of the trust store used to verify attestation data in the case of CA attestation.
         *
         * @return the alias of the trust store.
         */
        @Attribute(order = 65, validators = SecretIdValidator.class)
        default Optional<String> trustStoreAlias() {
            return Optional.of("trustalias");
        }

        /**
         * Whether to enforce the checking of revocation entries from certificates. If this is set to true,
         * then any attestation certificate's trust chain MUST have a CRL or OCSP entry that can be verified by
         * AM during processing to not have a revocation entry for the certificate under verification.
         * <p>
         * If this is is set to false, then presented certificates will not be checked for revocation.
         *
         * @return whether to enforce the revocation check.
         */
        @Attribute(order = 68)
        default boolean enforceRevocationCheck() {
            return false;
        }

        /**
         * Specifies the timeout of the registration node.
         *
         * @return the length of time to wait for a device to register.
         */
        @Attribute(order = 70)
        default int timeout() {
            return 60;
        }

        /**
         * Specifies whether only a single credential can be registered by auth device.
         *
         * @return true if limited, false otherwise
         */
        @Attribute(order = 80)
        default boolean excludeCredentials() {
            return false;
        }

        /**
         * Specifies whether to generate recovery codes and store them in the shared state, and
         * the device profile if this node is responsible for device storage.
         *
         * @return true if the codes are to be generated
         */
        @Attribute(order = 90)
        default boolean generateRecoveryCodes() {
            return true;
        }

        /**
         * Specifies whether if the node completes successfully to place the attestation data provided by
         * the device into the transient state for analysis by later nodes.
         *
         * @return true if the attestation data is to be stored
         */
        @Attribute(order = 100)
        default boolean storeAttestationDataInTransientState() {
            return false;
        }

        /**
         * Specifies whether if the node completes successfully to save the device profile directly or
         * to postpone the device profile storage and place the information into the transient state for
         * storage by later nodes.
         *
         * @return true if the device profile is to be postponed.
         */
        @Attribute(order = 110)
        default boolean postponeDeviceProfileStorage() {
            return false;
        }

        /**
         * Specify that the authenticators used during registration must support resident credentials
         * (storing the username on the device).
         *
         * @return {@literal true} if resident key support is required.
         */
        @Attribute(order = 120)
        default boolean requiresResidentKey() {
            return false;
        }

        /**
         * Specify the shared state attribute from which to read the user's Display Name.
         *
         * @return the shared state attribute from which to read the user's display name.
         */
        @Attribute(order = 130)
        Optional<String> displayNameSharedState();

        /**
         * Specify whether to return the challenge as a script or just as metadata.
         *
         * @return {@literal true} if return as a script.
         */
        @Attribute(order = 140)
        default boolean asScript() {
            return true;
        }
    }

    /**
     * The constructor.
     *
     * @param config                  node config.
     * @param realm                   the realm.
     * @param registerFlow            registration ceremony flow.
     * @param clientScriptUtilities   utilities for handling the client side scripts.
     * @param webAuthnProfileManager  managers user's device profiles.
     * @param secureRandom            instance of the secure random generator
     * @param recoveryCodeGenerator   instance of the recovery code generator
     * @param webAuthnDeviceJsonUtils Helper to convert device to json
     * @param identityUtils           A {@code IdentityUtils} instance.
     * @param coreWrapper             The {@code CoreWrapper} instance.
     */
    @Inject
    public WebAuthnRegistrationNode(@Assisted Config config, @Assisted Realm realm, RegisterFlow registerFlow,
            ClientScriptUtilities clientScriptUtilities,
            UserWebAuthnDeviceProfileManager webAuthnProfileManager,
            SecureRandom secureRandom, RecoveryCodeGenerator recoveryCodeGenerator,
            WebAuthnDeviceJsonUtils webAuthnDeviceJsonUtils,
            IdentityUtils identityUtils, CoreWrapper coreWrapper) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom, recoveryCodeGenerator);

        this.config = config;
        this.realm = realm;
        this.registerFlow = registerFlow;
        this.webAuthnDeviceJsonUtils = webAuthnDeviceJsonUtils;
        this.identityUtils = identityUtils;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("WebAuthnRegistrationNode started");
        String username = context.sharedState.get(USERNAME).asString();
        byte[] challengeBytes = getChallenge(context);
        String registrationScript = clientScriptUtilities.getScriptAsString(REGISTRATION_SCRIPT);

        Optional<String> result = context.getCallback(HiddenValueCallback.class)
                .map(HiddenValueCallback::getValue)
                .filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));

        if (result.isPresent()) {
            logger.debug("processing user response");

            //if the browser does not support us
            if (result.get().equals(UNSUPPORTED)) {
                logger.warn("User's user agent does not support WebAuthn");
                return Action.goTo(UNSUPPORTED_OUTCOME_ID).build();
            }

            //if there was a DOM error
            if (result.get().startsWith(ERROR_MESSAGE)) {
                WebAuthnDomException exception = parseError(result.get());
                logger.error("An error was reported by the DOM.");
                return Action.goTo(ERROR_OUTCOME_ID).replaceSharedState(
                        context.sharedState.copy().put(WEB_AUTHENTICATION_DOM_EXCEPTION, exception.toString()))
                        .build();
            }

            //parse script, perform attestation
            ClientRegistrationScriptResponse response =
                    clientScriptUtilities.parseClientRegistrationResponse(result.get());
            AttestationResponse attestationResponse;

            //retrieve the domain from the passing in Origin if not specified
            String rpId = getDomain(config.relyingPartyDomain(), context.request.headers.get("origin"),
                    context.request.serverUrl);


            try {
                attestationResponse = registerFlow.accept(realm, response.getClientData(),
                        response.getAttestationData(), challengeBytes, rpId, response.getCredentialId(),
                        getPermittedOrigins(config.origins(), context), config);
                registeredAaguid = Hex.encodeHexString(
                        attestationResponse.getAttestationObject().authData.attestedCredentialData.aaguid);
                attestationType = attestationResponse.getAttestationType();
                logger.debug("registeredAaguid: {}, attestationType: {}", registeredAaguid, attestationType);
            } catch (WebAuthnRegistrationException wre) {
                //we can build more outcomes from the various Exception types when/if required
                logger.error(wre.getMessage(), wre);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

            Action.ActionBuilder outcomeBuilder =
                    Action.goTo(SUCCESS_OUTCOME_ID).addNodeType(context, WEB_AUTHN_AUTH_TYPE);

            JsonValue transientState = context.transientState.copy();

            //store data for later analysis, generate device if appropriate, return with success if possible
            try {
                if (config.storeAttestationDataInTransientState()) {
                    transientState.put(WEB_AUTHN_STATE_DATA, result.get());
                    transientState.put(WEB_AUTHN_ATTESTATION_TYPE, attestationType.toString());
                }

                return generateDevice(outcomeBuilder, response, attestationResponse.getAttestationObject(), username,
                        transientState, context);
            } catch (DevicePersistenceException | CodeException e) {
                logger.error("Unable to create device profile from response data.", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

        } else {
            logger.debug("sending callbacks to user for completion");
            Optional<AMIdentity> user = getAMIdentity(context, identityUtils, coreWrapper);
            if (user.isEmpty()) {
                logger.warn("getIdentity: Unable to find user {}", username);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
            return getCallbacksForWebAuthnInteraction(config.asScript(), registrationScript,
                    getScriptContext(challengeBytes, user.get(), config.relyingPartyDomain().orElse(null), context),
                    context);
        }
    }

    private Action generateDevice(Action.ActionBuilder outcomeBuilder, ClientScriptResponse response,
            AttestationObject attestationObject, String username, JsonValue transientState,
            TreeContext context)
            throws DevicePersistenceException, CodeException {
        logger.debug("getting user device data");

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE,
                WebAuthnRegistrationNode.OutcomeProvider.class.getClassLoader());
        WebAuthnDeviceSettings device = webAuthnProfileManager.createDeviceProfile(response.getCredentialId(),
                attestationObject.authData.attestedCredentialData.publicKey,
                attestationObject.authData.attestedCredentialData.algorithm,
                bundle.getString(DEFAULT_DEVICE_NAME_KEY));

        registeredDeviceUuid = device.getUUID();
        credentialId = device.getCredentialId();
        deviceName = device.getDeviceName();

        logger.debug("registeredDeviceUuid: {}, credentialId: {}, deviceName: {}",
                registeredDeviceUuid, credentialId, deviceName);

        if (config.postponeDeviceProfileStorage()) {
            logger.debug("storing device data in shared state");
            try {
                JsonValue deviceJson = webAuthnDeviceJsonUtils.toJsonValue(device);
                transientState.put(WEB_AUTHN_DEVICE_DATA, deviceJson);
            } catch (IOException e) {
                throw new DevicePersistenceException("Unable to store device in shared state", e);
            }
        } else {
            Optional<AMIdentity> user = getAMIdentity(context, identityUtils, coreWrapper);
            if (user.isEmpty()) {
                logger.debug("getIdentity: Unable to find user {}", username);
                throw new DevicePersistenceException("Unable to store device data");
            }
            setRecoveryCodesOnDevice(config.generateRecoveryCodes(), device, transientState);
            logger.debug("storing device data in profile");
            webAuthnProfileManager.saveDeviceProfile(user.get().getName(),
                    DNMapper.orgNameToRealmName(user.get().getRealm()), device);
        }

        outcomeBuilder.replaceTransientState(transientState);
        logger.debug("returning with success outcome");
        return outcomeBuilder.build();
    }

    private JsonValue getScriptContext(byte[] challengeBytes, AMIdentity user, String configuredRpId,
            TreeContext context)
            throws NodeProcessException {
        JsonValue scriptContext = json(object());
        scriptContext.put("_action", "webauthn_registration");
        scriptContext.put("challenge", Arrays.toString(challengeBytes));
        scriptContext.put("attestationPreference", config.attestationPreference().getValue());
        scriptContext.put("userName", user.getName());
        scriptContext.put("userId", EncodingUtilities.base64UrlEncode(user.getName()));
        scriptContext.put("relyingPartyName", config.relyingPartyName());
        scriptContext.put("authenticatorSelection", getAuthenticatorSelection());
        scriptContext.put("_authenticatorSelection", getAuthenticatorSelectionAsJson());
        scriptContext.put("pubKeyCredParams", clientScriptUtilities
                .getPubKeyCredParams(config.acceptedSigningAlgorithms()));
        scriptContext.put("_pubKeyCredParams", clientScriptUtilities
                .getPubKeyCredParamsAsJson(config.acceptedSigningAlgorithms()));
        scriptContext.put("timeout", String.valueOf(config.timeout() * 1000));
        scriptContext.put("excludeCredentials", getExcludeCredentials(user));
        scriptContext.put("_excludeCredentials", getExcludeCredentialsAsJson(user));

        if (config.displayNameSharedState().isPresent()) {
            String displayName = context.sharedState.get(config.displayNameSharedState().get()).asString();
            scriptContext.put("displayName", StringUtils.isNotEmpty(displayName) ? displayName : user.getName());
        } else {
            scriptContext.put("displayName", user.getName());
        }

        StringBuilder sb = new StringBuilder();
        if (configuredRpId != null) {
            sb.append("id: \"").append(configuredRpId).append("\",");
        } else {
            configuredRpId = "";
        }
        scriptContext.put("relyingPartyId", sb.toString());
        scriptContext.put("_relyingPartyId", configuredRpId);

        return scriptContext;
    }

    private String getExcludeCredentials(AMIdentity user) throws NodeProcessException {

        List<WebAuthnDeviceSettings> devices;
        try {
            devices = webAuthnProfileManager.getDeviceProfiles(user.getName(), user.getRealm());
        } catch (DevicePersistenceException e) {
            logger.warn("unable to read existing device profiles");
            throw new NodeProcessException("Unable to read existing device profiles");
        }

        if (config.excludeCredentials() && !CollectionUtils.isEmpty(devices)) {
            return clientScriptUtilities.getDevicesAsJavaScript(devices);
        }
        return "";
    }

    private JsonValue getExcludeCredentialsAsJson(AMIdentity user) throws NodeProcessException {

        List<WebAuthnDeviceSettings> devices;
        try {
            devices = webAuthnProfileManager.getDeviceProfiles(user.getName(), user.getRealm());
        } catch (DevicePersistenceException e) {
            logger.warn("unable to read existing device profiles");
            throw new NodeProcessException("Unable to read existing device profiles");
        }

        if (config.excludeCredentials() && !CollectionUtils.isEmpty(devices)) {
            return clientScriptUtilities.getDevicesAsJson(devices);
        }
        return json(array());
    }


    private String getAuthenticatorSelection() throws NodeProcessException {

        try {
            JsonValue jsonValue = getAuthenticatorSelectionAsJson();
            return new String(Json.writeJson(jsonValue));
        } catch (IOException ioe) {
            logger.error("Internal error generating JSON string. Aborting operation.");
            throw new NodeProcessException("Unable to generate JSON for webauthn script");
        }
    }

    private JsonValue getAuthenticatorSelectionAsJson() {
        JsonValue jsonValue = json(
                object(field("userVerification", config.userVerificationRequirement().getValue())));
        if (config.authenticatorAttachment() != AuthenticatorAttachment.UNSPECIFIED) {
            jsonValue.put("authenticatorAttachment", config.authenticatorAttachment().getValue());
        }
        if (config.requiresResidentKey()) {
            jsonValue.put("requireResidentKey", true);
        }

        return jsonValue;
    }


    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnRegistrationNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(UNSUPPORTED_OUTCOME_ID, bundle.getString(UNSUPPORTED_OUTCOME_ID)),
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)),
                    new Outcome(ERROR_OUTCOME_ID, bundle.getString(ERROR_OUTCOME_ID)));
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(
                field("registeredDeviceUuid", registeredDeviceUuid),
                field("registeredAaguid", registeredAaguid),
                field("attestationType", attestationType),
                field("credentialId", credentialId),
                field("deviceName", deviceName)));
    }
}
