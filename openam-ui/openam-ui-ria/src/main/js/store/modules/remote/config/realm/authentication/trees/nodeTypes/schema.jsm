/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const ADD_OR_UPDATE_SCHEMA = "remote/config/realm/authentication/trees/nodeTypes/schema/ADD_OR_UPDATE_SCHEMA";

// Actions
export const addOrUpdateSchema = createAction(ADD_OR_UPDATE_SCHEMA,
    (payload) => payload, (payload, nodeType) => ({ nodeType }));

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE_SCHEMA]: (state, action) => ({
        ...state,
        [action.meta.nodeType]: action.payload
    })
}, initialState);
