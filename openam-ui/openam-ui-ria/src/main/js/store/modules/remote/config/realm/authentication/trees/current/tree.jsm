/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { omit } from "lodash";

// Types
const ADD_OR_UPDATE_NODE = "remote/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_NODE";
const REMOVE_NODE = "remote/config/realm/authentication/trees/current/tree/REMOVE_NODE";
const SET_NODES = "remote/config/realm/authentication/trees/current/tree/SET_NODES";

// Actions
export const addOrUpdateNode = createAction(ADD_OR_UPDATE_NODE);
export const removeNode = createAction(REMOVE_NODE);
export const setNodes = createAction(SET_NODES);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE_NODE]: (state, action) => ({
        ...state,
        ...action.payload
    }),
    [REMOVE_NODE]: (state, action) => omit(state, [action.payload]),
    [SET_NODES]: (state, action) => action.payload
}, initialState);
