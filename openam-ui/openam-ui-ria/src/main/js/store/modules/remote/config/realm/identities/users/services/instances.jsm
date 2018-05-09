/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { findWhere, without } from "lodash";

// Types
const REMOVE_INSTANCE = "remote/config/realm/identities/users/services/instances/REMOVE_INSTANCE";
const SET_INSTANCES = "remote/config/realm/identities/users/services/instances/SET_INSTANCES";

// Actions
export const removeInstance = createAction(REMOVE_INSTANCE,
    (payload) => payload, (payload, userId) => ({ userId }));
export const setInstances = createAction(SET_INSTANCES,
    (payload) => payload, (payload, userId) => ({ userId }));

// Reducer
const initialState = {};
export default handleActions({
    [REMOVE_INSTANCE]: (state, action) => {
        const userServices = state[action.meta.userId];

        return {
            ...state,
            [action.meta.userId]: without(userServices, findWhere(userServices, { "_id": action.payload }))
        };
    },
    [SET_INSTANCES]: (state, action) => ({
        ...state,
        [action.meta.userId]: action.payload
    })
}, initialState);
