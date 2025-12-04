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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.service.datastore;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.forgerock.openam.services.datastore.DataStoreException;
import org.forgerock.opendj.ldap.ConnectionFactory;

import com.iplanet.services.ldap.DataStoreConfigurationManager;
import com.iplanet.services.ldap.LDAPServiceException;

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
            DataStoreConfigurationManager defaultConfiguration =
                    DataStoreConfigurationManager.getInstance();
            return defaultConfiguration.getNewSMSConnectionFactory();
        } catch (LDAPServiceException e) {
            throw new DataStoreException("Unable to create default connection factory", e);
        }
    }

}
