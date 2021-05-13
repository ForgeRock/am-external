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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.BGCOLOUR_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_BG_COLOR;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_ISSUER;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_TIMEOUT;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.HIDDEN_CALLCABK_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.IMG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.ISSUER_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.SCAN_QR_CODE_MSG;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.URI_PATH_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.AUTH_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.CHALLENGE_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.LOADBALANCER_DATA_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_REGISTRATION_TIMEOUT;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_URI_SCHEME_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.REG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.SHARED_SECRET_QR_CODE_KEY;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.MessageType;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The Authenticator Push Registration node is a registration node that does not authenticate a user but
 * allows a user already authenticated earlier to register their mobile device.
 */
@Node.Metadata(outcomeProvider = PushRegistrationNode.OutcomeProvider.class,
        configClass = PushRegistrationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class PushRegistrationNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationNode.class);
    private static final String BUNDLE = PushRegistrationNode.class.getName();

    private final Config config;
    private final Realm realm;
    private final PushNotificationService pushNotificationService;
    private final SessionCookies sessionCookies;
    private final MessageIdFactory messageIdFactory;
    private final PushDeviceProfileHelper deviceProfileHelper;

    /**
     * The Push registration node constructor.
     *
     * @param config the node configuration.
     * @param realm the realm.
     * @param coreWrapper the {@code CoreWrapper} instance.
     * @param pushNotificationService the PushNotification system.
     * @param sessionCookies manages session cookies.
     * @param messageIdFactory handles push messages.
     * @param deviceProfileHelper stores device profiles.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityUtils an instance of the IdentityUtils.
     */
    @Inject
    public PushRegistrationNode(@Assisted Config config, @Assisted Realm realm, CoreWrapper coreWrapper,
            PushNotificationService pushNotificationService,
            SessionCookies sessionCookies,
            MessageIdFactory messageIdFactory,
            PushDeviceProfileHelper deviceProfileHelper,
            MultiFactorNodeDelegate<AuthenticatorPushService> multiFactorNodeDelegate,
            IdentityUtils identityUtils) {
        super(realm, coreWrapper, multiFactorNodeDelegate, identityUtils);
        this.config = config;
        this.realm = realm;
        this.pushNotificationService = pushNotificationService;
        this.sessionCookies = sessionCookies;
        this.messageIdFactory = messageIdFactory;
        this.deviceProfileHelper = deviceProfileHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("PushRegistrationNode started.");

        Optional<PollingWaitCallback> pollingWaitCallback = context.getCallback(PollingWaitCallback.class);

        if (pollingWaitCallback.isPresent()) {
            logger.debug("Registration process already started. Waiting for completion...");
            if (!context.sharedState.isDefined(MESSAGE_ID_KEY)) {
                logger.error("Unable to find push message ID in sharedState.");
                throw new NodeProcessException("Unable to find push message ID");
            }
            return getActionFromState(context);
        } else {
            String username = context.sharedState.get(USERNAME).asString();
            if (StringUtils.isBlank(username)) {
                logger.error("No username found.");
                throw new NodeProcessException("Expected username to be set.");
            }
            if (!deviceProfileHelper.isDeviceSettingsStored(username)) {
                logger.debug("User '{}' not registered for Push Authentication. "
                        + "Starting registration process.", username);
                return startRegistration(context);
            } else {
                logger.debug("User '{}' already registered for Push Authentication.", username);
                return buildAction(SUCCESS_OUTCOME_ID, context);
            }
        }
    }

    /**
     * Triggers the next Action according to the state.
     *
     * @param context the context of the tree authentication.
     * @return the next action to perform.
     */
    private Action getActionFromState(TreeContext context) throws NodeProcessException {
        MessageId messageId = getMessageId(context);
        MessageState messageState;

        try {
            PushDeviceSettings pushDeviceSettings = deviceProfileHelper
                    .getDeviceProfileFromSharedState(context, PUSH_DEVICE_PROFILE_KEY);
            if (pushDeviceSettings == null) {
                throw new NodeProcessException("No device profile found on shared state");
            }

            AMIdentity userIdentity = getIdentity(context);

            messageState = getMessageState(messageId);
            if (messageState == null) {
                logger.debug("The push message with ID {} has timed out", messageId);
                return buildAction(TIMEOUT_OUTCOME_ID, context);
            }

            switch (messageState) {
            case SUCCESS:
                logger.debug("The push registration message was processed successfully.");
                JsonValue pushContent = deleteMessage(messageId);
                List<String> recoveryCodes;

                if (pushContent != null) {
                    logger.debug("Saving push device profile.");
                    recoveryCodes = deviceProfileHelper.saveDeviceSettings(pushDeviceSettings,
                            pushContent, userIdentity, config.generateRecoveryCodes());
                    setUserToNotSkippable(userIdentity, realm.asPath());
                } else {
                    throw new NodeProcessException("Failed to save device to user profile");
                }

                if (CollectionUtils.isNotEmpty(recoveryCodes)) {
                    logger.debug("Completed Push registration. Sending recovery codes.");
                    return buildActionWithRecoveryCodes(context, pushDeviceSettings.getDeviceName(), recoveryCodes);
                } else {
                    logger.debug("Completed Push registration.");
                    return buildAction(SUCCESS_OUTCOME_ID, context);
                }
            case DENIED:
                logger.debug("The push registration message has failed.");
                deleteMessage(messageId);

                return buildAction(FAILURE_OUTCOME_ID, context);
            case UNKNOWN:
                logger.debug("Waiting push registration message to be processed.");
                JsonValue newSharedState = context.sharedState.copy();
                int timeOutInMs = config.timeout() * 1000;
                int timeElapsed = context.sharedState.get(PUSH_REGISTRATION_TIMEOUT).asInteger();
                if (timeElapsed >= timeOutInMs) {
                    return buildAction(TIMEOUT_OUTCOME_ID, context);
                } else {
                    newSharedState.put(PUSH_REGISTRATION_TIMEOUT, timeElapsed + REGISTER_DEVICE_POLL_INTERVAL);
                }
                String challenge = context.sharedState.get(PUSH_CHALLENGE_KEY).asString();
                List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings,
                        userIdentity, messageId.toString(), challenge, context);

                return send(callbacks)
                        .replaceSharedState(newSharedState)
                        .build();
            default:
                throw new NodeProcessException("Unrecognized push message status: " + messageState);
            }
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Unable initialise push notification service.", e);
        } catch (CoreTokenException e) {
            throw new NodeProcessException("An unexpected error occurred while verifying the push result", e);
        }
    }

    /**
     * Starts the Push Registration process.
     *
     * @param context the context of the tree authentication.
     * @return the next action to perform.
     */
    private Action startRegistration(TreeContext context) throws NodeProcessException {
        PushDeviceSettings pushDeviceSettings = deviceProfileHelper.createDeviceSettings(config.issuer());
        try {
            MessageId messageId = messageIdFactory.create(DefaultMessageTypes.REGISTER);
            String challenge = deviceProfileHelper.createChallenge();
            AMIdentity userIdentity = getIdentity(context);

            List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings, userIdentity,
                    messageId.toString(), challenge, context);

            updateMessageDispatcher(pushDeviceSettings, messageId, challenge);

            JsonValue sharedState = context.sharedState.copy()
                    .put(PUSH_DEVICE_PROFILE_KEY, deviceProfileHelper.encodeDeviceSettings(pushDeviceSettings))
                    .put(PUSH_CHALLENGE_KEY, challenge)
                    .put(PUSH_REGISTRATION_TIMEOUT, REGISTER_DEVICE_POLL_INTERVAL)
                    .put(MESSAGE_ID_KEY, messageId.toString());

            return send(callbacks)
                    .replaceSharedState(sharedState)
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Cleanup the SharedState.
     *
     * @param context the context of the tree authentication.
     * @return the action builder.
     */
    @Override
    public Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        JsonValue sharedState = context.sharedState.copy();
        sharedState.remove(MESSAGE_ID_KEY);
        sharedState.remove(PUSH_DEVICE_PROFILE_KEY);
        sharedState.remove(PUSH_CHALLENGE_KEY);

        return builder.replaceSharedState(sharedState);
    }

    /**
     * Creates the QRCode callback.
     *
     * @param identity the AM identity.
     * @param params the query parameters.
     * @return the QRCode callback.
     */
    @VisibleForTesting
    Callback createQRCodeCallback(AMIdentity identity, Map<String, String> params) {
        return createQRCodeCallback(
                PUSH_URI_SCHEME_QR_CODE_KEY,
                PUSH_URI_HOST_QR_CODE_KEY,
                URI_PATH_QR_CODE_KEY,
                getUserAttributeForAccountName(identity, config.accountName().toString()),
                0,
                params
        );
    }

    /**
     * Creates the HiddenValue callback.
     *
     * @param identity the AM identity.
     * @param params the query parameters.
     * @return the HiddenValue callback.
     */
    @VisibleForTesting
    Callback createHiddenCallback(AMIdentity identity, Map<String, String> params) {
        return createHiddenValueCallback(
                HIDDEN_CALLCABK_ID,
                PUSH_URI_SCHEME_QR_CODE_KEY,
                PUSH_URI_HOST_QR_CODE_KEY,
                URI_PATH_QR_CODE_KEY,
                getUserAttributeForAccountName(identity, config.accountName().toString()),
                params
        );
    }

    /**
     * Build the URI parameters to create the QR Code.
     *
     * @param deviceProfile the Push device's settings.
     * @param messageId the messageId key.
     * @param challenge the challenge secret.
     * @param context the context of the tree authentication.
     * @return the map of parameters.
     * @throws NodeProcessException if unable to retrieve the Push service URLs.
     */
    @VisibleForTesting
    Map<String, String> buildURIParameters(PushDeviceSettings deviceProfile, String messageId,
            String challenge, TreeContext context) throws NodeProcessException {
        Map<String, String> params = new LinkedHashMap<>();

        try {
            params.put(LOADBALANCER_DATA_QR_CODE_KEY,
                    Base64url.encode(sessionCookies.getLBCookie().getBytes()));
            params.put(ISSUER_QR_CODE_KEY,
                    Base64url.encode(config.issuer().getBytes()));
            params.put(MESSAGE_ID_QR_CODE_KEY, messageId);
            params.put(SHARED_SECRET_QR_CODE_KEY,
                    Base64url.encode(Base64.decode(deviceProfile.getSharedSecret())));
            params.put(CHALLENGE_QR_CODE_KEY, Base64url.encode(Base64.decode(challenge)));
            params.put(REG_QR_CODE_KEY,
                    Base64url.encode(getServiceAddressUrl(context, DefaultMessageTypes.REGISTER).getBytes()));
            params.put(AUTH_QR_CODE_KEY,
                    Base64url.encode(getServiceAddressUrl(context, DefaultMessageTypes.AUTHENTICATE).getBytes()));

            if (config.bgColor() != null && config.bgColor().startsWith("#")) {
                params.put(BGCOLOUR_QR_CODE_KEY, config.bgColor().substring(1));
            } else {
                params.put(BGCOLOUR_QR_CODE_KEY, config.bgColor());
            }

            if (config.imgUrl() != null) {
                params.put(IMG_QR_CODE_KEY, Base64url.encode(config.imgUrl().getBytes()));
            }
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Unable to read service addresses for Push Notification Service.", e);
        }

        return params;
    }

    /**
     * Creates a set of callbacks used to display the QRCode for scanning.
     *
     * @param deviceProfile the Push device's settings.
     * @param identity the AM identity.
     * @param messageId the messageId key.
     * @param challenge the challenge secret.
     * @param context the context of the tree authentication.
     * @return List of callbacks used for scan the QRCode.
     * @throws NodeProcessException if unable to create the callbacks.
     */
    private List<Callback> createScanQRCodeCallbacks(PushDeviceSettings deviceProfile,
            AMIdentity identity, String messageId, String challenge, TreeContext context)
            throws NodeProcessException {
        Map<String, String> params = buildURIParameters(deviceProfile, messageId, challenge, context);

        Callback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, SCAN_QR_CODE_MSG);
        Callback hiddenCallback = createHiddenCallback(identity, params);
        Callback qrCodeCallback = createQRCodeCallback(identity, params);

        List<Callback> callbacks = ImmutableList.of(
                textOutputCallback,
                qrCodeCallback,
                hiddenCallback,
                getPollingWaitCallback()
        );

        return callbacks;
    }

    /**
     * Gets the Push Authentication service URL.
     *
     * @param context the context of the tree authentication.
     * @param messageType the message type this endpoint location should handle.
     * @return the Push Authentication service URL.
     */
    private String getServiceAddressUrl(TreeContext context, MessageType messageType)
            throws PushNotificationException {
        return context.request.serverUrl + "/json" + pushNotificationService.getServiceAddressFor(realm.asPath(),
                messageType);
    }

    /**
     * Retrieves the messageId from the context.
     *
     * @param context the context of the tree authentication.
     * @return the messageId from the context.
     */
    @VisibleForTesting
    MessageId getMessageId(TreeContext context) throws NodeProcessException {
        try {
            String pushMessageId = context.sharedState.get(MESSAGE_ID_KEY).asString();
            return messageIdFactory.create(pushMessageId, realm.asPath());
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Could not get messageId", e);
        }
    }

    /**
     * Retrieves State of the messageId.
     *
     * @param messageId the message Id.
     * @return the message state.
     */
    @VisibleForTesting
    MessageState getMessageState(MessageId messageId)
            throws NodeProcessException, CoreTokenException, PushNotificationException {
        Map<MessageType, ClusterMessageHandler> messageHandlers =
                pushNotificationService.getMessageHandlers(realm.asPath());
        ClusterMessageHandler messageHandler = messageHandlers.get(messageId.getMessageType());

        if (messageHandler == null) {
            throw new NodeProcessException("The push message corresponds to " + messageId.getMessageType()
                    + " message type which is not registered in the " + realm + " realm");
        }
        return messageHandler.check(messageId);
    }

    /**
     * Delete the messageId and retrieves its content.
     *
     * @param messageId the message Id.
     * @return the message content.
     */
    @VisibleForTesting
    JsonValue deleteMessage(MessageId messageId) throws CoreTokenException, PushNotificationException {
        Map<MessageType, ClusterMessageHandler> messageHandlers =
                pushNotificationService.getMessageHandlers(realm.asPath());
        ClusterMessageHandler messageHandler = messageHandlers.get(messageId.getMessageType());
        JsonValue pushContent = messageHandler.getContents(messageId);
        messageHandler.delete(messageId);
        return pushContent;
    }

    /**
     * Dispatches a Push Registration message.
     *
     * @param pushDeviceSettings the Push device's settings.
     * @param messageId the messageId key.
     * @param challenge the challenge secret.
     */
    @VisibleForTesting
    void updateMessageDispatcher(PushDeviceSettings pushDeviceSettings, MessageId messageId, String challenge) {
        byte[] secret = Base64.decode(pushDeviceSettings.getSharedSecret());
        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(new SignedJwtVerificationPredicate(secret, JWT));
        servicePredicates.add(new PushMessageChallengeResponsePredicate(secret, challenge, JWT));

        try {
            Map<MessageType, Set<Predicate>> messagePredicates =
                    pushNotificationService.getMessagePredicatesFor(realm.asPath());
            Set<Predicate> predicates = messagePredicates.get(DefaultMessageTypes.REGISTER);
            if (predicates != null) {
                servicePredicates.addAll(predicates);
            }

            pushNotificationService.getMessageDispatcher(realm.asPath())
                    .expectInCluster(messageId, servicePredicates);
        } catch (NotFoundException | PushNotificationException e) {
            logger.error("Unable to read service addresses for Push Notification Service.", e);
        } catch (CoreTokenException e) {
            logger.error("Unable to persist token in core token service.", e);
        }
    }

    /**
     * Set the current user should not skip MFA.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     */
    @VisibleForTesting
    void setUserToNotSkippable(AMIdentity amIdentity, String realm) {
        setUserSkip(amIdentity, realm, SkipSetting.NOT_SKIPPABLE);
    }

    /**
     * The Push Registration node configuration.
     */
    public interface Config {

        /**
         * Specifies the name of the issuer.
         *
         * @return issuer name as string.
         */
        @Attribute(order = 10, validators = {RequiredValueValidator.class})
        default String issuer() {
            return DEFAULT_ISSUER;
        }

        /**
         * Specifies the attribute to be used as account name.
         *
         * @return account name as string.
         */
        @Attribute(order = 20)
        default UserAttributeToAccountNameMapping accountName() {
            return UserAttributeToAccountNameMapping.USERNAME;
        }

        /**
         * Specifies the timeout of the registration node.
         *
         * @return the length of time to wait for a device to register.
         */
        @Attribute(order = 30)
        default int timeout() {
            return DEFAULT_TIMEOUT;
        }

        /**
         * Background color of entry in ForgeRock Authenticator app.
         *
         * @return the hexadecimal color value as string.
         */
        @Attribute(order = 40)
        default String bgColor() {
            return DEFAULT_BG_COLOR;
        }

        /**
         * URL of a logo image resource associated with the Issuer.
         *
         * @return the URL of the logo image resource.
         */
        @Attribute(order = 50)
        default String imgUrl() {
            return "";
        }

        /**
         * Specifies whether to generate recovery codes and store them in the device profile.
         *
         * @return true if the codes are to be generated.
         */
        @Attribute(order = 60)
        default boolean generateRecoveryCodes() {
            return true;
        }
    }

    /**
     * Provides the push registration node's set of outcomes.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)),
                    new Outcome(TIMEOUT_OUTCOME_ID, bundle.getString(TIMEOUT_OUTCOME_ID)));
        }
    }
}
