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
const ADD_REALM = "remote/realms/ADD_REALM";
const REMOVE_REALM = "remote/realms/REMOVE_REALM";
const SET_REALMS = "remote/realms/SET_REALMS";

// Actions
export const addRealm = createAction(ADD_REALM);
export const removeRealm = createAction(REMOVE_REALM);
export const setRealms = createAction(SET_REALMS);

// Reducer
const initialState = {};
const createPath = (parentPath, name) => {
    if (parentPath === "/") {
        return parentPath + name;
    } else if (parentPath) {
        return `${parentPath}/${name}`;
    } else {
        return "/";
    }
};
const addPayloadPath = (payload) => ({
    ...payload,
    path: createPath(payload.parentPath, payload.name)
});
const addPathToAction = (handler) => (state, action) => {
    action.payload = addPayloadPath(action.payload);

    return handler(state, action);
};
export default handleActions({
    [ADD_REALM]: addPathToAction((state, action) => ({
        ...state,
        [action.payload.path]: {
            ...action.payload
        }
    })),
    [REMOVE_REALM]: addPathToAction((state, action) => omit(state, action.payload.path)),
    [SET_REALMS]: (state, action) => chain(action.payload)
        .map(addPayloadPath)
        .indexBy("path")
        .value()
}, initialState);
