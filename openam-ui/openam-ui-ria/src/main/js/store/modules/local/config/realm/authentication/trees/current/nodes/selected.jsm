/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction } from "redux-actions";
import PropTypes from "prop-types";
import { checkedActionHandlers } from "../../../../../../../../utils";

// Types
const SET = "local/config/realm/authentication/trees/current/nodes/selected/SET";
const REMOVE = "local/config/realm/authentication/trees/current/nodes/selected/REMOVE";

// Actions
export const set = createAction(SET);
export const remove = createAction(REMOVE);

export const propType = {
    selected: PropTypes.oneOfType([
        PropTypes.objectOf((_, key) => new Error(`Should be an empty object, but has property ${key}`)),
        PropTypes.shape({
            id: PropTypes.string.isRequired,
            type: PropTypes.string.isRequired
        })
    ]).isRequired
};

// Reducer
const initialState = {};
export default checkedActionHandlers(propType, {
    [REMOVE]: () => initialState,
    [SET]: (state, action) => action.payload
}, initialState, (state) => ({ selected: state }));
