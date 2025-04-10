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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { createAction, handleActions } from "redux-actions";
import { keyBy, omitBy } from "lodash";

// Types
const ADD = "remote/config/realm/authentication/nodes/instances/ADD";
const REMOVE = "remote/config/realm/authentication/nodes/instances/REMOVE";
const SET = "remote/config/realm/authentication/nodes/instances/SET";

// Actions
export const add = createAction(ADD);
export const remove = createAction(REMOVE);
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [ADD]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    }),
    [REMOVE]: (state, action) => omitBy(state, (instance) => instance._id === action.payload),
    [SET]: (state, action) => keyBy(action.payload, "_id")
}, initialState);
