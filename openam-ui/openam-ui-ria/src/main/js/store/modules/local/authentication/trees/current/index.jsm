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
 * Copyright 2017 ForgeRock AS.
 */
import { mapValues, omit } from "lodash";
import { combineReducers } from "redux";
import { createAction } from "redux-actions";

import nodes from "./nodes/index";
import tree from "./tree";

const REMOVE_NODE = "local/authentication/trees/current/REMOVE_NODE";
const REMOVE_CURRENT_TREE = "local/authentication/trees/current/REMOVE_CURRENT_TREE";

export const removeNode = createAction(REMOVE_NODE);
export const removeCurrentTree = createAction(REMOVE_CURRENT_TREE);

const subReducer = combineReducers({
    nodes,
    tree
});

export default function current (state = {}, action) {
    switch (action.type) {
        case REMOVE_NODE:
            return {
                nodes: {
                    ...state.nodes,
                    measurements: omit(state.nodes.measurements, [action.payload]),
                    properties: omit(state.nodes.properties, [action.payload]),
                    selected: (state.nodes.selected.id === action.payload) ? {} : state.nodes.selected
                },
                tree: mapValues(omit(state.tree, [action.payload]), (node) => ({
                    ...node,
                    connections: omit(node.connections, (toNode) => toNode === action.payload)
                }))
            };
        case REMOVE_CURRENT_TREE:
            return {
                nodes: {
                    measurements: {},
                    properties: {},
                    selected: {}
                },
                tree: {}
            };
        default:
            return subReducer(state, action);
    }
}
