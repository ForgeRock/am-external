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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openam.services.push;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.Predicate;
import org.forgerock.openam.services.push.dispatch.PredicateNotMetException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import com.sun.identity.shared.debug.Debug;

/**
 * A class for helping Push messages resolve correctly. Push messages should end up here so they can be dispatched back
 * to the appropriate place within OpenAM.
 *
 * This class handles both authentication and registration messages. These messages must
 * contain JSON which conforms to (at least) the following format:
 *
 * {
 *     "jwt" : "...", // signed jwt, including all claims necessary for operation (reg or auth)
 *     "messageId" : "..." // unique identifier for this message
 * }
 *
 * {@see SnsMessageResource}.
 * {@see PushNotificationService}.
 */
public class PushMessageResource {

    private final PushNotificationService pushNotificationService;
    private final Debug debug;
    private final CTSPersistentStore coreTokenService;
    private final JSONSerialisation jsonSerialisation;
    private final JwtReconstruction jwtReconstruction;

    /**
     * Generate a new PushMessageResource using the provided MessageDispatcher.
     *
     * @param coreTokenService A copy of the core token services - messages are dropped on to this for use in clustered
     *                         environments.
     * @param pushNotificationService Used to get the message dispatcher, usde to deliver messages received at this
     *                         endpoint to their appropriate locations within OpenAM.
     * @param jsonSerialisation Used to perform the serialisation necessary for inserting tokens into the CTS.
     * @param debug For writing out debug messages.
     * @param jwtReconstruction For recreating JWTs.
     */
    @Inject
    public PushMessageResource(CTSPersistentStoreProxy coreTokenService,
                               PushNotificationService pushNotificationService,
                               JSONSerialisation jsonSerialisation, @Named("frPush") Debug debug,
                               JwtReconstruction jwtReconstruction) {
        this.pushNotificationService = pushNotificationService;
        this.jsonSerialisation = jsonSerialisation;
        this.debug = debug;
        this.coreTokenService = coreTokenService;
        this.jwtReconstruction = jwtReconstruction;
    }

    /**
     * Handles both <code>registration</code> and <code>authentication</code> messages from Push sources over
     * CREST. Should be called to deliver the message from the supported endpoint back to the appropriate
     * location within OpenAM via the {@link MessageDispatcher}.
     *
     * @param context Context of this request.
     * @param actionRequest The action to be performed.
     * @param requestType <code>registration</code> or <code>authentication</code>
     * @return The result of the operation.
     */
    public Promise<ActionResponse, ResourceException> handle(Context context, ActionRequest actionRequest,
            RequestType requestType) {
        Reject.ifFalse(context.containsContext(RealmContext.class));

        String realm = context.asContext(RealmContext.class).getRealm().asPath();

        final JsonValue actionContent = actionRequest.getContent();

        JsonValue messageIdLoc = actionContent.get(MESSAGE_ID_JSON_POINTER);
        String messageId;

        if (messageIdLoc == null) {
            debug.warning("Received message in realm {} with invalid messageId.", realm);
            return RestUtils.generateBadRequestException();
        } else {
            messageId = messageIdLoc.asString();
        }

        try {
            MessageDispatcher messageDispatcher = pushNotificationService.getMessageDispatcher(realm);
            try {
                messageDispatcher.handle(messageId, actionContent);
            } catch (NotFoundException e) {
                debug.warning("Unable to deliver message with messageId {} in realm {}.", messageId, realm, e);
                try {
                    attemptFromCTS(messageId, actionContent, requestType);
                } catch (IllegalAccessException | InstantiationException | ClassNotFoundException
                        | CoreTokenException | NotFoundException ex) {
                    debug.warning("Nothing in the CTS with messageId {}.", messageId, ex);
                    return RestUtils.generateBadRequestException();
                }
            } catch (PredicateNotMetException e) {
                debug.warning("Unable to deliver message with messageId {} in realm {} as predicate not met.",
                        messageId, realm, e);
                return RestUtils.generateBadRequestException();
            }
        } catch (NotFoundException e) {
            return e.asPromise();
        }

        return newActionResponse(json(object())).asPromise();
    }

    /**
     * For the in-memory equivalent, {@link MessageDispatcher#handle(String, JsonValue)}.
     */
    private boolean attemptFromCTS(String messageId, JsonValue actionContent, RequestType requestType)
            throws CoreTokenException, ClassNotFoundException, IllegalAccessException, InstantiationException,
            NotFoundException {
        Token coreToken = coreTokenService.read(messageId);

        if (coreToken == null) {
            throw new NotFoundException("Unable to find token with id " + messageId + " in CTS.");
        }

        byte[] serializedBlob = coreToken.getBlob();
        String fromBlob = new String(serializedBlob);

        JsonValue jsonValue = JsonValueBuilder.toJsonValue(fromBlob);
        Map<String, Object> predicates = jsonValue.asMap();

        for (Map.Entry<String, Object> entry : predicates.entrySet()) {
            String className = entry.getKey();
            Predicate pred =
                    (Predicate) jsonSerialisation.deserialise((String) entry.getValue(), Class.forName(className));
            if (!pred.perform(actionContent)) {
                return false;
            }
        }

        if (requestType == RequestType.REGISTER) {
            addRegistrationInfo(coreToken, actionContent);
        } else {
            addDeny(coreToken, actionContent);
        }

        coreTokenService.update(coreToken);

        return true;
    }

    private void addRegistrationInfo(Token coreToken, JsonValue actionContent) {
        coreToken.setBlob(jsonSerialisation.serialise(actionContent.getObject()).getBytes());
        coreToken.setAttribute(CoreTokenField.INTEGER_ONE, ACCEPT_VALUE);
    }

    private void addDeny(Token coreToken, JsonValue actionContent) {

        Jwt possibleDeny = jwtReconstruction.reconstructJwt(actionContent.get(JWT).asString(), SignedJwt.class);

        if (possibleDeny.getClaimsSet().getClaim(DENY_LOCATION) != null) {
            coreToken.setAttribute(CoreTokenField.INTEGER_ONE, DENY_VALUE);
        } else {
            coreToken.setAttribute(CoreTokenField.INTEGER_ONE, ACCEPT_VALUE);
        }

    }

    /**
     * A public enum, used to differentiate message types.
     */
    public enum RequestType {
        /**
         * An authentication request.
         */
        AUTHENTICATE,
        /**
         * A registration request.
         */
        REGISTER
    }

}
