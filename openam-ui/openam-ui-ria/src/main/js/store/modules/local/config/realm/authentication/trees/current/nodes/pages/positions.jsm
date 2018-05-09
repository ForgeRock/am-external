/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { createAction } from "redux-actions";
import PropTypes from "prop-types";

import { checkedActionHandlers } from "store/utils";

// Types
const ADD = "local/config/realm/authentication/trees/current/nodes/pages/positions/ADD";

// Actions
export const add = createAction(ADD, (key, value) => ({ [key]: value }));

export const propType = {
    positions: PropTypes.objectOf(PropTypes.shape({
        height: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired
    })).isRequired
};

// Reducer
const initialState = {};
export default checkedActionHandlers(propType, {
    [ADD]: (state, action) => ({
        ...state,
        ...action.payload
    })
}, initialState, (state) => ({ positions: state }));
