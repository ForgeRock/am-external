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
 * Copyright 2018-2021 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.shared.Constants.LDAP_CONN_IDLE_TIME_IN_SECS;
import static com.sun.identity.shared.Constants.LDAP_SM_HEARTBEAT_INTERVAL;
import static org.forgerock.openam.ldap.LDAPUtils.CACHED_POOL_OPTIONS;
import static org.forgerock.openam.ldap.LDAPUtils.newFailoverConnectionFactory;
import static org.forgerock.openam.utils.SchemaAttributeUtils.stripAttributeNameFromValue;
import static org.forgerock.opendj.ldap.LdapConnectionFactory.REQUEST_TIMEOUT;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LDAPUtils.CachedPoolOptions;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

import com.iplanet.am.util.SystemProperties;

/**
 * Provider used to deliver up ldap connection factory instances.
 *
 * @since 6.5.0
 */
class LdapConnectionFactoryProvider {

    private static final int HEARTBEAT_INTERVAL_DEFAULT = 10;

    ConnectionFactory createLdapConnectionFactory(DataStoreConfig config) {
        Options ldapOptions = populateLdapOptions(config);
        Set <LDAPURL> ldapUrls = LDAPUtils.getLdapUrls(config.getLDAPURLs(), config.isUseSsl());
        int heartBeatInterval = SystemProperties.getAsInt(LDAP_SM_HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL_DEFAULT);
        return newFailoverConnectionFactory(ldapUrls, config.getBindDN(), config.getBindPassword(),
                heartBeatInterval, TimeUnit.SECONDS.toString(), config.isStartTLSEnabled(), false, ldapOptions);
    }

    private Options populateLdapOptions(DataStoreConfig config) {
        int idleTimeout = SystemProperties.getAsInt(LDAP_CONN_IDLE_TIME_IN_SECS, 0);
        int timeout = SystemProperties.getAsInt(DataLayerConstants.DATA_LAYER_TIMEOUT, 10);
        Options options = Options.defaultOptions()
                .set(REQUEST_TIMEOUT, Duration.duration((long) timeout, TimeUnit.SECONDS))
                .set(CACHED_POOL_OPTIONS, new CachedPoolOptions(config.getMinConnections(),
                        config.getMaxConnections(), idleTimeout, TimeUnit.SECONDS))
                .set(LDAPUtils.AFFINITY_ENABLED, config.isAffinityEnabled());
        return options;
    }
}
