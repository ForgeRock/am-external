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
 * Copyright 2023-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceBinding.FailureReason.INVALID_CLAIM;
import static org.forgerock.openam.auth.nodes.DeviceBinding.FailureReason.INVALID_SIGNATURE;
import static org.forgerock.openam.auth.nodes.DeviceBinding.FailureReason.INVALID_SUBJECT;
import static org.forgerock.openam.auth.nodes.DeviceBinding.FailureReason.INVALID_USER;
import static org.forgerock.openam.auth.nodes.DeviceBinding.FailureReason.NOT_ACTIVE_USER;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.forgerock.am.identity.application.LegacyIdentityService;
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
import org.forgerock.openam.authentication.callbacks.DeviceSigningVerifierCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingSettings;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A Device Signing Verifier Node. Verify the data signed by the bounded device
 */
@Node.Metadata(outcomeProvider = DeviceSigningVerifierNode.OutcomeProvider.class,
        configClass = DeviceSigningVerifierNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class DeviceSigningVerifierNode implements Node, DeviceBinding {

    private static final String BUNDLE = DeviceSigningVerifierNode.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(DeviceSigningVerifierNode.class);

    static final String SUCCESS_OUTCOME_ID = "success";
    static final String FAILURE_OUTCOME_ID = "failure";
    static final String NO_DEVICE_REGISTERED = "noDeviceRegistered";
    static final String KEY_NOT_FOUND = "keyNotFound";
    static final String CLIENT_ERROR_OUTCOMES = "clientErrorOutcomes";
    static final String CHALLENGE = DeviceSigningVerifierNode.class.getSimpleName() + ".CHALLENGE";
    static final String DEVICE_JWT_CONTEXT_NAME = DeviceSigningVerifierNode.class.getSimpleName() + ".JWT";

    static final String FAILURE_REASON = DeviceSigningVerifierNode.class.getSimpleName() + ".FAILURE";

    private final Config config;
    private final CoreWrapper coreWrapper;
    private final LocaleSelector localeSelector;
    private final LegacyIdentityService identityService;
    private final DeviceBindingManager deviceBindingManager;

    private static final List<String> DEFAULT_CLIENT_ERROR_OUTCOMES =
            List.of("Unsupported", "Abort", "Timeout", "ClientNotRegistered");

    //audit attribute
    private String jwt;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Challenge to be signed.
         *
         * @return The random challenge
         */
        @Attribute(order = 100)
        default boolean challenge() {
            return true;
        }

        /**
         * SharedState data to be signed.
         *
         * @return The SharedState attribute name
         */
        @Attribute(order = 200)
        default String sharedStateAttribute() {
            return "";
        }

        /**
         * The android package name and iOS bundle id that issue the binding request.
         *
         * @return The client authentication type
         */
        @Attribute(order = 250, validators = {RequiredValueValidator.class})
        default Set<String> applicationIds() {
            return emptySet();
        }

        /**
         * The title to be displayed for the biometric prompt.
         *
         * @return The title.
         */
        @Attribute(order = 300)
        default Map<Locale, String> title() {
            return Collections.emptyMap();
        }

        /**
         * The subtitle to be displayed for the biometric prompt.
         *
         * @return The subtitle.
         */
        @Attribute(order = 400)
        default Map<Locale, String> subtitle() {
            return Collections.emptyMap();
        }

        /**
         * The description to be displayed for the biometric prompt.
         *
         * @return The description.
         */
        @Attribute(order = 500)
        default Map<Locale, String> description() {
            return Collections.emptyMap();
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
         * If the node fail, error detail will be provided in the shared state for analysis by later nodes.
         *
         * @return true When enabled, instead of throwing {@link NodeProcessException}, it goes to Failure outcome.
         */
        @Attribute(order = 650)
        default boolean captureFailure() {
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
     * @param config the service config
     * @param identityService Identity Service to access the user identity
     * @param coreWrapper Instance of CoreWrapper
     * @param deviceBindingManager Instance of DeviceBindingManager
     * @param localeSelector a LocaleSelector for choosing the correct message to display
     */
    @Inject
    public DeviceSigningVerifierNode(@Assisted Config config, LegacyIdentityService identityService,
            DeviceBindingManager deviceBindingManager, CoreWrapper coreWrapper,
            LocaleSelector localeSelector) {
        this.config = config;
        this.identityService = identityService;
        this.coreWrapper = coreWrapper;
        this.localeSelector = localeSelector;
        this.deviceBindingManager = deviceBindingManager;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        Optional<DeviceSigningVerifierCallback> deviceSigningVerifierCallback =
                context.getCallback(DeviceSigningVerifierCallback.class);

        NodeState nodeState = context.getStateFor(this);

        if (deviceSigningVerifierCallback.isPresent()
                && StringUtils.isNotEmpty(deviceSigningVerifierCallback.get().getJws())) {
            //Signed data is received
            try {
                SignedJwt signedJwt = new JwtReconstruction().reconstructJwt(
                        deviceSigningVerifierCallback.get().getJws(), SignedJwt.class);

                String username = getUsername(context, nodeState, signedJwt);

                //Make sure the user is valid
                Optional<AMIdentity> userIdentity = getAMIdentity(Optional.of(username),
                        nodeState,
                        identityService, coreWrapper);
                validateUser(userIdentity, nodeState);

                //Check device registered
                String realm = getContextValue(nodeState, REALM);
                List<DeviceBindingSettings> deviceBindingSettings = deviceBindingManager.getDeviceProfiles(username,
                        realm);
                if (deviceBindingSettings.isEmpty()) {
                    return Action.goTo(NO_DEVICE_REGISTERED).build();
                }

                //Find the device by matching the kid from the header of JWT
                Optional<DeviceBindingSettings> deviceOptional = deviceBindingSettings.stream()
                        .filter(d -> d.getKey().getKeyId().equals(signedJwt.getHeader().getKeyId()))
                        .findFirst();
                if (deviceOptional.isEmpty()) {
                    return Action.goTo(KEY_NOT_FOUND).build();
                }

                DeviceBindingSettings device = deviceOptional.get();

                validateClaim(nodeState, signedJwt);
                validateSignature(nodeState, signedJwt, device.getKey());

                //For Audit
                jwt = deviceSigningVerifierCallback.get().getJws();

                device.setLastAccessDate(Time.currentTimeMillis());
                deviceBindingManager.saveDeviceProfile(username, realm, device);

                //Responsible for putting the username into the shared state so the appropriate user logs in
                nodeState.putShared(USERNAME, userIdentity.get().getName())
                        .putTransient(DEVICE_JWT_CONTEXT_NAME, jwt);

                return Action.goTo(SUCCESS_OUTCOME_ID)
                        .addNodeType(context, "Binding")
                        .withIdentifiedIdentity(userIdentity.get())
                        .withUniversalId(userIdentity.get().getUniversalId())
                        .build();
            } catch (NodeProcessException e) {
                if (config.captureFailure()) {
                    return Action.goTo(FAILURE_OUTCOME_ID).build();
                }
                throw e;
            } catch (Exception e) {
                logger.warn("Device Signing failed", e);
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }

        } else if (deviceSigningVerifierCallback.isPresent()
                && StringUtils.isNotEmpty(deviceSigningVerifierCallback.get().getClientError())) {
            //Client Error, go to client defined error
            return Action.goTo(deviceSigningVerifierCallback.get().getClientError()).build();
        } else {
            Optional<AMIdentity> userIdentity = getAMIdentity(context.universalId, context.getStateFor(this),
                    identityService, coreWrapper);
            try {
                if (userIdentity.isEmpty()) {
                    if (nodeState.isDefined((USERNAME))) {
                        nodeState.putShared(FAILURE_REASON, INVALID_USER.name());
                        throw new NodeProcessException("Failed to get the identity object");
                    }
                    //usernameless, the user will inject the userid during signing
                    return getCallback(context, nodeState, null);
                } else {
                    validateUser(userIdentity, nodeState);
                    String realm = getContextValue(nodeState, REALM);
                    List<DeviceBindingSettings> deviceBindingSettings = deviceBindingManager
                            .getDeviceProfiles(userIdentity.get().getUniversalId(),
                                    realm);
                    if (deviceBindingSettings.isEmpty()) {
                        return Action.goTo(NO_DEVICE_REGISTERED).build();
                    }
                    return getCallback(context, nodeState, userIdentity.get());
                }
            } catch (Exception e) {
                if (config.captureFailure()) {
                    return Action.goTo(FAILURE_OUTCOME_ID).build();
                }
                throw new NodeProcessException(e);
            }
        }
    }

    private void validateSignature(NodeState nodeState, SignedJwt signedJwt, JWK jwk)
            throws NoSuchSecretException, SignatureException, InvalidKeyException {
        try {
            validateSignature(signedJwt, jwk);
        } catch (Exception e) {
            nodeState.putShared(FAILURE_REASON, INVALID_SIGNATURE.name());
            throw e;
        }
    }

    private void validateClaim(NodeState nodeState, SignedJwt signedJwt) throws NodeProcessException {

        String challenge;
        if (config.challenge()) {
            //Using challenge instead of SharedContext attribute
            challenge = getContextValue(nodeState, CHALLENGE);
        } else {
            //Sign data from SharedContext attribute
            challenge = getContextValue(nodeState, config.sharedStateAttribute());
        }
        //Validate Claim
        try {
            validateClaim(signedJwt, challenge, config.applicationIds());
        } catch (Exception e) {
            nodeState.putShared(FAILURE_REASON, INVALID_CLAIM.name());
            throw e;
        }
    }

    /**
     * Verify the current context has the same user as the input JWT
     * For usernameless the current user context may be empty.
     */
    private String getUsername(TreeContext context, NodeState nodeState, SignedJwt signedJwt)
            throws NodeProcessException {

        //The subject should be the username
        String subject = signedJwt.getClaimsSet().getSubject();
        if (StringUtils.isEmpty(subject)) {
            nodeState.putShared(FAILURE_REASON, INVALID_SUBJECT.name());
            throw new NodeProcessException("Invalid Subject");
        }

        //Check to see if the current context includes a user
        Optional<AMIdentity> userIdentity = getAMIdentity(context.universalId, context.getStateFor(this),
                identityService, coreWrapper);

        //If no user context, then consider the subject as the user, but if current context has a user, fail the Node.
        if (userIdentity.isEmpty()) {
            if (nodeState.isDefined((USERNAME))) {
                nodeState.putShared(FAILURE_REASON, INVALID_USER.name());
                throw new NodeProcessException("Failed to get the identity object");
            }
            return subject;
        } else if (!userIdentity.get().getUniversalId().equals(subject)) {
            nodeState.putShared(FAILURE_REASON, INVALID_SUBJECT.name());
            throw new NodeProcessException("Invalid Subject");
        }
        return subject;
    }

    private Action getCallback(TreeContext context, NodeState nodeState, @Nullable AMIdentity identity)
            throws NodeProcessException {
        String challenge;
        if (config.challenge()) {
            challenge = createRandomBytes();
            nodeState.putShared(CHALLENGE, challenge);
        } else {
            challenge = getContextValue(nodeState, config.sharedStateAttribute());
        }
        if (identity == null) {
            return send(
                    new DeviceSigningVerifierCallback(challenge,
                            null,
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.title(),
                                    "titleDefault"),
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.subtitle(),
                                    "subtitleDefault"),
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.description(),
                                    "descriptionDefault"),
                            config.timeout())).build();
        } else {
            return send(
                    new DeviceSigningVerifierCallback(challenge,
                            identity.getUniversalId(),
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.title(),
                                    "titleDefault"),
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.subtitle(),
                                    "subtitleDefault"),
                            getLocalisedMessage(context, localeSelector, this.getClass(), config.description(),
                                    "descriptionDefault"),
                            config.timeout())).build();
        }
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceSigningVerifierNode.OutcomeProvider.class.getClassLoader());

            ArrayList<Outcome> outcomes = new ArrayList<>();

            outcomes.add(new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)));
            outcomes.add(new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)));
            outcomes.add(new Outcome(NO_DEVICE_REGISTERED, bundle.getString(NO_DEVICE_REGISTERED)));
            outcomes.add(new Outcome(KEY_NOT_FOUND, bundle.getString(KEY_NOT_FOUND)));

            if (nodeAttributes.isNotNull()) {
                // nodeAttributes is null when the node is created
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

    private void validateUser(Optional<AMIdentity> userIdentity, NodeState nodeState)
            throws NodeProcessException, IdRepoException, SSOException {
        try {
            if (userIdentity.isEmpty() || !userIdentity.get().isExists()) {
                nodeState.putShared(FAILURE_REASON, INVALID_USER.name());
                throw new NodeProcessException("User does not exist.");
            }

            //If user is not active, check if the configuration for routing to user not active outcome.
            if (!userIdentity.get().isActive()) {
                nodeState.putShared(FAILURE_REASON, NOT_ACTIVE_USER.name());
                throw new NodeProcessException("User status is not active.");
            }
        } catch (IdRepoException | SSOException e) {
            nodeState.putShared(FAILURE_REASON, INVALID_USER.name());
            throw e;
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return JsonValue.json(JsonValue.object(JsonValue.field(DEVICE_JWT_CONTEXT_NAME, jwt)));
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
            new OutputState(FAILURE_REASON, singletonMap("*", false)),
            new OutputState(USERNAME, singletonMap(SUCCESS_OUTCOME_ID, true))
        };
    }

}

