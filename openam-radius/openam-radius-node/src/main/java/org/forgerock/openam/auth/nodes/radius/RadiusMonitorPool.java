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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * A singleton pool that manages RADIUSMonitor instances by connection UUID.
 * This allows monitors to persist even when RadiusConn instances are torn down,
 * and enables reuse of monitors when connections are reopened with the same UUID.
 */
@Singleton
public class RadiusMonitorPool implements ServiceListener {
    private static final Logger logger = LoggerFactory.getLogger(RadiusMonitorPool.class);
    private static final String SERVICE_NAME = "RadiusDecisionNode";

    private final Map<UUID, RadiusMonitor> monitors = new ConcurrentHashMap<>();
    private final MessageAuthenticatorCalculator messageAuthenticatorCalculator;
    private final AnnotatedServiceRegistry annotatedServiceRegistry;
    private final SecretReferenceCache secretReferenceCache;
    private final TimerPool timerPool;
    private final RealmLookup realmLookup;

    private record MonitorSettings(Realm realm, String secret, int timeout, int healthCheckInterval,
                                   boolean requireMessageAuthenticator) {
    }

    /**
     * Constructs a RadiusMonitorPool and registers it as a listener for RADIUSDecisionNode config changes.
     *
     * @param configManagerFactory           the service config manager factory
     * @param messageAuthenticatorCalculator the message authenticator calculator
     * @param annotatedServiceRegistry       the annotated service registry
     * @param secretReferenceCache           the secret reference cache
     * @param realmLookup                    the realm lookup used to enumerate realms
     */
    @Inject
    public RadiusMonitorPool(ServiceConfigManagerFactory configManagerFactory,
            MessageAuthenticatorCalculator messageAuthenticatorCalculator,
            AnnotatedServiceRegistry annotatedServiceRegistry,
            SecretReferenceCache secretReferenceCache,
            RealmLookup realmLookup) {
        this.messageAuthenticatorCalculator = messageAuthenticatorCalculator;
        this.annotatedServiceRegistry = annotatedServiceRegistry;
        this.secretReferenceCache = secretReferenceCache;
        this.timerPool = SystemTimerPool.getTimerPool();
        this.realmLookup = realmLookup;
        try {
            ServiceConfigManager serviceConfigManager = configManagerFactory.create(SERVICE_NAME);
            serviceConfigManager.addListener(this);
            initializeMonitorsForExistingNodes(serviceConfigManager);
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException("Unable to create RADIUSMonitorPool", e);
        }
    }

    /**
     * Removes and cancels a monitor by UUID.
     * This should be called when a connection will no longer be used.
     *
     * @param uuid the connection UUID
     */
    public void remove(UUID uuid) {
        RadiusMonitor monitor = monitors.remove(uuid);
        if (monitor != null) {
            logger.debug("Removing and cancelling RADIUSMonitor for UUID: {}", uuid);
            monitor.cancel();
        }
    }

    /**
     * Gets an existing monitor without creating a new one.
     *
     * @param uuid the connection UUID
     * @return the monitor, or null if not found
     */
    public RadiusMonitor get(UUID uuid) {
        return monitors.get(uuid);
    }

    /**
     * Checks if a monitor exists for the given UUID.
     *
     * @param uuid the connection UUID
     * @return true if a monitor exists, false otherwise
     */
    public boolean contains(UUID uuid) {
        return monitors.containsKey(uuid);
    }

    /**
     * Gets the number of monitors in the pool.
     *
     * @return the number of monitors
     */
    public int size() {
        return monitors.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schemaChanged(String serviceName, String version) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void globalConfigChanged(
            String serviceName, String version, String groupName, String serviceComponent, int type) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        if (!SERVICE_NAME.equals(serviceName)) {
            return;
        }

        Optional<UUID> uuidOpt = extractUuid(serviceComponent);
        if (uuidOpt.isEmpty()) {
            logger.debug("Skipping RADIUS monitor update for unrecognised service component: {}", serviceComponent);
            return;
        }

        Optional<Realm> realmOpt = resolveRealm(orgName);
        if (realmOpt.isEmpty()) {
            logger.warn("Skipping RADIUS monitor update for node {} because realm could not be resolved (orgName={}).",
                    uuidOpt, orgName);
            return;
        }

        Realm realm = realmOpt.get();
        UUID uuid = uuidOpt.get();
        switch (type) {
        case ADDED:
        case MODIFIED:
            loadMonitorSettings(realm, uuid)
                    .ifPresentOrElse(settings -> upsertMonitor(uuid, settings), () -> {
                        logger.warn("Unable to configure RADIUS monitor for node {} in realm {}.", uuid,
                                realm.asPath());
                        remove(uuid);
                    });
            break;
        case REMOVED:
            remove(uuid);
            logger.debug("Removed RADIUSMonitor for node {} in realm {}", uuid, realm.asPath());
            break;
        default:
            logger.warn("RADIUSMonitorPool received unknown config change type {} for node {} in realm {}",
                    type, uuid, realm.asPath());
            break;
        }
    }

    private Optional<Realm> resolveRealm(String orgName) {
        try {
            if (orgName == null || orgName.isBlank()) {
                return Optional.of(Realms.root());
            }
            return Optional.of(Realms.of(orgName));
        } catch (RealmLookupException e) {
            logger.warn("Failed to resolve realm for organization {}", orgName, e);
            return Optional.empty();
        }
    }

