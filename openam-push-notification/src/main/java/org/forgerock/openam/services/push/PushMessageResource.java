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
 * Copyright 2016-2018 ForgeRock AS.
 */
package org.forgerock.openam.services.push;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.PredicateNotMetException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import com.sun.identity.shared.debug.Debug;

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
 * {@see SnsMessageResource}.
 * {@see PushNotificationService}.
 */
public class PushMessageResource {

    private final PushNotificationService pushNotificationService;
    private final Debug debug;
    private final MessageIdFactory messageIdFactory;

    /**
     * Generate a new PushMessageResource using the provided MessageDispatcher.
     *
     * @param pushNotificationService Used to get the message dispatcher, usde to deliver messages received at this
     *                         endpoint to their appropriate locations within OpenAM.
     * @param debug For writing out debug messages.
     * @param messageIdFactory Used to reconstruct the message ID object.
     */
    @Inject
    public PushMessageResource(PushNotificationService pushNotificationService, @Named("frPush") Debug debug,
            MessageIdFactory messageIdFactory) {
        this.pushNotificationService = pushNotificationService;
        this.debug = debug;
        this.messageIdFactory = messageIdFactory;
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
        Reject.ifFalse(context.containsContext(RealmContext.class));

        String realm = context.asContext(RealmContext.class).getRealm().asPath();

        final JsonValue actionContent = actionRequest.getContent();

        JsonValue messageIdLoc = actionContent.get(MESSAGE_ID_JSON_POINTER);
        MessageId messageId = null;

        try {
            try {
                if (messageIdLoc == null) {
                    debug.warning("Received message in realm {} with missing messageId.", realm);
                    return RestUtils.generateBadRequestException();
                }

                messageId = messageIdFactory.create(messageIdLoc.asString(), realm);
                ClusterMessageHandler messageHandler = pushNotificationService.getMessageHandlers(realm)
                        .get(messageId.getMessageType());
                MessageDispatcher messageDispatcher = pushNotificationService.getMessageDispatcher(realm);
                messageDispatcher.handle(messageId, actionContent, messageHandler);
            } catch (NotFoundException e) {
                debug.warning("Unable to deliver message with messageId {} in realm {}.", messageId, realm, e);
                return RestUtils.generateBadRequestException();
            } catch (PredicateNotMetException e) {
                debug.warning("Unable to deliver message with messageId {} in realm {} as predicate not met.",
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
}
