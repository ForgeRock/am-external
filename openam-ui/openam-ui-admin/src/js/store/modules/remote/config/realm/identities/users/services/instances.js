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
import { find, without } from "lodash";

// Types
const REMOVE_INSTANCE = "remote/config/realm/identities/users/services/instances/REMOVE_INSTANCE";
const SET_INSTANCES = "remote/config/realm/identities/users/services/instances/SET_INSTANCES";

// Actions
export const removeInstance = createAction(REMOVE_INSTANCE,
    (payload) => payload, (payload, userId) => ({ userId }));
export const setInstances = createAction(SET_INSTANCES,
    (payload) => payload, (payload, userId) => ({ userId }));

// Reducer
const initialState = {};
export default handleActions({
    [REMOVE_INSTANCE]: (state, action) => {
        const userServices = state[action.meta.userId];

        return {
            ...state,
            [action.meta.userId]: without(userServices, find(userServices, { "_id": action.payload }))
        };
    },
    [SET_INSTANCES]: (state, action) => ({
        ...state,
        [action.meta.userId]: action.payload
    })
}, initialState);
