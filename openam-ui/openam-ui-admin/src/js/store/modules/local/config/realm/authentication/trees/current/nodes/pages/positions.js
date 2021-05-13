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
 * Copyright 2017-2019 ForgeRock AS.
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
