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
 * Copyright 2016-2019 ForgeRock AS.
 */
package org.forgerock.openam.services.push.dispatch;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import org.forgerock.am.cts.api.tokens.TokenFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.am.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PredicateNotMetException;
import org.forgerock.am.cts.api.tokens.CoreTokenField;
import org.forgerock.am.cts.api.tokens.TokenType;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.PromiseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

/**
 * <p>The MessageDispatcher acts to forward messages between disparate locations in OpenAM via
 * a unique identifier and an async interface. The MessageDispatcher should initially be
 * told to <code>#expect()</code> to receive a message with a given id - this will return an incomplete
 * {@link MessagePromise} which may or may not later be updated by this dispatcher; and store this promise in a cache
 * under a key of its messageId.</p>
 *
 * <p>The MessageDispatcher makes use of two caches - the in-memory Guava cache, used for communicating when the
 * response comes from the same machine as the initiating message (i.e. used when the <code>#handle()</code> is be sent
 * to the same machine that issued the <code>#expect()</code>); and the CTS, used for communicating between machines
 * within a cluster (i.e. used when the <code>#handle()</code> will be called from a different machine to the one which
 * called <code>#expect()</code>.</p>
 *
 * <p>Later, the MessageDispatcher may be asked to handle a message with a messageId and its JsonValue contents.</p>
 *
 * <p>If this messageId is expected, then the promise associated with that messageId will be asked whether it has
 * any {@link Predicate}s:</p>
 *
 * <ul>
 *  <li>If it does not, the promise will be completed and <code>#handle()</code> will return without error,</li>
 *  <li>If it does, then all the predicates will be run - one at a time, and if any of them return <code>false</code
 *          then the {@link MessagePromise} will NOT be completed, and the MessageDispatcher will continue to await
 *          a valid message in response to that messageId.</li>
 * </ul>
 *
 * <p>A message delivered by the CTS will be affected by the appropriate delegate's {@link ClusterMessageHandler} for
 * that message type, this can be used to alter or amend information to the token from the received message. It
 * may choose, for example, to update the token in a way that is being watched by a continuous query; or to update
 * such that a polling thread notices it. The {@link Token} will still be removed from the CTS at the end of its expiry
 * as determined by the original {@link MessageDispatcher}'s <code>timeout</code> value. Note that in this case the
 * machine which issued the #expect() will still have an entry in its in-memory cache until it times out, or #forget()
 * is explicitly called.</p>
 */
public class MessageDispatcher {

    /** CTS attribute to which the message's auditTrackingId is stored. **/
    private static final CoreTokenField CTS_AUDIT_TRACKING_ID_FIELD = CoreTokenField.STRING_FIVE;
    private static final boolean CACHE_LOCALLY = true;
    private static final boolean DONT_CACHE_LOCALLY = false;

    private final Cache<MessageId, MessagePromise> cache;
    private final Logger debug = LoggerFactory.getLogger(MessageDispatcher.class);
    private final CTSPersistentStore ctsPersistentStore;
    private final JSONSerialisation jsonSerialisation;
    private final int timeout;
    private final TokenFactory tokenFactory;

    /**
     * A message dispatcher which holds a Cache (a timeout-based Map) which contains the
     * promises which have yet to be returned to their instantiators.
     *
     * @param dispatch A cache to store messages which will shortly be dispatched.
     * @param ctsPersistentStore for checking if another machine in the cluster has written down the message.
     * @param jsonSerialisation for understanding JSON objects.
     * @param timeout the length of time a message will be valid in the cache/stored in the CTS.
     * @param tokenFactory A TokenFactory instance.
     */
    public MessageDispatcher(Cache<MessageId, MessagePromise> dispatch,
            CTSPersistentStore ctsPersistentStore, JSONSerialisation jsonSerialisation, int timeout,
            TokenFactory tokenFactory) {
        this.cache = dispatch;
        this.ctsPersistentStore = ctsPersistentStore;
        this.jsonSerialisation = jsonSerialisation;
        this.timeout = timeout;
        this.tokenFactory = tokenFactory;
    }

    /**
     * Handles the message passed to the dispatcher with the provided messageId.
     *
     * @param messageId The messageId of the promise to complete. May not be null.
     * @param content The contents to complete the awaiting promise with. May not be null.
     * @param messageHandler The handler that will manipulate the clustered version of this message.
     * @throws NotFoundException If the provided messageId was not found.
     * @throws PredicateNotMetException If any expected-successful predicate fails.
     */
    public void handle(MessageId messageId, JsonValue content, ClusterMessageHandler messageHandler)
            throws NotFoundException, PredicateNotMetException {
        Reject.ifNull(content);
        Reject.ifNull(messageId);
        MessagePromise messagePromise = cache.getIfPresent(messageId);
        if (messagePromise != null) {
            try {
                handlePushResponseLocally(messagePromise, content, messageId);
            } catch (CoreTokenException e) {
                debug.warn("Error deleting the cluster-wide message {} from the CTS.", messageId);
            }
        } else {
            try {
                handlePushResponseUsingCts(content, messageId, messageHandler);
            } catch (CoreTokenException | ClassNotFoundException e) {
                debug.warn("Cache was asked to handle {} but could not utilises the CTS.", messageId);
                throw new NotFoundException("Unable to utilise the CTS to to handle message " + messageId);
            }
        }
    }

