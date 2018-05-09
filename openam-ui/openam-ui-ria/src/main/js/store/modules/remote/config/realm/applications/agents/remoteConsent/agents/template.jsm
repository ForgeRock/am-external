/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const SET_TEMPLATE = "remote/config/realm/applications/agents/remoteConsent/agents/template/SET_TEMPLATE";

// Actions
export const setTemplate = createAction(SET_TEMPLATE);

// Reducer
const initialState = null;
export default handleActions({
    [SET_TEMPLATE]: (state, action) => action.payload
}, initialState);
