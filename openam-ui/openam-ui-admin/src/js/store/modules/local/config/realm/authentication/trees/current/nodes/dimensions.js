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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import PropTypes from "prop-types";
import { checkedActionHandlers } from "../../../../../../../../utils";
import { createAction } from "redux-actions";

// Types
const UPDATE_DIMENSIONS = "local/config/realm/authentication/trees/current/nodes/dimensions/UPDATE_DIMENSIONS";

// Actions
export const updateDimensions = createAction(UPDATE_DIMENSIONS);

// PropType
export const propType = {
    dimensions: PropTypes.objectOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        height: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired
    })).isRequired
};

// Reducer
const initialState = {};
const initialDimensionState = { height: 0, width: 0 };
export default checkedActionHandlers(propType, {
    [UPDATE_DIMENSIONS]: (state, action) => ({
        ...state,
        [action.payload.id]: {
            ...initialDimensionState,
            ...state[action.payload.id],
            height: action.payload.height,
            id: action.payload.id,
            width: action.payload.width
        }
    })
}, initialState, (state) => ({ dimensions: state }));
