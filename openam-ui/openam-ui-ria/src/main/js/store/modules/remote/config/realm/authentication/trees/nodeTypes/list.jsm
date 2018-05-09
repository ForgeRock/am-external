/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { indexBy } from "lodash";

// Types
const SET = "remote/config/realm/authentication/trees/nodeTypes/list/SET";

// Actions
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [SET]: (state, action) => indexBy(action.payload, "_id")
}, initialState);
