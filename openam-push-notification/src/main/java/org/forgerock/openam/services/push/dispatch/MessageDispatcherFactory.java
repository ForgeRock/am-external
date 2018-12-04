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
package org.forgerock.openam.services.push.dispatch;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.services.push.MessageId;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.identity.shared.debug.Debug;

/**
 * Generates message dispatchers.
 */
@Singleton
public class MessageDispatcherFactory {

    private final JSONSerialisation jsonSerialisation;
    private final CTSPersistentStore ctsPersistentStore;

    /**
     * Construct a new MessageDispatcherFactory with details to pass on to constructed {@link MessageDispatcher}s.
     *
     * @param ctsPersistentStore For storing cluster-wide messages.
     * @param jsonSerialisation To serialise objects into and out of JSON.
     */
    @Inject
    public MessageDispatcherFactory(CTSPersistentStoreProxy ctsPersistentStore, JSONSerialisation jsonSerialisation) {
        this.ctsPersistentStore = ctsPersistentStore;
        this.jsonSerialisation = jsonSerialisation;
    }

    /**
     * Generate a new MessageDispatcher configured with the appropriate settings.
     *
     * @param maxSize Maximum size of the cache.
     * @param concurrency Level of concurrency the cache supports.
     * @param expireAfter Entries should expire after this time from the cache (in seconds).
     * @param debug A debug writer for errors.
     * @return A newly constructed MessageDispatcher.
     */
    public MessageDispatcher create(long maxSize, int concurrency, int expireAfter, Debug debug) {

        Cache<MessageId, MessagePromise> cache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrency)
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfter, TimeUnit.SECONDS)
                .build();

        return new MessageDispatcher(cache, debug, ctsPersistentStore, jsonSerialisation, expireAfter);
    }

}
