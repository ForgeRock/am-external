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
 * Copyright 2017 ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { omit } from "lodash";

// Types
const ADD_OR_UPDATE_NODE = "remote/authentication/trees/current/tree/ADD_OR_UPDATE_NODE";
const REMOVE_NODE = "remote/authentication/trees/current/tree/REMOVE_NODE";
const SET_NODES = "remote/authentication/trees/current/tree/SET_NODES";

// Actions
export const addOrUpdateNode = createAction(ADD_OR_UPDATE_NODE);
export const removeNode = createAction(REMOVE_NODE);
export const setNodes = createAction(SET_NODES);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE_NODE]: (state, action) => ({
        ...state,
        ...action.payload
    }),
    [REMOVE_NODE]: (state, action) => omit(state, [action.payload]),
    [SET_NODES]: (state, action) => action.payload
}, initialState);
