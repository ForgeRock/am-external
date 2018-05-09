/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
        public SharedStateDataStore(JsonValue value) {
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
