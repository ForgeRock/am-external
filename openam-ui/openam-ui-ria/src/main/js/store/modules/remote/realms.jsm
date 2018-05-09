/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
