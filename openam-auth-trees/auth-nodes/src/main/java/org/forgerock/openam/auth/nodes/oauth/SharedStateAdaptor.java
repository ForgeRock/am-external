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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oauth;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthException;
import org.forgerock.openam.auth.node.api.NodeProcessException;

/**
 * Adaptor between {@code Datasotre} and {@code JsonValue}.
 */
public final class SharedStateAdaptor {

    private SharedStateAdaptor() {
        // utility class.
    }

    /**
     * Takes a datastore and returns its json representation.
     * @param dataStore the datastore
     * @return a the json representation of this datastore
     * @throws NodeProcessException when an error occurs while retrieving the data.
     */
    public static JsonValue fromDatastore(DataStore dataStore) throws NodeProcessException {
        if (dataStore == null) {
            return null;
        }
        try {
            return dataStore.retrieveData();
        } catch (OAuthException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Takes a jsonValue and return a {@code Datastore} instance representing that jsonValue.
     * @param sharedState the sharedState as json
     * @return the shared state as Datastore
     */
    public static DataStore toDatastore(JsonValue sharedState) {
        if (sharedState == null) {
            return null;
        }
        return new SharedStateDataStore(sharedState);
    }

    /**
     * DataStore implementation using Auth Nodes's shared state.
     *
     * @see DataStore
     */
    static class SharedStateDataStore implements DataStore {

        private JsonValue value;

        /**
         * Constructs a SharedStateDataStore instance.
         *
         * @param value The value.
         */
        SharedStateDataStore(JsonValue value) {
            this.value = value;
        }

        @Override
        public String getProvider() throws OAuthException {
            return "";
        }

        @Override
        public void storeData(JsonValue jsonValue) throws OAuthException {
            value.asMap().putAll(jsonValue.asMap());
        }

        @Override
        public JsonValue retrieveData() {
            return value;
        }
    }
}
