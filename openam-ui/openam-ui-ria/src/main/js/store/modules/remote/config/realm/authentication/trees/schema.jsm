/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const SET = "remote/config/realm/authentication/trees/schema/SET";

// Actions
export const set = createAction(SET);

// Reducer
const initialState = null;
export default handleActions({
    [SET]: (state, action) => action.payload
}, initialState);
