/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import { handleActions } from "redux-actions";
import { mapValues } from "lodash";

export function checkedActionResult (propType, action, oldState, newState, stateMapper = (state) => state) {
    PropTypes.checkPropTypes(propType, stateMapper(oldState), "old store state", action.type);
    PropTypes.checkPropTypes(propType, stateMapper(newState), "new store state", action.type);
    return newState;
}

export function checkedActionHandlers (propType, handlers, initialState, stateMapper = (state) => state) {
    return handleActions(mapValues(handlers, (f) => function (state, action) {
        return checkedActionResult(propType, action, state, f(...arguments), stateMapper);
    }), initialState);
}
