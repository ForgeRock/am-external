/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { omit } from "lodash";
import { createAction, handleActions } from "redux-actions";

// Types
const ADD_OR_UPDATE = "remote/config/realm/authentication/trees/current/nodes/properties/ADD_OR_UPDATE";
const REMOVE = "remote/config/realm/authentication/trees/current/nodes/properties/REMOVE";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);
export const remove = createAction(REMOVE);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    }),
    [REMOVE]: (state, action) => omit(state, action.payload)
}, initialState);
