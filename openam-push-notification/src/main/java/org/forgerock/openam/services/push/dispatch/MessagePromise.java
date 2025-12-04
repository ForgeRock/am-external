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
package org.forgerock.openam.services.push.dispatch;

import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.generator.IdGenerator;
import org.forgerock.util.promise.PromiseImpl;

/**
 * A MessagePromise is an encapsulation of a promise and a set of predicates that must be (successfully) applied
 * to the contents of any supposed response to this message from an external source.
 *
 * A message which has been successfully responded to may be in one of two states: <code>DENIED</code> or
 * <code>SUCCESS</code> as per {@link MessageState}. Until it has been responded to, it is kept in the state
 * <code>UNKNOWN</code>.
 */
public class MessagePromise {

    private final PromiseImpl<JsonValue, Exception> promise;
    private final Set<Predicate> predicates = new HashSet<>();
    private final MessageId messageId;
    private final String auditTrackingId;

    /**
     * Generate a new MessagePromise with the supplied promise and predicates.
     *
     * @param promise The promise, likely retrieved from the MessageDispatcher or similar.
     * @param predicates Predicates to be applied to the contents of the response to this message.
     * @param messageId The message ID for the message.
     */
    public MessagePromise(PromiseImpl<JsonValue, Exception> promise, Set<Predicate> predicates,
                          MessageId messageId) {
        Reject.ifNull(predicates);
        Reject.ifNull(promise);
        this.predicates.addAll(predicates);
        this.promise = promise;
        this.messageId = messageId;
        this.auditTrackingId = IdGenerator.DEFAULT.generate();
    }

    /**
     * Informs whether there are any predicates set on this MessagePromise.
     *
     * @return true if there are predicates, false otherwise.
     */
    public boolean hasPredicates() {
        return !CollectionUtils.isEmpty(predicates);
    }

    /**
     * Retrieve the set of predicates which should be applied to the contents of any supposed response to
     * this message.
     *
     * @return a set of predicates.
     */
    public Set<Predicate> getPredicates() {
        return predicates;
    }

    /**
     * Retrieve the internally stored promise implementation.
     *
     * @return The stored promise implementation.
     */
    public PromiseImpl<JsonValue, Exception> getPromise() {
        return promise;
    }

    /**
     * Returns the message id of this message promise.
     *
     * @return MessageID.
     */
    public MessageId getMessageId() {
        return messageId;
    }

    /**
     * Returns the tracking ID, used for tracking exchanges relating to this message throughout the audit logs.
     *
     * @return the tracking ID.
     */
    public String getAuditTrackingId() {
        return auditTrackingId;
    }

    /**
     * Return the state of this message as a {@link MessageState}, whether it has been completed successfully or not,
     * or whether the state is currently unknown.
     *
     * @return the MessageState of this message, having been responded to.
     */
    public MessageState getMessageState() {

        if (!promise.isDone()) {
            return MessageState.UNKNOWN;
        }

        try {
            Jwt signedJwt = new JwtReconstruction().reconstructJwt(promise.get().get(JWT).asString(), Jwt.class);
            Boolean denied = (Boolean) signedJwt.getClaimsSet().getClaim(PushNotificationConstants.JWT_DENY_CLAIM_KEY);

            if (denied == null || !denied) { //null (not including deny param) counts as success
                return MessageState.SUCCESS;
            } else {
                return MessageState.DENIED;
            }
        } catch (Exception e) {
            //error, fall through to return unknown
        }

        return MessageState.UNKNOWN;
    }
}
