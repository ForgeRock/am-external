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

// Types
const SET = "local/authentication/trees/current/nodes/selected/SET";
const REMOVE = "local/authentication/trees/current/nodes/selected/REMOVE";

// Actions
export const set = createAction(SET);
export const remove = createAction(REMOVE);

// Reducer
const initialState = {};
export default handleActions({
    [REMOVE]: () => initialState,
    [SET]: (state, action) => action.payload
}, initialState);
