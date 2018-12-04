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

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.ERROR_MESSAGE;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.WEB_AUTHENTICATION_DOM_EXCEPTION;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeDisplayNode;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.tools.objects.MapFormat;

/**
 * A web authentication registration node. Uses client side javascript to interact with the browser and negotiate
 * Registration according to the webauthn spec. Results in credentials, public key and other data which can be used
 * to authenticate.
 */
@Node.Metadata(outcomeProvider = WebAuthnRegistrationNode.OutcomeProvider.class,
        configClass = WebAuthnRegistrationNode.Config.class)
public class WebAuthnRegistrationNode extends AbstractWebAuthnNode {

    private static final String BUNDLE = RESOURCE_LOCATION + "WebAuthnRegistrationNode";
    private static final String REGISTRATION_SCRIPT = RESOURCE_LOCATION + "webauthn-client-registration-script.js";

    private static final String DEFAULT_DEVICE_NAME_KEY = "newDeviceName";
    private static final int NUM_RECOVERY_CODES = 10;

    private final Config config;
    private RegisterFlow registerFlow;
    private RecoveryCodeGenerator recoveryCodeGenerator;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The relying party display name. NOT to be confused the relying party id, RpID or the relying party domain.
         *
         * @return the display friendly name of the RP.
         */
        @Attribute(order = 10)
        default String relyingPartyName() {
            return "ForgeRock";
        }

        /**
         * Sets the domain. If left empty, AM will attempt a best-guess.
         *
         * @return the relying party domain.
         */
        @Attribute(order = 20)
        Optional<String> relyingPartyDomain();

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
         * Specifies whether the user is presented with recovery codes after the device has been successfully
         * registered.
         *
         * @return true if the codes are to be presented
         */
        @Attribute(order = 90)
        default boolean generateRecoveryCodes() {
            return true;
        }
    }

    /**
     * The constructor.
     *
     * @param config node config.
     * @param registerFlow registration ceremony flow.
     * @param clientScriptUtilities utilities for handling the client side scripts.
     * @param webAuthnProfileManager managers user's device profiles.
     * @param secureRandom instance of the secure random generator
     * @param recoveryCodeGenerator instance of the recovery code generator
     */
    @Inject
    public WebAuthnRegistrationNode(@Assisted Config config, RegisterFlow registerFlow,
                                    ClientScriptUtilities clientScriptUtilities,
                                    UserWebAuthnDeviceProfileManager webAuthnProfileManager,
                                    SecureRandom secureRandom, RecoveryCodeGenerator recoveryCodeGenerator) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom);

        this.config = config;
        this.registerFlow = registerFlow;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("WebAuthnRegistrationNode started");
        AMIdentity user = IdUtils.getIdentity(context.sharedState.get(USERNAME).asString(),
                    context.sharedState.get(REALM).asString());
        byte[] challengeBytes = getChallenge(context);
        String rpId = getDomain(context.request.serverUrl, config.relyingPartyDomain());
        String registrationScript = clientScriptUtilities.getScriptAsString(REGISTRATION_SCRIPT);
        registrationScript = MapFormat.format(registrationScript, getScriptContext(challengeBytes, rpId, user));

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
                        context.sharedState.copy().put(WEB_AUTHENTICATION_DOM_EXCEPTION, exception.toString())).build();
            }

            //parse script, perform attestation
            ClientScriptResponse response = clientScriptUtilities.parseClientRegistrationResponse(result.get());
            AttestationObject attestationObject;

            try {
                attestationObject = registerFlow.accept(response.getClientData(),
                        response.getAttestationData(), challengeBytes, rpId, config.userVerificationRequirement(),
                        response.getCredentialId(), context.request.serverUrl, config.attestationPreference());
            } catch (WebAuthnRegistrationException wre) {
                //we can build more outcomes from the various Exception types when/if required
                logger.error(wre.getMessage(), wre);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

            //generate device, return with success if possible
            try {
                return generateDevice(response, attestationObject, user, context);
            } catch (DevicePersistenceException | CodeException e) {
                logger.error("Unable to create device profile from response data.", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

        } else {
            logger.debug("sending callbacks to user for completion");
            return getCallbacksForWebAuthnInteraction(registrationScript, context);
        }
    }

    private Action generateDevice(ClientScriptResponse response, AttestationObject attestationObject,
                                                  AMIdentity user, TreeContext context)
            throws DevicePersistenceException, CodeException {
        logger.debug("getting user device data");

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE,
                WebAuthnRegistrationNode.OutcomeProvider.class.getClassLoader());
        WebAuthnDeviceSettings device = webAuthnProfileManager.createDeviceProfile(response.getCredentialId(),
                attestationObject.authData.attestedCredentialData.publicKey,
                attestationObject.authData.attestedCredentialData.algorithm,
                bundle.getString(DEFAULT_DEVICE_NAME_KEY));

        Action.ActionBuilder outcomeBuilder = Action.goTo(SUCCESS_OUTCOME_ID).addNodeType(context, WEB_AUTHN_AUTH_TYPE);

        //generate recovery codes
        if (config.generateRecoveryCodes()) {
            List<String> codes = recoveryCodeGenerator.generateCodes(NUM_RECOVERY_CODES, Alphabet.ALPHANUMERIC,
                    false);
            device.setRecoveryCodes(codes);
            outcomeBuilder.replaceTransientState(context.transientState.copy()
                            .put(RecoveryCodeDisplayNode.RECOVERY_CODE_KEY, codes)
                            .put(RecoveryCodeDisplayNode.RECOVERY_CODE_DEVICE_NAME, device.getDeviceName()))
                    .build();
        }

        webAuthnProfileManager.saveDeviceProfile(user.getName(), DNMapper.orgNameToRealmName(user.getRealm()),
                device);

        logger.debug("returning with success outcome");
        return outcomeBuilder.build();
    }

    private Map<String, String> getScriptContext(byte[] challengeBytes, String rpId, AMIdentity user)
            throws NodeProcessException {
        Map<String, String> scriptContext = new HashMap<>();
        scriptContext.put("challenge", Arrays.toString(challengeBytes));
        scriptContext.put("relyingPartyId", rpId);
        scriptContext.put("attestationPreference", config.attestationPreference().getValue());
        scriptContext.put("userName", user.getName());
        scriptContext.put("userId", EncodingUtilities.getURLSafeEncodedUserName(user.getUniversalId()));
        scriptContext.put("relyingPartyName", config.relyingPartyName());
        scriptContext.put("authenticatorSelection", getAuthenticatorSelection());
        scriptContext.put("pubKeyCredParams", clientScriptUtilities
                .getPubKeyCredParams(config.acceptedSigningAlgorithms()));
        scriptContext.put("timeout", String.valueOf(config.timeout() * 1000));
        scriptContext.put("excludeCredentials", getExcludeCredentials(user));
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

    private String getAuthenticatorSelection() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("userVerification: ")
                .append("\"")
                .append(config.userVerificationRequirement().getValue())
                .append("\"");
        if (config.authenticatorAttachment() != AuthenticatorAttachment.UNSPECIFIED) {
            sb.append(",\n");
            sb.append("authenticatorAttachment: ")
                    .append("\"")
                    .append(config.authenticatorAttachment().getValue())
                    .append("\"");
        }
        sb.append("\n");
        sb.append("}");
        return sb.toString();
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
}
