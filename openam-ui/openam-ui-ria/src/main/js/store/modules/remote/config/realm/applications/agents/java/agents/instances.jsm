/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
