/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const SET_CREATABLES = "remote/config/realm/identities/users/services/creatables/SET_CREATABLES";

// Actions
export const setCreatables = createAction(SET_CREATABLES,
    (payload) => payload, (payload, userId) => ({ userId }));

// Reducer
const initialState = {};
export default handleActions({
    [SET_CREATABLES]: (state, action) => ({
        ...state,
        [action.meta.userId]: action.payload
    })
}, initialState);
