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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.dispatch;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.TokenFactory;
import org.forgerock.am.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.services.push.MessageId;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Generates message dispatchers.
 */
@Singleton
public class MessageDispatcherFactory {

    private final JSONSerialisation jsonSerialisation;
    private final CTSPersistentStore ctsPersistentStore;
    private final TokenFactory tokenFactory;

    /**
     * Construct a new MessageDispatcherFactory with details to pass on to constructed {@link MessageDispatcher}s.
     *
     * @param ctsPersistentStore For storing cluster-wide messages.
     * @param jsonSerialisation To serialise objects into and out of JSON.
     * @param tokenFactory A TokenFactory instance.
     */
    @Inject
    public MessageDispatcherFactory(CTSPersistentStoreProxy ctsPersistentStore, JSONSerialisation jsonSerialisation,
            TokenFactory tokenFactory) {
        this.ctsPersistentStore = ctsPersistentStore;
        this.jsonSerialisation = jsonSerialisation;
        this.tokenFactory = tokenFactory;
    }

    /**
     * Generate a new MessageDispatcher configured with the appropriate settings.
     *
     * @param maxSize Maximum size of the cache.
     * @param concurrency Level of concurrency the cache supports.
     * @param expireAfter Entries should expire after this time from the cache (in seconds).
     * @return A newly constructed MessageDispatcher.
     */
    public MessageDispatcher create(long maxSize, int concurrency, int expireAfter) {

        Cache<MessageId, MessagePromise> cache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrency)
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfter, TimeUnit.SECONDS)
                .build();

        return new MessageDispatcher(cache, ctsPersistentStore, jsonSerialisation, expireAfter, tokenFactory);
    }

}
