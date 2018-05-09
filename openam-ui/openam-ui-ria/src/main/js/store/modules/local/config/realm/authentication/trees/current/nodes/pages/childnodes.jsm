/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createAction } from "redux-actions";
import { omit } from "lodash";
import PropTypes from "prop-types";
import { checkedActionHandlers } from "store/utils";

// Types
const ADD = "local/config/realm/authentication/trees/current/nodes/pages/childnodes/ADD";
const REMOVE = "local/config/realm/authentication/trees/current/nodes/pages/childnodes/REMOVE";

// Actions
export const add = createAction(ADD, (key, value) => ({ [key]: value }));
export const remove = createAction(REMOVE);

export const propType = {
    childnodes: PropTypes.objectOf(PropTypes.string.isRequired).isRequired
};

// Reducer
const initialState = {};
export default checkedActionHandlers(propType, {
    [ADD]: (state, action) => {
        return ({
            ...state,
            ...action.payload
        });
    },
    [REMOVE]: (state, action) => omit(state, action.payload)
}, initialState, (state) => ({ childnodes: state }));
