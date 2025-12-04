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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.service.datastore;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iplanet.services.naming.ServiceListeners;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.DataStoreInitializer;
import com.sun.identity.sm.SMSException;

/**
 * Initialises DataStores, but provides a layer of caching to avoid unnecessary calls.
 *
 * <p>This is used on defining new DataStores at runtime when creating new realms. The DataStore service is copied,
 * if present, to the new child realm and the configured DataStores are initialised for the new realm.</p>
 */
final class CachingDataStoreInitializer implements DataStoreInitializer {

    private final static Logger logger = LoggerFactory.getLogger(CachingDataStoreInitializer.class);

    private final DataStoreInitializer delegate;
    private final ServiceListeners serviceListeners;
    private final AtomicBoolean listenerInitialized = new AtomicBoolean();
    private final Set<CacheKey> storesInitialized = ConcurrentHashMap.newKeySet();
    private final Cache<String, Boolean> deletedRealms;

    @Inject
    public CachingDataStoreInitializer(DataStoreInitializer delegate, ServiceListeners serviceListeners) {
        this.delegate = delegate;
        this.serviceListeners = serviceListeners;
        deletedRealms = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))
                .softValues()
                .build();
    }

    private void initListeners() {
        if (listenerInitialized.compareAndSet(false, true)) {
            try {
                serviceListeners.forService(ISAuthConstants.EXTERNAL_DATASTORE_SERVICE_NAME)
                        .onGlobalChange(this::clearCache)
                        .onRealmChange(this::clearCacheForRealm)
                        .listen();

                Realms.onRealmCreation(realm -> deletedRealms.invalidate(realm.asPath()));
                Realms.onRealmDeletion(realm -> {
                    clearCacheForRealm(realm);
                    deletedRealms.put(realm.asPath(), true);
                });
            } catch (Exception e) {
                logger.debug("Unable to add external datastore service listener", e);
                listenerInitialized.compareAndSet(true, false);
                throw e;
            }
        }
    }

    private void clearCache() {
        storesInitialized.clear();
    }

    private void clearCacheForRealm(Realm realm) {
        storesInitialized.removeIf(key -> key.realm.equals(realm));
    }

    @Override
    public void handlePolicyDataStoreCreation(SSOToken token, Realm realm, DataStoreId dataStoreId)
            throws SSOException, SMSException, RealmLookupException {
        initListeners();
        CacheKey key = new CacheKey(realm, dataStoreId, StoreType.POLICY);
        if (storesInitialized.contains(key) || deletedRealms.asMap().containsKey(realm.asPath())) {
            return;
        }
        delegate.handlePolicyDataStoreCreation(token, realm, dataStoreId);
        storesInitialized.add(key);
    }

    @Override
    public void handleApplicationDataStoreCreation(SSOToken token, Realm realm, DataStoreId dataStoreId)
            throws SSOException, SMSException, RealmLookupException {
        initListeners();
        CacheKey key = new CacheKey(realm, dataStoreId, StoreType.APPLICATION);
        if (storesInitialized.contains(key) || deletedRealms.asMap().containsKey(realm.asPath())) {
            return;
        }
        delegate.handleApplicationDataStoreCreation(token, realm, dataStoreId);
        storesInitialized.add(key);
    }

    private static final class CacheKey {

        private final Realm realm;
        private final DataStoreId dataStoreId;
        private final StoreType storeType;

        private CacheKey(Realm realm, DataStoreId dataStoreId, StoreType storeType) {
            this.realm = realm;
            this.dataStoreId = dataStoreId;
            this.storeType = storeType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(realm, dataStoreId, storeType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey that = (CacheKey) o;
            return Objects.equal(this.realm, that.realm)
                    && Objects.equal(this.dataStoreId, that.dataStoreId)
                    && Objects.equal(this.storeType, that.storeType);
        }
    }

    private enum StoreType {
        POLICY,
        APPLICATION
    }
}
