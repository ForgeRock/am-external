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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.services.push;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;
import static org.forgerock.openam.services.push.PushNotificationConstants.MECHANISM_ID_JSON_POINTER;
import static org.forgerock.openam.services.push.PushNotificationConstants.MESSAGE_ID_JSON_POINTER;
import static org.forgerock.openam.services.push.PushNotificationConstants.USERNAME_JSON_POINTER;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.PredicateNotMetException;
import org.forgerock.openam.services.push.utils.PushDeviceUpdater;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A class for helping Push messages resolve correctly. Push messages should end up here so they can be dispatched
 * back to the appropriate place within OpenAM.</p>
 *
 * <p>This class handles both authentication and registration messages. These messages must
 * contain JSON which conforms to (at least) the following format:</p>
 *
 * <pre>
 * {
 *     "jwt" : "...", // signed jwt, including all claims necessary for operation (reg or auth)
 *     "messageId" : "..." // unique identifier for this message
 * }
 * </pre>
 *
 * <p>Additionally, this class provides an update method to handle push device updates. The update method expects
 * JSON in the following format:</p>
 *
 * <pre>
 * {
 *     "mechanismUid" : "...", // unique identifier for the device mechanism
 *     "username" : "...", // username associated with the device
 *     "jwt" : "...", // signed jwt, including all claims necessary for the update
 *     ...
 * }
 * </pre>
 *
 * {@see SnsMessageResource}.
 * {@see PushNotificationService}.
 */
public class PushMessageResource {

    private final PushNotificationService pushNotificationService;
    private final Logger debug = LoggerFactory.getLogger(PushMessageResource.class);
    private final MessageIdFactory messageIdFactory;
    private final PushDeviceUpdater pushDeviceUpdater;
    private static final String SIGNING_ALGORITHM = "HmacSHA256";

    /**
     * Generate a new PushMessageResource using the provided MessageDispatcher.
     *
     * @param pushNotificationService Used to get the message dispatcher, usde to deliver messages received at this
     *                         endpoint to their appropriate locations within OpenAM.
     * @param messageIdFactory Used to reconstruct the message ID object.
     * @param pushDeviceUpdater Used to update the push device settings.
     */
    @Inject
    public PushMessageResource(PushNotificationService pushNotificationService,
                               MessageIdFactory messageIdFactory,
                               PushDeviceUpdater pushDeviceUpdater) {
        this.pushNotificationService = pushNotificationService;
        this.messageIdFactory = messageIdFactory;
        this.pushDeviceUpdater = pushDeviceUpdater;
    }

    /**
     * Handles both <code>registration</code> and <code>authentication</code> messages from Push sources over
     * CREST. Should be called to deliver the message from the supported endpoint back to the appropriate
     * location within OpenAM via the {@link MessageDispatcher}.
     *
     * @param context Context of this request.
     * @param actionRequest The action to be performed.
     * @return The result of the operation.
     */
    public Promise<ActionResponse, ResourceException> handle(Context context, ActionRequest actionRequest) {
        Reject.unless(context.containsContext(RealmContext.class));

        String realm = context.asContext(RealmContext.class).getRealm().asPath();

        final JsonValue actionContent = actionRequest.getContent();

        JsonValue messageIdLoc = actionContent.get(MESSAGE_ID_JSON_POINTER);
        MessageId messageId = null;

        try {
            try {
                if (messageIdLoc == null) {
                    debug.warn("Received message in realm {} with missing messageId.", realm);
                    return RestUtils.generateBadRequestException();
                }

                messageId = messageIdFactory.create(messageIdLoc.asString(), realm);
                ClusterMessageHandler messageHandler = pushNotificationService.getMessageHandlers(realm)
                        .get(messageId.getMessageType());
                MessageDispatcher messageDispatcher = pushNotificationService.getMessageDispatcher(realm);
                messageDispatcher.handle(messageId, actionContent, messageHandler);
            } catch (NotFoundException e) {
                debug.warn("Unable to deliver message with messageId {} in realm {}.", messageId, realm, e);
                return RestUtils.generateBadRequestException();
            } catch (PredicateNotMetException e) {
                debug.warn("Unable to deliver message with messageId {} in realm {} as predicate not met.",
                        messageId, realm, e);
                return RestUtils.generateBadRequestException();
            } catch (PushNotificationException e) {
                throw new NotFoundException("Cannot find Push Notification Service.");
            }
        } catch (NotFoundException e) {
            return e.asPromise();
        }

        return newActionResponse(json(object())).asPromise();
    }

    /**
     * Updates the push device settings with the new device token.
     *
     * @param context Context of this request.
     * @param actionRequest The action to be performed.
     * @return The result of the operation.
     */
    public Promise<ActionResponse, ResourceException> update(Context context, ActionRequest actionRequest) {
        Reject.unless(context.containsContext(RealmContext.class));

        String realm = context.asContext(RealmContext.class).getRealm().asPath();

        final JsonValue deviceResponse = actionRequest.getContent();

        JsonValue mechanismUid = deviceResponse.get(MECHANISM_ID_JSON_POINTER);
        JsonValue username = deviceResponse.get(USERNAME_JSON_POINTER);

        // Check for required fields
        if (mechanismUid == null || mechanismUid.isNull()) {
            debug.warn("Received push device update request in realm {} with missing mechanismUid.",
                    realm);
            return RestUtils.generateBadRequestException();
        }

        if (username == null || username.isNull()) {
            debug.warn("Received push device update request in realm {} with missing username.",
                    realm);
            return RestUtils.generateBadRequestException();
        }

        try {
            // Get the device profile
            var pushDeviceSettings = pushDeviceUpdater
                    .getDeviceSettings(username.asString(), mechanismUid.asString(), realm);

            if (pushDeviceSettings == null) {
                debug.warn("Received push device update request in realm {} but device profile not found.", realm);
                return RestUtils.generateBadRequestException();
            }

            debug.debug("Updating push device for user {} in realm {}.", username.asString(), realm);

            // Validate the JWT
            var key = new SecretKeySpec(Base64.decode(pushDeviceSettings.getSharedSecret()), SIGNING_ALGORITHM);
            var verificationKey = new SecretBuilder()
                    .secretKey(key)
                    .clock(Time.getClock())
                    .build(Purpose.VERIFY);

            boolean jwtIsValid = pushDeviceUpdater
                    .validateSignedJwt(deviceResponse, verificationKey, new JsonPointer(JWT));
            if (!jwtIsValid) {
                debug.warn("Received push device update request in realm {} with invalid JWT signature.", realm);
                return RestUtils.generateBadRequestException();
            }

            // Update the device token with the push provider
            boolean deviceUpdated = pushDeviceUpdater.updateDevice(deviceResponse, realm);
            if (!deviceUpdated) {
                debug.warn("Error updating device with push provider for user {} in realm {}.",
                        username.asString(), realm);
                return RestUtils.generateBadRequestException();
            }

            debug.debug("Device profile updated for user {} in realm {}.", username.asString(), realm);
            pushDeviceUpdater.saveDeviceSettings(pushDeviceSettings, deviceResponse, username.asString(), realm);
        } catch (DevicePersistenceException e) {
            debug.warn("Error updating device profile for user {} in realm {}.", username.asString(), realm, e);
            return RestUtils.generateBadRequestException();
        } catch (NoSuchSecretException e) {
            debug.warn("Error retrieving shared secret for user {} in realm {}.", username.asString(), realm, e);
            return RestUtils.generateBadRequestException();
        }

        return newActionResponse(json(object())).asPromise();
    }

}