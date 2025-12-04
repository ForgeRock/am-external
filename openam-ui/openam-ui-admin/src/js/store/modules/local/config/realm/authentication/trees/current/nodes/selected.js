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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
import { createAction } from "redux-actions";
import PropTypes from "prop-types";
import { checkedActionHandlers } from "../../../../../../../../utils";

// Types
const SET = "local/config/realm/authentication/trees/current/nodes/selected/SET";
const REMOVE = "local/config/realm/authentication/trees/current/nodes/selected/REMOVE";

// Actions
export const set = createAction(SET);
export const remove = createAction(REMOVE);

export const propType = {
    selected: PropTypes.oneOfType([
        PropTypes.objectOf((_, key) => new Error(`Should be an empty object, but has property ${key}`)),
        PropTypes.shape({
            id: PropTypes.string.isRequired,
            type: PropTypes.string.isRequired
        })
    ]).isRequired
};

// Reducer
const initialState = {};
export default checkedActionHandlers(propType, {
    [REMOVE]: () => initialState,
    [SET]: (state, action) => action.payload
}, initialState, (state) => ({ selected: state }));
