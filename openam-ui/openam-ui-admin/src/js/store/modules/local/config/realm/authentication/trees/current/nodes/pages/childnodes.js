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
