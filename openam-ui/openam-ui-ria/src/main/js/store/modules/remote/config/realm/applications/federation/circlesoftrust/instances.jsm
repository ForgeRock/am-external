/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { indexBy, omit } from "lodash";

// Types
const ADD_INSTANCE = "remote/config/realm/applications/federation/circlesoftrust/instances/ADD_INSTANCE";
const REMOVE_INSTANCE = "remote/config/realm/applications/federation/circlesoftrust/instances/REMOVE_INSTANCE";
const SET_INSTANCES = "remote/config/realm/applications/federation/circlesoftrust/instances/SET_INSTANCES";

// Actions
export const addInstance = createAction(ADD_INSTANCE);
export const removeInstance = createAction(REMOVE_INSTANCE);
export const setInstances = createAction(SET_INSTANCES);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_INSTANCE]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    }),
    [REMOVE_INSTANCE]: (state, action) => omit(state, action.payload._id),
    [SET_INSTANCES]: (state, action) => indexBy(action.payload, "_id")
}, initialState);
