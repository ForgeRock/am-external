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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.REGISTER_DEVICE_POLL_INTERVAL;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.TIMEOUT_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.BGCOLOUR_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.HIDDEN_CALLCABK_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.IMG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.ISSUER_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.SCAN_QR_CODE_MSG_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.USER_ID_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.AUTH_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.CHALLENGE_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.LOADBALANCER_DATA_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_REGISTRATION_TIMEOUT;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_RESOURCE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_URI_SCHEME_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.REG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.SHARED_SECRET_QR_CODE_KEY;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.net.UrlEscapers;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationUtilities;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
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
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Helper class used for nodes performing Push Registration, shares common features and components across the
 * multiple push-related nodes.
 */
public class PushRegistrationHelper {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationHelper.class);

    private final Realm realm;
    private final PushNotificationService pushNotificationService;
    private final SessionCookies sessionCookies;
    private final MessageIdFactory messageIdFactory;
    private final PushDeviceProfileHelper deviceProfileHelper;
    private final MultiFactorNodeDelegate<AuthenticatorPushService> multiFactorNodeDelegate;
    private final LocalizedMessageProvider localizationHelper;
    private final MultiFactorRegistrationUtilities multiFactorRegistrationUtilities;


    /**
     * The Push registration helper constructor.
     *
     * @param realm                            the realm.
     * @param pushNotificationService          the PushNotification system.
     * @param sessionCookies                   manages session cookies.
     * @param messageIdFactory                 handles push messages.
     * @param deviceProfileHelper              stores device profiles.
     * @param multiFactorNodeDelegate          shared utilities common to second factor implementations.
     * @param localizationHelperFactory        the localization helper factory.
     * @param multiFactorRegistrationUtilities shared utilities for mfa registration operations.
     */
    @Inject
    public PushRegistrationHelper(@Assisted Realm realm,
                                  PushNotificationService pushNotificationService,
                                  SessionCookies sessionCookies,
                                  MessageIdFactory messageIdFactory,
                                  PushDeviceProfileHelper deviceProfileHelper,
                                  MultiFactorNodeDelegate<AuthenticatorPushService> multiFactorNodeDelegate,
                                  LocalizedMessageProviderFactory localizationHelperFactory,
                                  MultiFactorRegistrationUtilities multiFactorRegistrationUtilities) {
        this.realm = realm;
        this.pushNotificationService = pushNotificationService;
        this.sessionCookies = sessionCookies;
        this.messageIdFactory = messageIdFactory;
        this.deviceProfileHelper = deviceProfileHelper;
        this.multiFactorNodeDelegate = multiFactorNodeDelegate;
        this.localizationHelper = localizationHelperFactory.create(realm);
        this.multiFactorRegistrationUtilities = multiFactorRegistrationUtilities;
    }

    /**
     * Checks if Device Profile is stored.
     *
     * @param username the username of user
     * @return true if already stored, false otherwise.
     */
    public boolean isDeviceSettingsStored(String username) {
        return this.deviceProfileHelper.isDeviceSettingsStored(username);
    }

    /**
     * Return the device settings object stored on the shared state.
     *
     * @param context the context of the tree authentication.
     * @return the push device settings object.
     * @throws NodeProcessException if fail to deserialize device profile from shared state.
     */
    public PushDeviceSettings getDeviceProfileFromSharedState(TreeContext context) throws NodeProcessException {
        return deviceProfileHelper
                .getDeviceProfileFromSharedState(context, PUSH_DEVICE_PROFILE_KEY);
    }

    /**
     * Process successful registration message.
     *
     * @param messageId             the message Id.
     * @param pushDeviceSettings    the Push device's settings.
     * @param userIdentity          the AM identity.
     * @param generateRecoveryCodes indicates the generation of recovery codes.
     * @return the list of recovery codes.
     * @throws NodeProcessException      if failed to save device to user profile.
     * @throws CoreTokenException        if unable to remove registration message.
     * @throws PushNotificationException if unable to remove registration message.
     */
    public List<String> handleSuccessMessage(MessageId messageId,
                                             PushDeviceSettings pushDeviceSettings,
                                             AMIdentity userIdentity,
                                             boolean generateRecoveryCodes)
            throws NodeProcessException, CoreTokenException, PushNotificationException {
        logger.debug("The push registration message was processed successfully.");

        JsonValue pushContent = deleteMessage(messageId);
        List<String> recoveryCodes;
        if (pushContent != null) {
            logger.debug("Saving push device profile.");
            recoveryCodes = saveDeviceSettings(pushDeviceSettings, pushContent, userIdentity, generateRecoveryCodes);
        } else {
            throw new NodeProcessException("Failed to save device to user profile");
        }
        return recoveryCodes;
    }

    /**
     * Builds an Action for wait the registration to complete.
     *
     * @param context   the context of the tree authentication.
     * @param callbacks list of Callbacks.
     * @param timeout   the wait timeout.
     * @return the instance of the ActionBuilder.
     */
    public Action.ActionBuilder waitRegistration(TreeContext context, List<Callback> callbacks, int timeout) {
        logger.debug("Waiting push registration message to be processed.");
        JsonValue newSharedState = context.sharedState.copy();
        int timeOutInMs = timeout * 1000;
        int timeElapsed = context.sharedState.get(PUSH_REGISTRATION_TIMEOUT).asInteger();
        if (timeElapsed >= timeOutInMs) {
            return Action.goTo(TIMEOUT_OUTCOME_ID);
        } else {
            newSharedState.put(PUSH_REGISTRATION_TIMEOUT, timeElapsed + REGISTER_DEVICE_POLL_INTERVAL);
        }
        return send(callbacks).replaceSharedState(newSharedState);
    }

    /**
     * Builds an Action for the failure Outcome and cleanup the SharedState.
     *
     * @param context   the context of the tree authentication.
     * @param messageId the message Id.
     * @return the instance of the ActionBuilder.
     * @throws CoreTokenException        if unable to remove registration message.
     * @throws PushNotificationException if unable to remove registration message.
     */
    public Action.ActionBuilder failRegistration(TreeContext context, MessageId messageId)
            throws CoreTokenException, PushNotificationException {
        logger.debug("The push registration message has failed.");
        deleteMessage(messageId);
        Action.ActionBuilder builder = Action.goTo(FAILURE_OUTCOME_ID);
        return cleanupSharedState(context, builder);
    }

    /**
     * Builds an Action for the timeout Outcome and cleanup the SharedState.
     *
     * @param context   the context of the tree authentication.
     * @param messageId the message Id.
     * @return the instance of the ActionBuilder.
     */
    public Action.ActionBuilder timeoutRegistration(TreeContext context, MessageId messageId) {
        logger.debug("The push message with ID {} has timed out", messageId);
        Action.ActionBuilder builder = Action.goTo(TIMEOUT_OUTCOME_ID);
        return cleanupSharedState(context, builder);
    }

    /**
     * Builds an Action for the success Outcome and update the SharedState.
     *
     * @param context            the context of the tree authentication.
     * @param pushDeviceSettings the Push device's settings.
     * @param recoveryCodes      the list of recovery codes.
     * @return the instance of the ActionBuilder.
     */
    public Action.ActionBuilder completeRegistration(TreeContext context,
                                                     PushDeviceSettings pushDeviceSettings,
                                                     List<String> recoveryCodes) {
        Action.ActionBuilder builder = Action.goTo(SUCCESS_OUTCOME_ID);

        if (CollectionUtils.isNotEmpty(recoveryCodes)) {
            logger.debug("Completed Push registration. Sending recovery codes.");
            JsonValue transientState = context.transientState.copy();
            transientState
                    .put(RECOVERY_CODE_KEY, recoveryCodes)
                    .put(RECOVERY_CODE_DEVICE_NAME, pushDeviceSettings.getDeviceName());

            return cleanupSharedState(context, builder).replaceTransientState(transientState);
        } else {
            logger.debug("Completed Push registration.");
            return cleanupSharedState(context, builder);
        }
    }

    /**
     * Cleanup the SharedState.
     *
     * @param context the context of the tree authentication.
     * @param builder the ActionBuilder.
     * @return the same instance of the ActionBuilder.
     */
    public Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        JsonValue sharedState = context.sharedState.copy();
        sharedState.remove(MESSAGE_ID_KEY);
        sharedState.remove(PUSH_DEVICE_PROFILE_KEY);
        sharedState.remove(PUSH_CHALLENGE_KEY);
        return builder.replaceSharedState(sharedState);
    }

    /**
     * Remove push entries from given SharedState.
     *
     * @param nodeState the Node state representation object
     */
    public void clearSharedState(NodeState nodeState) {
        nodeState.remove(MESSAGE_ID_KEY);
        nodeState.remove(PUSH_DEVICE_PROFILE_KEY);
        nodeState.remove(PUSH_CHALLENGE_KEY);
    }

    /**
     * Set push entries used for registration to the SharedState.
     *
     * @param nodeState          the Node state representation object.
     * @param pushDeviceSettings the Push device's settings.
     * @param messageId          the messageId key.
     * @param challenge          the challenge secret.
     * @throws IOException if unable to update the shared state.
     */
    public void updateSharedState(NodeState nodeState,
                                  PushDeviceSettings pushDeviceSettings,
                                  MessageId messageId,
                                  String challenge) throws IOException {
        nodeState.putShared(PUSH_DEVICE_PROFILE_KEY, deviceProfileHelper.encodeDeviceSettings(pushDeviceSettings));
        nodeState.putShared(PUSH_CHALLENGE_KEY, challenge);
        nodeState.putShared(PUSH_REGISTRATION_TIMEOUT, REGISTER_DEVICE_POLL_INTERVAL);
        nodeState.putShared(MESSAGE_ID_KEY, messageId.toString());
    }

    /**
     * Create a new Push device's settings.
     *
     * @param config the node configuration.
     * @return the Push device's settings.
     */
    public PushDeviceSettings createDeviceSettings(PushRegistrationConfig config) {
        return deviceProfileHelper.createDeviceSettings(config.issuer());
    }

    /**
     * Save the push device settings and return an optional set of recovery codes.
     *
     * @param pushDeviceSettings    the Push device's settings.
     * @param deviceResponse        the payload response containing device data.
     * @param userIdentity          the AM identity.
     * @param generateRecoveryCodes indicates the generation of recovery codes.
     * @return list of recovery codes.
     * @throws NodeProcessException if unable to store device profile or generate the recovery codes.
     */
    public List<String> saveDeviceSettings(
            PushDeviceSettings pushDeviceSettings,
            JsonValue deviceResponse,
            AMIdentity userIdentity,
            boolean generateRecoveryCodes) throws NodeProcessException {
        List<String> recoveryCodes = deviceProfileHelper.saveDeviceSettings(pushDeviceSettings,
                deviceResponse, userIdentity, generateRecoveryCodes);
        setUserToNotSkippable(userIdentity);
        return recoveryCodes;
    }

    /**
     * Create the URI to register a push mechanism.
     *
     * @param userIdentity  the AM identity.
     * @param deviceProfile the Push device's settings.
     * @param messageId     the messageId key.
     * @param challenge     the challenge secret.
     * @param config        the Push registration configuration.
     * @param context       the context of the tree authentication.
     * @return the registration URI.
     * @throws NodeProcessException if unable to create the Push registration URI.
     */
    public String createRegistrationUri(AMIdentity userIdentity, PushDeviceSettings deviceProfile, String messageId,
                                        String challenge, PushRegistrationConfig config, TreeContext context)
            throws NodeProcessException {
        Map<String, String> params = buildURIParameters(deviceProfile, userIdentity.getName(), messageId, challenge,
                config, context);

        return multiFactorRegistrationUtilities.createUri(
                PUSH_URI_SCHEME_QR_CODE_KEY,
                PUSH_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(userIdentity,
                        config.accountName().toString()),
                params);
    }

    /**
     * Creates the QRCode callback.
     *
     * @param userIdentity  the AM identity.
     * @param params        the query parameters.
     * @param issuer        the issuer.
     * @param userAttribute the user attribute.
     * @return the QRCode callback.
     */
    public Callback createQRCodeCallback(AMIdentity userIdentity, Map<String, String> params, String issuer,
                                         String userAttribute) {
        return multiFactorRegistrationUtilities.createQRCodeCallback(
                PUSH_URI_SCHEME_QR_CODE_KEY,
                PUSH_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(issuer),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(userIdentity, userAttribute),
                0,
                params
        );
    }

    /**
     * Creates the HiddenValue callback.
     *
     * @param userIdentity  the AM identity.
     * @param params        the query parameters.
     * @param issuer        the issuer.
     * @param userAttribute the user attribute.
     * @return the HiddenValue callback.
     */
    public Callback createHiddenCallback(AMIdentity userIdentity, Map<String, String> params, String issuer,
                                         String userAttribute) {
        return multiFactorRegistrationUtilities.createHiddenValueCallback(
                HIDDEN_CALLCABK_ID,
                PUSH_URI_SCHEME_QR_CODE_KEY,
                PUSH_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(issuer),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(userIdentity, userAttribute),
                params
        );
    }

    /**
     * Creates a localized TextOutput callback.
     *
     * @param context           the context of the tree authentication.
     * @param bundleClass       the bundle class.
     * @param scanQRCodeMessage localized scan QR Code message.
     * @return the textOutput callback.
     */
    public Callback createLocalizedTextCallback(TreeContext context, Class<?> bundleClass,
                                                Map<Locale, String> scanQRCodeMessage) {
        String message = localizationHelper.getLocalizedMessage(context, bundleClass,
                scanQRCodeMessage, SCAN_QR_CODE_MSG_KEY);
        return new TextOutputCallback(TextOutputCallback.INFORMATION, message);
    }

    /**
     * Create a new PollingWaitCallback with default wait time.
     *
     * @return a polling waiting callback.
     */
    public Callback createPollingWaitCallback() {
        return multiFactorRegistrationUtilities.getPollingWaitCallback();
    }

    /**
     * Build the URI parameters to create the QR Code.
     *
     * @param deviceProfile the Push device's settings.
     * @param userId        the user ID.
     * @param messageId     the messageId key.
     * @param challenge     the challenge secret.
     * @param config        the Push registration configuration.
     * @param context       the context of the tree authentication.
     * @return the map of parameters.
     * @throws NodeProcessException if unable to retrieve the Push service URLs.
     */
    public Map<String, String> buildURIParameters(PushDeviceSettings deviceProfile,
                                                  String userId,
                                                  String messageId,
                                                  String challenge,
                                                  PushRegistrationConfig config,
                                                  TreeContext context) throws NodeProcessException {
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
            params.put(USER_ID_QR_CODE_KEY, Base64url.encode(userId.getBytes()));
            params.put(PUSH_RESOURCE_ID_KEY, Base64url.encode(deviceProfile.getUUID().getBytes()));

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
     * Gets the Push Authentication service URL.
     *
     * @param context     the context of the tree authentication.
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
     * @throws NodeProcessException if unable to create registration message.
     */
    public MessageId getMessageId(TreeContext context) throws NodeProcessException {
        try {
            String pushMessageId = context.sharedState.get(MESSAGE_ID_KEY).asString();
            return messageIdFactory.create(pushMessageId, realm.asPath());
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Could not get messageId", e);
        }
    }

    /**
     * Creates a new Push registration message.
     *
     * @return the unique {@link MessageId} instance.
     */
    public MessageId createRegistrationMessage() {
        return messageIdFactory.create(DefaultMessageTypes.REGISTER);
    }

    /**
     * Create a new challenge for the push registration.
     *
     * @return the challenge as a Base64 encoded String.
     */
    public String createChallenge() {
        return deviceProfileHelper.createChallenge();
    }

    /**
     * Retrieves State of the messageId.
     *
     * @param messageId the message Id.
     * @return the message state.
     * @throws NodeProcessException if unable to retrieve message state.
     * @throws CoreTokenException if unable to start registration process.
     * @throws PushNotificationException if unable to start registration process.
     */
    public MessageState getMessageState(MessageId messageId)
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
     * @throws CoreTokenException if unable to delete the message.
     * @throws PushNotificationException if unable to delete the message.
     */
    public JsonValue deleteMessage(MessageId messageId) throws CoreTokenException, PushNotificationException {
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
     * @param messageId          the messageId key.
     * @param challenge          the challenge secret.
     */
    public void updateMessageDispatcher(PushDeviceSettings pushDeviceSettings, MessageId messageId, String challenge) {
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
     * @param userIdentity the AM identity.
     */
    public void setUserToNotSkippable(AMIdentity userIdentity) {
        try {
            multiFactorNodeDelegate.setUserSkip(userIdentity, realm.asPath(), SkipSetting.NOT_SKIPPABLE);
        } catch (NodeProcessException e) {
            logger.debug("Unable to set user attribute as skippable.", e);
        }
    }

}
