/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const ADD_OR_UPDATE = "local/config/realm/identities/groups/template/ADD_OR_UPDATE";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);

// Reducer
const initialState = null;
export default handleActions({
    [ADD_OR_UPDATE]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    })
}, initialState);
