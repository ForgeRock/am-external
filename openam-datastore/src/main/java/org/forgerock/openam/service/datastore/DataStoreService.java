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

import org.forgerock.opendj.ldap.Connection;

/**
 * Data store service configuration interface.
 *
 * @since 6.0.0
 */
public interface DataStoreService {

    /**
     * Gets the Default DataStore Connection.
     * @return the default datastore connection
     * @throws DataStoreException if the default connection is not configured
     */
    Connection getDefaultConnection();

    /**
     * Gets the Datastore from the given dataStore ID.
     * @param dataStoreId the id of the datastore to return
     * @return the Datastore requested
     * @throws DataStoreException if the datastore ID is not found
     */
    Connection getConnection(String dataStoreId);

}
