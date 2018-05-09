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
const ADD_OR_UPDATE = "local/config/realm/authentication/trees/current/nodes/properties/ADD_OR_UPDATE";

// Actions
export const addOrUpdate = createAction(ADD_OR_UPDATE);

export const propType = {
    properties: PropTypes.objectOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        _type: PropTypes.shape({
            _id: PropTypes.string.isRequired
        }).isRequired
    })).isRequired
};

// Reducer
const initialState = {};
export default checkedActionHandlers(propType, {
    [ADD_OR_UPDATE]: (state, action) => ({
        ...state,
        [action.payload._id]: action.payload
    })
}, initialState, (state) => ({ properties: state }));
