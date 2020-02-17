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
 * Copyright 2018-2020 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.ERROR_MESSAGE;
import static org.forgerock.openam.auth.nodes.webauthn.WebAuthnDomException.WEB_AUTHENTICATION_DOM_EXCEPTION;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
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
 * A web authentication, authentication node. Uses client side javascript to interact with the browser and negotiate
 * authentication according to the webauthn spec. If the credentials, public key encrypted challenge response and
 * other details are valid, the outcome of the node is 'true'.
 */
@Node.Metadata(outcomeProvider = WebAuthnAuthenticationNode.OutcomeProvider.class,
        configClass = WebAuthnAuthenticationNode.Config.class)
public class WebAuthnAuthenticationNode extends AbstractWebAuthnNode {

    private static final String BUNDLE = RESOURCE_LOCATION + "WebAuthnAuthenticationNode";
    private static final String AUTH_SCRIPT = RESOURCE_LOCATION + "webauthn-client-auth-script.js";

    private static final String IS_RECOVERY_CODE_ALLOWED = "isRecoveryCodeAllowed";

    private static final String NO_DEVICE_OUTCOME_ID = "noDevice";
    private static final String RECOVERY_CODE_OUTCOME_ID = "recoveryCode";

    private static final int RECOVERY_PRESSED = 0;
    private static final int RECOVERY_NOT_PRESSED = 100;

    private final Config config;

    private AuthenticationFlow authenticationFlow;

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
        Optional<String> relyingPartyDomain();

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
    }

    /**
     * The constructor.
     *
     * @param config the node's config.
     * @param authenticationFlow the authentication flow as executable steps.
     * @param clientScriptUtilities utilities for handling the client side script.
     * @param webAuthnProfileManager management of user's devices
     * @param secureRandom instance of the secure random generator
     */
    @Inject
    public WebAuthnAuthenticationNode(@Assisted Config config, AuthenticationFlow authenticationFlow,
                                      ClientScriptUtilities clientScriptUtilities,
                                      UserWebAuthnDeviceProfileManager webAuthnProfileManager,
                                      SecureRandom secureRandom) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom);

        this.config = config;
        this.authenticationFlow = authenticationFlow;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("WebAuthnAuthenticationNode started");
        byte[] challengeBytes = getChallenge(context);
        String rpId = getDomain(context.request.serverUrl, config.relyingPartyDomain());

        List<WebAuthnDeviceSettings> devices;

        try {
            logger.debug("getting user data and device data");
            AMIdentity user = IdUtils.getIdentity(context.sharedState.get(USERNAME).asString(),
                    context.sharedState.get(REALM).asString());
            devices = webAuthnProfileManager.getDeviceProfiles(user.getName(),
                    DNMapper.orgNameToRealmName(user.getRealm()));
            if (devices.isEmpty()) {
                return Action.goTo(NO_DEVICE_OUTCOME_ID).build();
            }
        } catch (DevicePersistenceException e) {
            logger.error("Unable to retrieve user's device profile information.", e);
            return Action.goTo(FAILURE_OUTCOME_ID).build();
        }

        String authScript = clientScriptUtilities.getScriptAsString(AUTH_SCRIPT);
        Map<String, String> scriptContext = new HashMap<>();
        scriptContext.put("challenge", Arrays.toString(challengeBytes));
        scriptContext.put("acceptableCredentials", clientScriptUtilities.getDevicesAsJavaScript(devices));
        scriptContext.put("timeout", String.valueOf(config.timeout() * 1000));
        scriptContext.put("relyingPartyId", rpId);
        authScript = MapFormat.format(authScript, scriptContext);

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

            ClientScriptResponse response = clientScriptUtilities.parseClientAuthenticationResponse(result.get());
            WebAuthnDeviceSettings device = getEntry(response.getCredentialId(), devices);
            if (authenticationFlow.accept(response.getClientData(), response.getAuthenticatorData(),
                    response.getSignature(), challengeBytes, rpId, device, context.request.serverUrl,
                    config.userVerificationRequirement())) {
                logger.debug("returning with success outcome");

                return Action.goTo(SUCCESS_OUTCOME_ID)
                        .addNodeType(context, WEB_AUTHN_AUTH_TYPE)
                        .build();
            } else {
                logger.debug("returning with failure outcome");
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
        } else {
            logger.debug("sending callbacks to user for completion");
            Callback[] additionalCallbacks = getAdditionalCallbacks(context.request.locales);
            return getCallbacksForWebAuthnInteraction(authScript, context, additionalCallbacks);
        }
    }

    private Callback[] getAdditionalCallbacks(PreferredLocales locales) {

        if (config.isRecoveryCodeAllowed()) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnAuthenticationNode.OutcomeProvider.class.getClassLoader());

            ConfirmationCallback confirmationCallback =
                    new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                            new String[] { bundle.getString("recoveryCodeButton") }, RECOVERY_PRESSED);
            confirmationCallback.setSelectedIndex(RECOVERY_NOT_PRESSED);

            return new Callback[] { confirmationCallback };
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
        logger.warn("No registered device found for credentialId: {}.", credentialId);
        throw new NodeProcessException("Unable to locate registered device.");
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

}
