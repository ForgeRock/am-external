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

import static com.sun.identity.shared.Constants.LDAP_CONN_IDLE_TIME_IN_SECS;

import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.opendj.ldap.ConnectionFactory;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DefaultDataStoreConfigurationManager;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;

/**
 * Singleton service to allow the dependency injection of a Connection Factory Service
 */
@Singleton
final class DefaultConnectionFactoryProvider implements Provider<ConnectionFactory> {

    /**
     * Gets the Connection Factory instance managed by this Service.
     *
     * @return the ConnectionFactory instance
     */
    public ConnectionFactory get() {
        try {
            DefaultDataStoreConfigurationManager defaultConfiguration = DefaultDataStoreConfigurationManager
                    .getDataStoreConfigurationManager();

            ServerGroup smsServerGroup = defaultConfiguration.getServerGroup("sms");

            String serverGroupId;
            ServerInstance serverInstance;

            if (smsServerGroup != null) {
                serverGroupId = "sms";
                serverInstance = smsServerGroup.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            } else {
                serverGroupId = "default";
                serverInstance = defaultConfiguration.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            }

            if (serverInstance == null) {
                throw new DataStoreException("Could not find server instance.");
            }

            int idleTimeout = SystemProperties.getAsInt(LDAP_CONN_IDLE_TIME_IN_SECS, 0);
            int poolMin = serverInstance.getMinConnections();
            int poolMax = serverInstance.getMaxConnections();

            return defaultConfiguration.getNewConnectionPool(serverGroupId, LDAPUser.Type.AUTH_ADMIN, poolMin, poolMax,
                    idleTimeout, TimeUnit.SECONDS);
        } catch (LDAPServiceException e) {
            throw new DataStoreException("Unable to create default connection factory", e);
        }
    }

}