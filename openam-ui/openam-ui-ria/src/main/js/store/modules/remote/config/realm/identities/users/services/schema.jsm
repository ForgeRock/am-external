/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const SET_SCHEMA = "remote/config/realm/identities/users/services/schema/SET_SCHEMA";

// Actions
export const setSchema = createAction(SET_SCHEMA,
    (payload) => payload, (payload, serviceId) => ({ serviceId }));

// Reducer
const initialState = null;
export default handleActions({
    [SET_SCHEMA]: (state, action) => ({
        ...state,
        [action.meta.serviceId]: action.payload
    })
}, initialState);
