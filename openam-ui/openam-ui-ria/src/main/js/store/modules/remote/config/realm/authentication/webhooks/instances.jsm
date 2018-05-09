/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { indexBy, omit } from "lodash";

// Types
const ADD = "remote/config/realm/authentication/webhooks/instances/ADD";
const REMOVE = "remote/config/realm/authentication/webhooks/instances/REMOVE";
const SET = "remote/config/realm/authentication/webhooks/instances/SET";

// Actions
export const add = createAction(ADD);
export const remove = createAction(REMOVE);
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [ADD]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    }),
    [REMOVE]: (state, action) => omit(state, (instance) => instance._id === action.payload),
    [SET]: (state, action) => indexBy(action.payload, "_id")
}, initialState);
