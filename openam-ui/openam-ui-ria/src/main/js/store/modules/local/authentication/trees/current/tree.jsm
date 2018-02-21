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
import { createAction, handleActions } from "redux-actions";
import { omit } from "lodash";

import { failure, start, success } from "./nodes/static";

// Types
const ADD_OR_UPDATE_NODE = "local/authentication/trees/current/tree/ADD_OR_UPDATE_NODE";
const SET_NODES = "local/authentication/trees/current/tree/SET_NODES";

const ADD_OR_UPDATE_CONNECTION = "local/authentication/trees/current/tree/ADD_OR_UPDATE_CONNECTION";
const REMOVE_CONNECTION = "local/authentication/trees/current/tree/REMOVE_CONNECTION";

const SET_OUTCOMES = "local/authentication/trees/current/tree/SET_OUTCOMES";

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
const initialState = {};
export default handleActions({
    [ADD_OR_UPDATE_NODE]: (state, action) => ({
        ...state,
        ...action.payload
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
            ...action.payload,
            ...staticNodes
        };
    },
    [ADD_OR_UPDATE_CONNECTION]: (state, action) => ({
        ...state,
        [action.meta.nodeId]: {
            ...state[action.meta.nodeId],
            connections: {
                ...state[action.meta.nodeId].connections,
                ...action.payload
            }
        }
    }),
    [REMOVE_CONNECTION]: (state, action) => ({
        ...state,
        [action.meta.nodeId]: {
            ...state[action.meta.nodeId],
            connections: omit(state[action.meta.nodeId].connections, action.payload)
        }
    }),
    [SET_OUTCOMES]: (state, action) => ({
        ...state,
        [action.meta.nodeId]: {
            ...state[action.meta.nodeId],
            _outcomes: action.payload
        }
    })
}, initialState);
