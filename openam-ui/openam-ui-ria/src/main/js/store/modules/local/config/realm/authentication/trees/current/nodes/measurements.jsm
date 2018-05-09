/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
