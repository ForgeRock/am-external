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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.Constants.LDAP_CONN_IDLE_TIME_IN_SECS;
import static org.forgerock.openam.ldap.LDAPUtils.CACHED_POOL_OPTIONS;
import static org.forgerock.openam.ldap.LDAPUtils.newFailoverConnectionFactory;
import static org.forgerock.opendj.ldap.LdapClients.LDAP_CLIENT_REQUEST_TIMEOUT;

import java.util.concurrent.TimeUnit;

import org.forgerock.openam.ldap.ConnectionConfig;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LDAPUtils.CachedPoolOptions;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapConnectionFactory;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

import com.google.inject.Inject;
import com.iplanet.am.util.SystemProperties;

/**
 * Provider used to deliver up ldap connection factory instances.
 *
 * @since 6.5.0
 */
public class LdapConnectionFactoryProvider {

    private final Secrets secrets;

    @Inject
    private LdapConnectionFactoryProvider(Secrets secrets) {
        this.secrets = secrets;
    }

    /**
     * Create a new LDAP connection factory. The connection factory will not be managed or cached and the caller
     * must close the factory when its no longer in use.
     *
     * @param config The details required to establish a Connection to an LDAP server.
     * @return A new {@link ConnectionFactory}.
     */
    public ConnectionFactory createLdapConnectionFactory(ConnectionConfig config) {
        Options ldapOptions = populateLdapOptions(config);
        if (config.isMtlsEnabled()) {
            return newFailoverConnectionFactory(config, ldapOptions, secrets);
        }

        return newFailoverConnectionFactory(config.getLDAPURLs(), config.getBindDN(), config.getBindPassword(),
                config.getLdapHeartbeat(), TimeUnit.SECONDS.toString(), config.isStartTLSEnabled(), false,
                ldapOptions);
    }

    private Options populateLdapOptions(ConnectionConfig config) {
        int idleTimeout = SystemProperties.getAsInt(LDAP_CONN_IDLE_TIME_IN_SECS, 0);
        int timeout = SystemProperties.getAsInt(DataLayerConstants.DATA_LAYER_TIMEOUT, 10);
        return Options.defaultOptions()
                .set(LDAP_CLIENT_REQUEST_TIMEOUT, Duration.duration(timeout, TimeUnit.SECONDS))
                .set(CACHED_POOL_OPTIONS, new CachedPoolOptions(config.getMinConnections(),
                        config.getMaxConnections(), idleTimeout, TimeUnit.SECONDS))
                .set(LDAPUtils.AFFINITY_ENABLED, config.isAffinityEnabled())
                .set(LdapConnectionFactory.SSL_USE_STARTTLS, config.isStartTLSEnabled());
    }
}
