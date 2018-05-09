/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static org.forgerock.json.JsonValue.json;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthException;

/**
 * DataStore implementation using Auth Module's shared state.
 *
 * @see DataStore
 */
class SharedStateDataStore implements DataStore {

    private static final String SHARED_STATE_KEY_PREFIX = "DATA_STORE_";

    private final String id;
    private final String provider;
    private final Map sharedState;

    /**
     * Constructs a SharedStateDataStore instance.
     *
     * @param id The id of the data store instance.
     * @param provider The social provider for which this data store is used.
     * @param sharedState The shared state received from the Auth Module.
     */
    public SharedStateDataStore(String id, String provider, Map sharedState) {
        this.id = id;
        this.provider = provider;
        this.sharedState = sharedState;
    }

    /**
     * @return id of the data store.
     */
    public String getId() {
        return id;
    }

    @Override
    public String getProvider() throws OAuthException {
        return provider;
    }

    @Override
    public void storeData(JsonValue jsonValue) throws OAuthException {
        sharedState.put(SHARED_STATE_KEY_PREFIX + id, jsonValue.getObject());
    }

    @Override
    public JsonValue retrieveData() {
        JsonValue state = json(sharedState.get(SHARED_STATE_KEY_PREFIX + id));
        state.put("provider", provider);
        return state;
    }
}
