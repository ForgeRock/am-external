/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018-2019 ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

// Types
const ADD_OR_UPDATE = "local/config/realm/secretStores/instances/current/mappings/instances/ADD_OR_UPDATE";
const REMOVE = "local/config/realm/secretStores/instances/current/mappings/instances/REMOVE";
const SET = "local/config/realm/secretStores/instances/current/mappings/instances/SET";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);
export const remove = createAction(REMOVE);
export const set = createAction(SET);

// Reducer
const initialState = [];
export default handleActions({
    [ADD_OR_UPDATE]: (state, action) => {
        const newState = state.filter((instance) => {
            return instance._id !== action.payload._id;
        });
        newState.push(action.payload);
        return newState;
    },
    [REMOVE]: (state, action) => state.filter((instance) => instance._id !== action.payload),
    [SET]: (state, action) => action.payload
}, initialState);