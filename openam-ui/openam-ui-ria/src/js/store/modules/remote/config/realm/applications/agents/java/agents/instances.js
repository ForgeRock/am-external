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
 * Copyright 2017-2018 ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { indexBy } from "lodash";

// Types
const ADD_INSTANCE = "remote/config/realm/applications/agents/java/agents/instances/ADD_INSTANCE";
const SET_INSTANCES = "remote/config/realm/applications/agents/java/agents/instances/SET_INSTANCES";

// Actions
export const addInstance = createAction(ADD_INSTANCE);
export const setInstances = createAction(SET_INSTANCES);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_INSTANCE]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    }),
    [SET_INSTANCES]: (state, action) => indexBy(action.payload, "_id")
}, initialState);
