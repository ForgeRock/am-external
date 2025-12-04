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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.soap;

import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_MTLS;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_MTLS;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.secrets.Purpose.purpose;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;

import org.forgerock.http.Handler;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.http.CloseableHttpClientHandlerFactory;
import org.forgerock.openam.http.OptionsBuilder;
import org.forgerock.openam.secrets.DefaultingPurpose;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.secrets.config.SecretStoreConfigChangeListener;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

@Singleton
public class SamlMtlsHandlerFactory implements SecretStoreConfigChangeListener {

    private final Secrets secrets;
    private final CloseableHttpClientHandlerFactory factory;
    private final LoadingCache<CacheKey, Handler> cache;
    private final RealmLookup realmLookup;
    private final int handlerCacheMaxSize;

    private static final DefaultingPurpose<SigningKey> purpose =
            new DefaultingPurpose<>(purpose(SAML2_DEFAULT_SP_MTLS, SigningKey.class), SAML2_ENTITY_ROLE_MTLS);
    private static final String FORMAT_SPECIFIER = "%s";
    private static final String SAML2_ENTITY_SECRET_LABEL_PREFIX =
            SAML2_ENTITY_ROLE_MTLS.substring(0, SAML2_ENTITY_ROLE_MTLS.indexOf(FORMAT_SPECIFIER));
    private static final String MTLS = SAML2_ENTITY_ROLE_MTLS
            .substring(SAML2_ENTITY_ROLE_MTLS.indexOf(FORMAT_SPECIFIER) + FORMAT_SPECIFIER.length());
    private static final String SAML2_TLS_HANDLER_CACHE_SIZE_KEY = "org.forgerock.openam.saml2.tls.handler.cache.size";
    private static final int SAML2_TLS_HANDLER_CACHE_SIZE_DEFAULT = 50;

    private final Logger logger = LoggerFactory.getLogger(SamlMtlsHandlerFactory.class);

    private final Runnable cacheFullLogger;

    @Inject
    public SamlMtlsHandlerFactory(CloseableHttpClientHandlerFactory factory, Secrets secrets, RealmLookup realmLookup) {
        this.factory = factory;
        this.secrets = secrets;
        this.realmLookup = realmLookup;
        this.handlerCacheMaxSize = SystemPropertiesManager.getAsInt(SAML2_TLS_HANDLER_CACHE_SIZE_KEY,
                SAML2_TLS_HANDLER_CACHE_SIZE_DEFAULT);
        this.cache = initCache();

        this.cacheFullLogger = throttled(() -> logger.warn("The SAML mtls handler cache is full."+
                        " This could result in degraded performance." +
                        " Consider increasing the cache size by setting the advanced server property '{}'" +
                        " to a number higher than {}.", SAML2_TLS_HANDLER_CACHE_SIZE_KEY,
                handlerCacheMaxSize), 1, TimeUnit.MINUTES);
    }

    public Handler getHandler(Realm realm, String secretLabel) {
        try {
            return cache.get(new CacheKey(realm, customOrDefaultPurposeLabel(realm, secretLabel)));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to construct handler", e.getCause());
        }
    }

    private Handler createHandler(CacheKey cacheKey) {
        if (cache.size() == handlerCacheMaxSize) {
            cacheFullLogger.run();
        }
        OptionsBuilder extraOptions = OptionsBuilder.builder();
        KeyManager km = secrets.getRealmSecrets(cacheKey.realm)
                .getKeyManager(Purpose.purpose(cacheKey.secretLabel, SigningKey.class));
        extraOptions.withKeyManagers(new KeyManager[]{km});
        return factory.create(extraOptions);
    }

    private LoadingCache<CacheKey, Handler> initCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(handlerCacheMaxSize)
                .build(new CacheLoader<>() {
                    @Override
                    public Handler load(CacheKey cacheKey) {
                        return createHandler(cacheKey);
                    }
                });
    }

    private boolean isPurposeMapped(SecretsProviderFacade secretsProviderFacade, Purpose<?> purpose) {
        try {
            secretsProviderFacade.getActiveSecret(purpose).getOrThrowIfInterrupted();
            return true;
        } catch (NoSuchSecretException e) {
            return false;
        }
    }

    @Override
    public void secretStoreHasChanged(SecretStoreWithMappings secretStore, String orgName, int type) {
        if (isNotBlank(orgName)) {
            try {
                Realm realm = realmLookup.lookup(orgName);
                cache.invalidateAll(cache.asMap().keySet().stream()
                        .filter(cacheKey -> cacheKey.realm.equals(realm))
                        .collect(Collectors.toSet()));
            } catch (RealmLookupException e) {
                cache.invalidateAll();
            }
        } else {
            cache.invalidateAll();
        }
    }

    @Override
    public void secretStoreMappingHasChanged(PurposeMapping mapping, String orgName, int type) {
        Realm realm = null;
        if (isNotBlank(orgName)) {
            try {
                realm = realmLookup.lookup(orgName);
            } catch (RealmLookupException e) {
                // ignore
            }
        }

        if (realm != null && mapping != null && isDefaultOrCustomMtlsLabel(mapping.secretId())) {
            cache.invalidate(new CacheKey(realm, mapping.secretId()));
        } else if (realm != null) {
            Realm finalRealm = realm;
            cache.invalidateAll(cache.asMap().keySet().stream()
                    .filter(cacheKey -> cacheKey.realm.equals(finalRealm))
                    .collect(Collectors.toSet()));
        } else {
            cache.invalidateAll();
        }
    }

    private boolean isDefaultOrCustomMtlsLabel(String label) {
        if (label.equals(purpose.getDefaultPurpose().getLabel())) {
            return true;
        } else {
            return label.startsWith(SAML2_ENTITY_SECRET_LABEL_PREFIX) && (label.endsWith(MTLS));
        }
    }

    private String customOrDefaultPurposeLabel(Realm realm, String secretLabel) {
        SecretsProviderFacade secretsProviderFacade = secrets.getRealmSecrets(realm);
        if (isNotEmpty(secretLabel)) {
            Purpose<SigningKey> customPurpose = purpose.getCustomPurpose(secretLabel);
            if (isPurposeMapped(secretsProviderFacade, customPurpose)) {
                return customPurpose.getLabel();
            }
        }
        return purpose.getDefaultPurpose().getLabel();
    }

    private static Runnable throttled(final Runnable runnable, final long period, final TimeUnit periodUnit) {
        return new Runnable() {
            private long lastRun = 0;

            @Override
            public void run() {
                if (currentTimeMillis() >= lastRun + periodUnit.toMillis(period)) {
                    lastRun = currentTimeMillis();
                    runnable.run();
                }
            }
        };
    }

    static final class CacheKey {
        private final Realm realm;
        private final String secretLabel;

        @VisibleForTesting
        CacheKey(Realm realm, String secretLabel) {
            this.realm = realm;
            this.secretLabel = secretLabel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return realm.equals(cacheKey.realm) && secretLabel.equals(cacheKey.secretLabel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realm, secretLabel);
        }
    }
}
