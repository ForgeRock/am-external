/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const ADD_REALM = "remote/info/ADD_REALM";

// Actions
export const addRealm = createAction(ADD_REALM);

// Reducer
const initialState = {
    realm: undefined
};
export default handleActions({
    [ADD_REALM]: (state, action) => ({
        realm: action.payload.toLowerCase()
    })
}, initialState);
