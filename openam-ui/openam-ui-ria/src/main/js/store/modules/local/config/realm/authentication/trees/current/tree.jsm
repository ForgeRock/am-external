/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
