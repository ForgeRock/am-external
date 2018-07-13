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
import { chain, omit } from "lodash";

// Types
const ADD_OR_UPDATE = "remote/authentication/trees/list/ADD_OR_UPDATE";
const REMOVE = "remote/authentication/trees/list/REMOVE";
const SET = "remote/authentication/trees/list/SET";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);
export const remove = createAction(REMOVE);
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE]: (state, action) => ({
        ...state,
        [action.payload._id]: omit(action.payload, "nodes")
    }),
    [REMOVE]: (state, action) => omit(state, (instance) => instance._id === action.payload),
    [SET]: (state, action) =>
        chain(action.payload)
            .map((tree) => omit(tree, "nodes"))
            .indexBy("_id")
            .value()
}, initialState);
