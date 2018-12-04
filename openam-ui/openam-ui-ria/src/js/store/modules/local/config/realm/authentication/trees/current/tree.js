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
import { createAction } from "redux-actions";
import { omit } from "lodash";
import PropTypes from "prop-types";

import { failure, start, success } from "./nodes/static";
import { checkedActionHandlers } from "../../../../../../../utils";

export const propType = {
    nodes: PropTypes.objectOf(PropTypes.shape({
        connections: PropTypes.objectOf(PropTypes.string).isRequired,
        displayName: PropTypes.string.isRequired,
        nodeType: PropTypes.string.isRequired,
        _outcomes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.string.isRequired,
            displayName: PropTypes.string.isRequired
        }))
    }))
};

// Types
const ADD_OR_UPDATE_NODE = "local/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_NODE";
const SET_NODES = "local/config/realm/authentication/trees/current/tree/SET_NODES";

const ADD_OR_UPDATE_CONNECTION = "local/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_CONNECTION";
const REMOVE_CONNECTION = "local/config/realm/authentication/trees/current/tree/REMOVE_CONNECTION";

const SET_OUTCOMES = "local/config/realm/authentication/trees/current/tree/SET_OUTCOMES";

// Actions
export const addOrUpdateNode = createAction(ADD_OR_UPDATE_NODE);
export const setNodes = createAction(SET_NODES,
    (payload) => payload,
    (payload, entryNodeId, addSucccessNode = false, addFailureNode = false) => ({
        addSucccessNode,
        addFailureNode,
        entryNodeId
    })
);

export const addOrUpdateConnection = createAction(ADD_OR_UPDATE_CONNECTION, (payload) => payload,
    (payload, nodeId) => ({ nodeId }));
export const removeConnection = createAction(REMOVE_CONNECTION, (payload) => payload,
    (payload, nodeId) => ({ nodeId }));

export const setOutcomes = createAction(SET_OUTCOMES, (payload) => payload, (payload, nodeId) => ({ nodeId }));

// Reducer
const initialState = { nodes: {} };
export default checkedActionHandlers(propType, {
    [ADD_OR_UPDATE_NODE]: (state, action) => ({
        nodes: {
            ...state.nodes,
            ...action.payload
        }
    }),
    [SET_NODES]: (state, action) => {
        const startNode = start(action.meta.entryNodeId);

        let staticNodes = { ...startNode };
        if (action.meta.addSucccessNode) {
            staticNodes = {
                ...staticNodes,
                ...success()
            };
        }
        if (action.meta.addFailureNode) {
            staticNodes = {
                ...staticNodes,
                ...failure()
            };
        }

        return {
            nodes: {
                ...action.payload,
                ...staticNodes
            }
        };
    },
    [ADD_OR_UPDATE_CONNECTION]: (state, action) => ({
        nodes: {
            ...state.nodes,
            [action.meta.nodeId]: {
                ...state.nodes[action.meta.nodeId],
                connections: {
                    ...state.nodes[action.meta.nodeId].connections,
                    ...action.payload
                }
            }
        }
    }),
    [REMOVE_CONNECTION]: (state, action) => ({
        nodes: {
            ...state.nodes,
            [action.meta.nodeId]: {
                ...state.nodes[action.meta.nodeId],
                connections: omit(state.nodes[action.meta.nodeId].connections, action.payload)
            }
        }
    }),
    [SET_OUTCOMES]: (state, action) => ({
        nodes: {
            ...state.nodes,
            [action.meta.nodeId]: {
                ...state.nodes[action.meta.nodeId],
                _outcomes: action.payload
            }
        }
    })
}, initialState);
