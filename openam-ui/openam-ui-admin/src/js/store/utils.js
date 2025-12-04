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
 * Copyright 2018-2025 Ping Identity Corporation.
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
