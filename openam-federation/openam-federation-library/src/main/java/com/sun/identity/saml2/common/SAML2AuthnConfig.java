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

package com.sun.identity.saml2.common;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * This class provides methods to retrieve authentication configuration from the config store.
 * This also caches the configuration for a realm to avoid repeated lookups. When the configuration changes,
 * the cache is cleared for the realm.
 */
@Singleton
public class SAML2AuthnConfig implements ConfigurationListener {

    private static final Logger logger = LoggerFactory.getLogger(SAML2AuthnConfig.class);
    /**
     * Component name for the authn configuration.
     */
    private static final String AUTHN_CONFIG = "AUTHN";
    /**
     * Configuration key for the realm default service.
     */
    private static final String AUTH_CONFIG_ORG = "iplanet-am-auth-org-config";
    /**
     * Configuration key for the maximum duration of Authentication Sessions for the realm.
     */
    private static final String AUTH_SESSION_MAX_DURATION = "openam-auth-authentication-sessions-max-duration";
    @VisibleForTesting
    final ConcurrentHashMap<String, Map<String, Set<String>>> config = new ConcurrentHashMap<>();
    private final ConfigurationInstance ci;

    /**
     * Default constructor. Initializes the configuration instance and registers a listener.
     */
    public SAML2AuthnConfig() throws SAML2Exception {
        try {
            ci = ConfigurationManager.getConfigurationInstance(AUTHN_CONFIG);
            if (ci == null) {
                logger.error("Failed to load configuration instance");
                throw new SAML2Exception("Failed to load configuration instance");
            }
            ci.addListener(this);
        } catch (ConfigurationException e) {
            logger.error("Failed to load configuration instance", e);
            throw new SAML2Exception(e);
        }
    }

    /**
     * Returns the default service for the realm.
     *
     * @param realm Realm to get the default service for.
     * @return Default service for the realm, or null if not found.
     */
    public String getDefaultServiceForRealm(String realm) {
        Map<String, Set<String>> config = getConfig(realm);
        return config == null ? null : CollectionHelper.getMapAttr(config, AUTH_CONFIG_ORG);
    }

    /**
     * Returns the authentication session's maximum duration in minutes for the given realm.
     *
     * @param realm the realm for which the configuration is to be fetched
     * @return the authentication session's maximum duration in minutes for the given realm, or 0 if not found.
     */
    public int getAuthSessionMaxDurationForRealm(String realm) {
        Map<String, Set<String>> config = getConfig(realm);
        String maxDuration = config == null ? null : CollectionHelper.getMapAttr(config, AUTH_SESSION_MAX_DURATION);
        if (maxDuration == null) {
            logger.debug("Configuration for {} in realm {} was null", AUTH_SESSION_MAX_DURATION, realm);
            return 0;
        }
        return Integer.parseInt(maxDuration);
    }

    /**
     * Returns the authentication session's maximum duration in seconds for the given realm.
     *
     * @param realm the realm for which the configuration is to be fetched
     * @return the authentication session's maximum duration in seconds for the given realm, or 0 if not found.
     */
    public int getAuthSessionMaxDurationInSecondsForRealm(String realm) {
        return getAuthSessionMaxDurationForRealm(realm) * 60;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void configChanged(ConfigurationActionEvent e) {
        logger.debug("Configuration changed, clearing cache");
        try {
            if (e.getRealm() != null) {
                config.remove(Realms.of(e.getRealm()).asPath());
            }
        } catch (RealmLookupException ex) {
            logger.error("Failed to lookup the realm", ex);
        }
    }

    /**
     * Fetches the authn configuration for the given realm.
     *
     * @param realm the realm for which the configuration is to be fetched
     * @return the configuration for the given realm, or null if no configuration found.
     */
    private Map<String, Set<String>> getConfig(String realm) {
        return config.computeIfAbsent(realm, r -> {
            Map<String, Set<String>> config;
            try {
                config = ci.getConfiguration(r, null);
            } catch (ConfigurationException e) {
                logger.error("Failed to fetch configuration for realm: {}", r, e);
                return null;
            }
            if (config == null || config.isEmpty()) {
                // config will be null in e.g. fedlet case
                logger.info("Configuration for {} in realm {} was null or empty", AUTHN_CONFIG, r);
                return null;
            }
            return config;
        });
    }
}
