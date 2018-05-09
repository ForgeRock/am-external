/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { chain, omit } from "lodash";

// Types
const ADD_OR_UPDATE = "remote/config/realm/authentication/trees/list/ADD_OR_UPDATE";
const REMOVE = "remote/config/realm/authentication/trees/list/REMOVE";
const SET = "remote/config/realm/authentication/trees/list/SET";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);
export const remove = createAction(REMOVE);
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE]: (state, action) => ({
        ...state,
        [action.payload._id]: omit(action.payload, "nodes")
    }),
    [REMOVE]: (state, action) => omit(state, (instance) => instance._id === action.payload),
    [SET]: (state, action) =>
        chain(action.payload)
            .map((tree) => omit(tree, "nodes"))
            .indexBy("_id")
            .value()
}, initialState);
