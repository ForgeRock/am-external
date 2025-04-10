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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
