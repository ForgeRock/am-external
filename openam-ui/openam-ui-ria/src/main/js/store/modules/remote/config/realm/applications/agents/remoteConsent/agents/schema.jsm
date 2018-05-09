/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const SET_SCHEMA = "remote/config/realm/applications/agents/remoteConsent/agents/schema/SET_SCHEMA";

// Actions
export const setSchema = createAction(SET_SCHEMA);

// Reducer
const initialState = null;
export default handleActions({
    [SET_SCHEMA]: (state, action) => action.payload
}, initialState);
