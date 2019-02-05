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
 * Copyright 2017-2018 ForgeRock AS.
 */
import PropTypes from "prop-types";
import { checkedActionHandlers } from "../../../../../../../../utils";
import { createAction } from "redux-actions";

// Types
const UPDATE_DIMENSIONS = "local/config/realm/authentication/trees/current/nodes/measurements/UPDATE_DIMENSIONS";
const UPDATE_POSITION = "local/config/realm/authentication/trees/current/nodes/measurements/UPDATE_POSITION";

// Actions
export const updateDimensions = createAction(UPDATE_DIMENSIONS);
export const updatePosition = createAction(UPDATE_POSITION);

// PropType
export const propType = {
    measurements: PropTypes.objectOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        height: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired
    })).isRequired
};

// Reducer
const initialState = {};
const initialMeasurementState = { height: 0, width: 0, x: 0, y: 0 };
export default checkedActionHandlers(propType, {
    [UPDATE_DIMENSIONS]: (state, action) => ({
        ...state,
        [action.payload.id]: {
            ...initialMeasurementState,
            ...state[action.payload.id],
            height: action.payload.height,
            id: action.payload.id,
            width: action.payload.width
        }
    }),
    [UPDATE_POSITION]: (state, action) => ({
        ...state,
        [action.payload.id]: {
            ...initialMeasurementState,
            ...state[action.payload.id],
            id: action.payload.id,
            x: action.payload.x,
            y: action.payload.y
        }
    })
}, initialState, (state) => ({ measurements: state }));