    /**
     * Forgets any promise returned by this cache for the provided messageId. Asks to remove the message from
     * the CTS. Returns true if all of this occurred, false if the provided messageId was not found in the local cache.
     *
     * @param messageId The messageId to cancel.
     * @return <code>True</code> if the message was forgotten by the cache, <code>false</code> if the message did not
     * exist in the cache. Whether the token was removed from the CTS has no impact on this value.
     * @throws CoreTokenException if there was an issue deleting the token from the CTS.
     */
    public boolean forget(MessageId messageId) throws CoreTokenException {
        MessagePromise messagePromise = cache.getIfPresent(messageId);
        boolean result = false;

        if (messagePromise != null) {
            if (messagePromise.getAuditTrackingId() != null) {
                getAuditRequestContext().addTrackingId(messagePromise.getAuditTrackingId());
            }
            cache.invalidate(messageId);
            result = true;
        }

        ctsPersistentStore.deleteAsync(messageId.toString());

        return result;
    }

    /**
     * Tells the message dispatcher to expect a message to be handled with the given messageId. This returns
     * an incomplete promise. Only use this variant if you can guarantee that the server that expected the push message
     * will be the one checking if the promise has been completed.
     *
     * @param messageId The message key to inform this cache to prepare to handle. May not be null.
     * @param predicates The predicates that must be run against the content of any response to this messageId.
     * @return A promise which will later be completed by this MessageDispatcher handling a JsonValue for the messageId.
     * @throws CoreTokenException if there were issues storing the token in the CTS.
     */
    public MessagePromise expectLocally(MessageId messageId, Set<Predicate> predicates) throws CoreTokenException {
        return expect(messageId, predicates, CACHE_LOCALLY);
    }

    /**
     * Tells the message dispatcher to expect a message to be handled with the given messageId using the CTS only. Local
     * caching is disabled in this case as the push result may not be returned to the server that verifies it.
     *
     * @param messageId The message key to inform this cache to prepare to handle. May not be null.
     * @param predicates The predicates that must be run against the content of any response to this messageId.
     * @throws CoreTokenException if there were issues storing the token in the CTS.
     */
    public void expectInCluster(MessageId messageId, Set<Predicate> predicates) throws CoreTokenException {
        expect(messageId, predicates, DONT_CACHE_LOCALLY);
    }

    private MessagePromise expect(MessageId messageId, Set<Predicate> predicates, boolean cacheLocally)
            throws CoreTokenException {
        Reject.ifNull(messageId, predicates);
        Reject.ifTrue(StringUtils.isBlank(messageId.toString()));

        MessagePromise messagePromise = new MessagePromise(PromiseImpl.create(), predicates, messageId);
        getAuditRequestContext().addTrackingId(messagePromise.getAuditTrackingId());
        if (cacheLocally) {
            cache.put(messageId, messagePromise);
        }
        storeInCTS(messagePromise, timeout);
        return messagePromise;
    }

    private void handlePushResponseLocally(MessagePromise messagePromise, JsonValue content, MessageId messageId)
            throws PredicateNotMetException, CoreTokenException {
        getAuditRequestContext().addTrackingId(messagePromise.getAuditTrackingId());
        for (Predicate p : messagePromise.getPredicates()) {
            if (!p.perform(content)) {
                throw new PredicateNotMetException("Predicate was not matched. Message invalid.");
            }
        }

        messagePromise.getPromise().tryHandleResult(content);
        forget(messageId);
    }

    private void handlePushResponseUsingCts(JsonValue content, MessageId messageId,
            ClusterMessageHandler messageHandler) throws CoreTokenException, NotFoundException, ClassNotFoundException,
            PredicateNotMetException {
        Token coreToken = ctsPersistentStore.read(messageId.toString());

        if (coreToken == null) {
            debug.warn("Cache was asked to handle {} but never expected it.", messageId);
            throw new NotFoundException("Unable to find token with id " + messageId + " in CTS.");
        }

        String auditTrackingId = coreToken.getAttribute(CTS_AUDIT_TRACKING_ID_FIELD);
        if (auditTrackingId != null) {
            getAuditRequestContext().addTrackingId(auditTrackingId);
        }

        byte[] serializedBlob = coreToken.getBlob();
        String fromBlob = new String(serializedBlob);

        JsonValue jsonValue = JsonValueBuilder.toJsonValue(fromBlob);
        Map<String, Object> predicates = jsonValue.asMap();

        for (Map.Entry<String, Object> entry : predicates.entrySet()) {
            String className = entry.getKey();
            Predicate pred = jsonSerialisation.deserialise((String) entry.getValue(),
                            Class.forName(className).asSubclass(Predicate.class));
            if (!pred.perform(content)) {
                throw new PredicateNotMetException("Predicate was not matched. Message invalid.");
            }
        }

        messageHandler.update(coreToken, content);
    }

    private void storeInCTS(MessagePromise messagePromise, int timeout) throws CoreTokenException {
        Token ctsToken = tokenFactory.create(messagePromise.getMessageId().toString(), TokenType.PUSH);

        JsonValue jsonRepresentation = json(object());
        for (Predicate predicate : messagePromise.getPredicates()) {
            jsonRepresentation.put(predicate.getClass().getCanonicalName(), predicate.jsonify());
        }
        String result = jsonSerialisation.serialise(jsonRepresentation.getObject());
        ctsToken.setAttribute(CoreTokenField.BLOB, result.getBytes());

        Calendar calendar = Time.getCalendarInstance();
        calendar.add(Calendar.SECOND, timeout);
        ctsToken.setExpiryTimestamp(calendar);

        ctsToken.setAttribute(CTS_AUDIT_TRACKING_ID_FIELD, messagePromise.getAuditTrackingId());

        ctsPersistentStore.create(ctsToken);
    }
}
