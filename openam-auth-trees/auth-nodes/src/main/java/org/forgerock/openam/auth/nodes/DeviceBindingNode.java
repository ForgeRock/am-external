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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptySet;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;
import org.forgerock.openam.authentication.callbacks.DeviceBindingCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingJsonUtils;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingSettings;
import org.forgerock.openam.core.rest.devices.services.binding.AndroidKeyAttestationService;
import org.forgerock.openam.core.rest.devices.services.binding.attestation.KeyAttestationException;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.validation.PositiveIntegerValidator;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A Device Binding Node. Ensuring that the user is in possession of a trusted device.
 */
@Node.Metadata(outcomeProvider = DeviceBindingNode.OutcomeProvider.class,
        configClass = DeviceBindingNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class DeviceBindingNode implements Node, DeviceBinding {

    private static final String BUNDLE = DeviceBindingNode.class.getName();
    private static final String CHALLENGE = DeviceBindingNode.class.getSimpleName() + ".CHALLENGE";
    private static final String ATTESTATION = DeviceBindingNode.class.getSimpleName() + ".ATTESTATION";
    private static final String DEVICE_NAME = "deviceName";
    private static final Logger logger = LoggerFactory.getLogger(DeviceBindingNode.class);
    static final String SUCCESS_OUTCOME_ID = "success";
    static final String FAILURE_OUTCOME_ID = "failure";
    static final String MAX_SAVED_DEVICES = "maxSavedDevices";
    static final String EXCEED_DEVICE_LIMIT_OUTCOME_ID = "exceedDeviceLimit";
    static final String CLIENT_ERROR_OUTCOMES = "clientErrorOutcomes";
    static final String PLATFORM = "platform";
    static final String IOS = "ios";
    private final Config config;
    private final NodeUserIdentityProvider identityProvider;
    private final DeviceBindingManager deviceBindingManager;
    private final DeviceBindingJsonUtils deviceBindingJsonUtils;
    private final LocaleSelector localeSelector;
    private final AndroidKeyAttestationService androidKeyAttestationService;
    private final Realm realm;

    private static final List<String> DEFAULT_CLIENT_ERROR_OUTCOMES = List.of("Unsupported", "Abort", "Timeout");

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The authentication type used to authenticate with the authenticator.
         * Default to {@link AuthenticationType#BIOMETRIC_ALLOW_FALLBACK}
         *
         * @return The client authentication type
         */
        @Attribute(order = 100)
        default AuthenticationType authenticationType() {
            return AuthenticationType.BIOMETRIC_ALLOW_FALLBACK;
        }

        /**
         * The android package name and iOS bundle id that issue the binding request.
         *
         * @return The android package name or iOS bundle id
         */
        @Attribute(order = 150, validators = {RequiredValueValidator.class})
        default Set<String> applicationIds() {
            return emptySet();
        }

        /**
         * The title to be displayed for the biometric prompt.
         *
         * @return The title.
         */
        @Attribute(order = 200)
        default Map<Locale, String> title() {
            return Collections.emptyMap();
        }

        /**
         * The subtitle to be displayed for the biometric prompt.
         *
         * @return The subtitle.
         */
        @Attribute(order = 300)
        default Map<Locale, String> subtitle() {
            return Collections.emptyMap();
        }

        /**
         * The description to be displayed for the biometric prompt.
         *
         * @return The description.
         */
        @Attribute(order = 400)
        default Map<Locale, String> description() {
            return Collections.emptyMap();
        }

        /**
         * Maximum stored Device quantity.
         *
         * @return the maximum stored Device quantity
         */
        @Attribute(order = 500, requiredValue = true, validators = {PositiveIntegerValidator.class})
        default int maxSavedDevices() {
            return 0;
        }

        /**
         * Specifies the timeout of the biometric authentication.
         *
         * @return the length of time to wait for a device to bind.
         */
        @Attribute(order = 600)
        default int timeout() {
            return 60;
        }

        /**
         * To perform Android Attestation.
         *
         * @return true to perform Android Attestation, otherwise skip the attestation
         */
        @Attribute(order = 650)
        default boolean attestation() {
            return false;
        }

        /**
         * Specifies whether if the node completes successfully to save the device profile directly or
         * to postpone the device profile storage and place the information into the transient state for
         * storage by later nodes.
         *
         * @return true if the device profile is to be postponed.
         */
        @Attribute(order = 680)
        default boolean postponeDeviceProfileStorage() {
            return false;
        }

        /**
         * Client error outcomes.
         *
         * @return The client error outcomes
         */
        @Attribute(order = 700)
        default List<String> clientErrorOutcomes() {
            return DEFAULT_CLIENT_ERROR_OUTCOMES;
        }
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param config The Node configuration
     * @param realm The current realm
     * @param identityProvider The NodeUserIdentityProvider
     * @param androidKeyAttestationService Key Attestation Service to perform Android Key Attestation
     * @param deviceBindingManager Instance of DeviceBindingManager
     * @param deviceBindingJsonUtils Instance of the utils to help convert device to json
     * @param localeSelector a LocaleSelector for choosing the correct message to display
     */
    @Inject
    public DeviceBindingNode(@Assisted Config config, @Assisted Realm realm,
            NodeUserIdentityProvider identityProvider,
            AndroidKeyAttestationService androidKeyAttestationService,
            DeviceBindingManager deviceBindingManager,
            DeviceBindingJsonUtils deviceBindingJsonUtils,
            LocaleSelector localeSelector) {
        this.config = config;
        this.realm = realm;
        this.androidKeyAttestationService = androidKeyAttestationService;
        this.deviceBindingManager = deviceBindingManager;
        this.deviceBindingJsonUtils = deviceBindingJsonUtils;
        this.localeSelector = localeSelector;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        Optional<DeviceBindingCallback> deviceBindingCallback = context.getCallback(DeviceBindingCallback.class);

        NodeState nodeState = context.getStateFor(this);

        //Make sure we have a valid user in the context.
        Optional<AMIdentity> userIdentity = identityProvider.getAMIdentity(context.universalId,
                context.getStateFor(this));
        try {
            if (userIdentity.isEmpty()
                    || !userIdentity.get().isExists()
                    || !userIdentity.get().isActive()) {
                throw new NodeProcessException("User does not exist or inactive");
            }
        } catch (Exception e) {
            throw new NodeProcessException("Failed to lookup user", e);
        }

        String username = userIdentity.get().getName();
        String userRealm = userIdentity.get().getRealm();

        //0 will consider unlimited, check if we can reach device binding limit
        if (config.maxSavedDevices() > 0) {
            try {
                if (deviceBindingManager.getDeviceProfiles(username, userRealm).size() >= config.maxSavedDevices()) {
                    return Action.goTo(EXCEED_DEVICE_LIMIT_OUTCOME_ID).build();
                }
            } catch (DevicePersistenceException e) {
                logger.error("An error occurred while retrieving user's device", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
        }

        if (deviceBindingCallback.isPresent()
                && StringUtils.isNotEmpty(deviceBindingCallback.get().getJws())
                && StringUtils.isNotEmpty(deviceBindingCallback.get().getDeviceId())) {

            SignedJwt signedJwt;
            try {
                signedJwt = new JwtReconstruction().reconstructJwt(deviceBindingCallback.get().getJws(),
                        SignedJwt.class);
                validateClaim(nodeState, signedJwt);
                JWK jwk = signedJwt.getHeader().getJsonWebKey();
                validateSignature(signedJwt, jwk);
                validateKeyAttestation(nodeState, signedJwt);
                persist(context, nodeState, deviceBindingCallback.get(), username, userRealm, jwk);
            } catch (Exception e) {
                logger.warn("Device Binding failed:", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

            return Action.goTo(SUCCESS_OUTCOME_ID)
                    .addNodeType(context, "Binding").build();
        } else if (deviceBindingCallback.isPresent()
                && StringUtils.isNotEmpty(deviceBindingCallback.get().getClientError())) {
            return Action.goTo(deviceBindingCallback.get().getClientError()).build();
        } else {
            logger.debug("Send DeviceProfileCallback to client.");
            return getCallback(context, nodeState, userIdentity.get(), username);
        }
    }

    private DeviceSettings persist(TreeContext context, NodeState nodeState,
            DeviceBindingCallback deviceBindingCallback, String username,
            String realm, JWK jwk) throws DevicePersistenceException, IOException {
        //Get device name, if empty use default
        String deviceName = deviceBindingCallback.getDeviceName();
        if (StringUtils.isEmpty(deviceName)) {
            ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE,
                    OutcomeProvider.class.getClassLoader());
            deviceName = bundle.getString(DEVICE_NAME);
        }

        //Create Device binding
        DeviceBindingSettings device = deviceBindingManager.createDeviceProfile(
                jwk, deviceName, deviceBindingCallback.getDeviceId());

        if (config.postponeDeviceProfileStorage()) {
            //persist to transient state
            nodeState.putTransient(DEVICE, deviceBindingJsonUtils.toJsonValue(device));
        } else {
            //persist to storage
            deviceBindingManager.saveDeviceProfile(username, realm, device);
        }
        return device;
    }

    private void validateClaim(NodeState nodeState, SignedJwt signedJwt) throws NodeProcessException {
        //Validate Claim
        String challenge = getContextValue(nodeState, CHALLENGE);
        validateClaim(signedJwt, challenge, config.applicationIds());
    }

    private void validateKeyAttestation(NodeState nodeState, SignedJwt signedJwt) throws NodeProcessException,
            KeyAttestationException {

        if (signedJwt.getClaimsSet().isDefined(PLATFORM)
                && signedJwt.getClaimsSet().get(PLATFORM).asString().equals(IOS)) {
            return;
        }

        if (config.attestation()) {
            String challenge = getContextValue(nodeState, CHALLENGE);
            X509Certificate[] chain = toCertificateChain(signedJwt);
            if (!androidKeyAttestationService.verify(realm, chain,
                    Base64.getDecoder().decode(challenge))) {
                throw new KeyAttestationException("Key Attestation Failed.");
            }
            nodeState.putTransient(ATTESTATION,
                    androidKeyAttestationService.asJson(realm, chain, Base64.getDecoder().decode(challenge)));
        }
    }

    private X509Certificate[] toCertificateChain(SignedJwt jwt) throws KeyAttestationException {
        try {
            return CertificateUtils.getCertPathFromJwkX5c(jwt.getHeader().getJsonWebKey())
                    .getCertificates().stream()
                    .map(certificate -> (X509Certificate) certificate)
                    .toArray(X509Certificate[]::new);
        } catch (CertificateException e) {
            throw new KeyAttestationException("Failed to extract certificate from JWT", e);
        }
    }

    private Action getCallback(TreeContext context, NodeState nodeState, AMIdentity identity, String contextUsername) {
        String challenge = createRandomBytes();
        nodeState.putShared(CHALLENGE, challenge);
        String username = "";
        try {
            //We need to provide fallback for the username, the getAttribute may not return the username.
            username = getFirstItem(identity.getAttribute(IdConstants.USERNAME), contextUsername);
        } catch (IdRepoException | SSOException e) {
            logger.warn("Username attribute not found.", e);
        }
        return send(
                new DeviceBindingCallback(
                        config.authenticationType().name(),
                        challenge, identity.getUniversalId(), username,
                        getLocalisedMessage(context, localeSelector, this.getClass(),
                                config.title(), "titleDefault"),
                        getLocalisedMessage(context, localeSelector, this.getClass(),
                                config.subtitle(), "subtitleDefault"),
                        getLocalisedMessage(context, localeSelector, this.getClass(),
                                config.description(), "descriptionDefault"),
                        config.timeout(), config.attestation())).build();
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceBindingNode.OutcomeProvider.class.getClassLoader());

            ArrayList<Outcome> outcomes = new ArrayList<>();

            outcomes.add(new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)));
            outcomes.add(new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)));

            if (nodeAttributes.isNotNull()) {
                // nodeAttributes is null when the node is created
                if (nodeAttributes.get(MAX_SAVED_DEVICES).required().asInteger() > 0) {
                    outcomes.add(new Outcome(EXCEED_DEVICE_LIMIT_OUTCOME_ID,
                            bundle.getString(EXCEED_DEVICE_LIMIT_OUTCOME_ID)));
                }
                nodeAttributes.get(CLIENT_ERROR_OUTCOMES).required()
                        .asList(String.class)
                        .stream()
                        .map(outcome -> new Outcome(outcome, outcome))
                        .forEach(outcomes::add);

            } else {
                //Add the default when create
                DEFAULT_CLIENT_ERROR_OUTCOMES.stream()
                        .map(outcome -> new Outcome(outcome, outcome))
                        .forEach(outcomes::add);
            }
            return outcomes;
        }
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
            new OutputState(DEVICE, Map.of(SUCCESS_OUTCOME_ID, false))
        };
    }
}
