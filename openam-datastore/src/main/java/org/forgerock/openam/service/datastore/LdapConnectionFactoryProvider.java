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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.Constants.*;
import static org.forgerock.openam.ldap.LDAPUtils.LDAP_SECURE_PROTOCOLS;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.opendj.ldap.LdapClients.newCachedConnectionPool;
import static org.forgerock.opendj.ldap.LdapClients.newLdapClient;
import static org.forgerock.opendj.ldap.LdapConnectionFactory.*;
import static org.forgerock.opendj.security.SslOptions.*;
import static org.forgerock.util.time.Duration.duration;

import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapClient;
import org.forgerock.opendj.ldap.LdapConnectionFactory;
import org.forgerock.opendj.security.SslOptions;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Provider used to deliver up ldap connection factory instances.
 *
 * @since 6.5.0
 */
class LdapConnectionFactoryProvider {

    private static final Logger logger = LoggerFactory.getLogger(LdapDataStoreService.class);
    private static final Option<SslOptions> SSL_OPTIONS = Option.of(SslOptions.class, null);

    private static final int HEARTBEAT_INTERVAL_DEFAULT = 10;
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 3;

    ConnectionFactory createLdapConnectionFactory(DataStoreConfig config) {
        Options ldapOptions = populateLdapOptions(config);
        LdapClient basicClient = newLdapClient(config.getHostname(), config.getPort(), ldapOptions);

        int idleTimeout = SystemProperties.getAsInt(LDAP_CONN_IDLE_TIME_IN_SECS, 0);
        LdapClient pooledClient = newCachedConnectionPool(basicClient, config.getMinimumConnectionPool(),
                config.getMaximumConnectionPool(), idleTimeout, TimeUnit.SECONDS);

        return new LdapConnectionFactory(pooledClient);
    }

    private Options populateLdapOptions(DataStoreConfig config) {
        Options options = Options.defaultOptions().set(SSL_USE_STARTTLS, true);

        if (config.isUseSsl() || config.isUseStartTLS()) {
            try {
                options.set(SSL_OPTIONS,
                        newSslOptions(USE_EMPTY_KEY_MANAGER, USE_JVM_TRUST_MANAGER)
                                .enabledProtocols(LDAP_SECURE_PROTOCOLS));
            } catch (GeneralSecurityException e) {
                logger.error("An error occurred while creating the SSL context", e);
            }
        }

        int heartBeatInterval = SystemProperties.getAsInt(LDAP_SM_HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL_DEFAULT);
        int heartBeatTimeout = SystemPropertiesManager.getAsInt(LDAP_HEARTBEAT_TIMEOUT, DEFAULT_HEARTBEAT_TIMEOUT);
        TimeUnit heartBeatTimeUnit = TimeUnit.valueOf(
                SystemProperties.get(LDAP_SM_HEARTBEAT_TIME_UNIT, TimeUnit.SECONDS.toString()));

        if (heartBeatTimeout > 0 && heartBeatInterval > 0 && heartBeatTimeUnit != null) {
            options = options
                    .set(HEARTBEAT_ENABLED, true)
                    .set(HEARTBEAT_INTERVAL, duration(heartBeatTimeUnit.toSeconds(heartBeatInterval), TimeUnit.SECONDS))
                    .set(HEARTBEAT_TIMEOUT, duration(heartBeatTimeUnit.toSeconds(heartBeatTimeout), TimeUnit.SECONDS));
        } else {
            logger.debug("Heartbeat disabled. Heartbeat Interval: {}, Heartbeat Timeout: {}",
                    heartBeatInterval, heartBeatTimeout);
        }

        if (!isBlank(config.getBindDN())) {
            options.set(AUTHN_BIND_REQUEST,
                    LDAPRequests.newSimpleBindRequest(
                            config.getBindDN(), config.getBindPassword().toCharArray()));
        }

        return options;
    }

}