    private Optional<UUID> extractUuid(String serviceComponent) {
        if (serviceComponent == null || serviceComponent.isBlank()) {
            return Optional.empty();
        }
        int index = serviceComponent.lastIndexOf('/') + 1;
        String candidate = index > 0 ? serviceComponent.substring(index) : serviceComponent;
        try {
            return Optional.of(UUID.fromString(candidate));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Loads the monitor settings for the given realm and node UUID.
     *
     * @param realm the realm
     * @param uuid  the node UUID
     * @return an Optional containing the MonitorSettings, or empty if loading failed
     */
    private Optional<MonitorSettings> loadMonitorSettings(Realm realm, UUID uuid) {
        try {
            Optional<RadiusDecisionNode.Config> configOpt = annotatedServiceRegistry
                    .getRealmInstance(RadiusDecisionNode.Config.class, realm, uuid.toString());
            if (configOpt.isEmpty()) {
                logger.debug("No RadiusDecisionNode configuration found for node {} in realm {}", uuid,
                        realm.asPath());
                return Optional.empty();
            }

            RadiusDecisionNode.Config config = configOpt.get();
            Optional<String> secretOpt = resolveSharedSecret(realm, config);
            return secretOpt.map(s -> new MonitorSettings(realm, s, config.timeout(),
                    config.healthCheckInterval(), config.requireMessageAuthenticator()));

        } catch (SMSException | SSOException | IllegalStateException e) {
            logger.warn("Failed to load RadiusDecisionNode configuration for node {} in realm {}", uuid,
                    realm.asPath(), e);
            return Optional.empty();
        }
    }

    /**
     * Resolves the shared secret for the given realm and configuration.
     *
     * @param realm  the realm
     * @param config the RADIUS decision node configuration
     * @return an Optional containing the shared secret, or empty if not found
     */
    private Optional<String> resolveSharedSecret(Realm realm, RadiusDecisionNode.Config config) {
        Optional<Purpose<GenericSecret>> purposeOpt = config.sharedSecretPurpose();
        try {
            return purposeOpt.map(p -> secretReferenceCache.realm(realm).active(purposeOpt.get()))
                    .map(SecretReference::getAsync)
                    .map(promise -> promise.then(s -> s.revealAsUtf8(String::new)))
                    .map(promise -> promise.thenCatch(ex -> {
                        logger.debug("Failed to get secret from store ", ex);
                        return null;
                    }))
                    .map(Promise::getOrThrowIfInterrupted);
        } catch (IllegalStateException e) {
            logger.debug("Failed to resolve shared secret for realm {}", realm.asPath(), e);
            return Optional.empty();
        }
    }

    /**
     * Inserts or updates a monitor in the pool.
     *
     * @param uuid     the connection UUID
     * @param settings the monitor settings
     */
    private void upsertMonitor(UUID uuid, MonitorSettings settings) {
        monitors.compute(uuid, (id, existing) -> {
            MessageAuthenticatorCalculator calculator = settings.requireMessageAuthenticator()
                    ? messageAuthenticatorCalculator : null;
            if (existing == null) {
                logger.debug("Creating RADIUSMonitor for node {} in realm {}", uuid, settings.realm().asPath());
                return new RadiusMonitor(settings.secret(), settings.timeout(), settings.healthCheckInterval(),
                        calculator, timerPool);
            }
            logger.debug("Updating RADIUSMonitor for node {} in realm {}", uuid, settings.realm().asPath());
            existing.updateSettings(settings.secret(), settings.timeout(), settings.healthCheckInterval(), calculator);
            return existing;
        });
    }

    /**
     * Initializes monitors for all existing RADIUS decision nodes across all realms.
     * This is used to ensure monitors are available after a restart.
     *
     * @param scm the service config manager
     */
    private void initializeMonitorsForExistingNodes(ServiceConfigManager scm) {
        // Load monitors for the root realm
        initializeMonitorsForRealm(scm, Realms.root(), Realms.root().asPath());

        // Load monitors for all other realms
        Set<String> realmNames = Optional.ofNullable(realmLookup.getRealmNames()).orElse(Collections.emptySet());
        for (String realmName : realmNames) {
            if (realmName == null || realmName.isBlank() || "/".equals(realmName)) {
                continue;
            }
            try {
                Realm realm = realmLookup.lookup(realmName);
                if (realm == null) {
                    logger.warn("Skipping RADIUS monitor preload because lookup returned null for realm {}.",
                            realmName);
                    continue;
                }
                initializeMonitorsForRealm(scm, realm, realm.asPath());
            } catch (RealmLookupException e) {
                logger.warn("Failed to lookup realm {} while initializing RADIUS monitors", realmName, e);
            }
        }
    }

    /**
     * Loads existing monitors for a specific realm.
     *
     * @param scm       the service config manager
     * @param realm     the realm
     * @param realmName the name of the realm
     */
    private void initializeMonitorsForRealm(ServiceConfigManager scm, Realm realm, String realmName) {
        try {
            ServiceConfig realmConfig = scm.getOrganizationConfig(realmName, null);
            if (realmConfig == null || !realmConfig.exists()) {
                return;
            }

            for (String subConfigName : realmConfig.getSubConfigNames()) {
                extractUuid(subConfigName)
                        .ifPresent(uuid -> loadMonitorSettings(realm, uuid)
                                .ifPresent(settings -> upsertMonitor(uuid, settings)));
            }
        } catch (SMSException | SSOException e) {
            logger.warn("Failed to preload RADIUS monitors for realm {}", realm.asPath(), e);
        }
    }
}
