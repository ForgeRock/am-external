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
import PropTypes from "prop-types";
import { combineReducers } from "redux";
import { createAction } from "redux-actions";
import { assign, chain, filter, keys, mapValues, omit, omitBy, pickBy, toPairs } from "lodash";

import { PAGE_NODE_TYPE } from "./nodes/static";
import nodes, { propType as nodesType } from "./nodes";
import tree, { propType as treeType } from "./tree";
import { checkedActionResult } from "../../../../../../../utils";

const MOVE_NODE_TO_PAGE = "local/config/realm/authentication/trees/current/MOVE_NODE_TO_PAGE";
const MOVE_NODE_TO_TREE = "local/config/realm/authentication/trees/current/MOVE_NODE_TO_TREE";
const REMOVE_NODE = "local/config/realm/authentication/trees/current/REMOVE_NODE";
const REMOVE_CURRENT_TREE = "local/config/realm/authentication/trees/current/REMOVE_CURRENT_TREE";
const SET_DISPLAY_NAME = "local/config/realm/authentication/trees/current/SET_DISPLAY_NAME";

export const moveNodeToPage = createAction(MOVE_NODE_TO_PAGE);
export const moveNodeToTree = createAction(MOVE_NODE_TO_TREE);
export const removeNode = createAction(REMOVE_NODE);
export const removeCurrentTree = createAction(REMOVE_CURRENT_TREE);
export const setDisplayName = createAction(SET_DISPLAY_NAME);

export const propType = {
    nodes: PropTypes.shape(nodesType),
    tree: PropTypes.shape(treeType)
};

const subReducer = combineReducers({
    nodes,
    tree
});

export default function current (state = {}, action) {
    switch (action.type) {
        case MOVE_NODE_TO_PAGE: {
            if (state.nodes.pages.childnodes[action.payload.nodeId] === action.payload.pageId) {
                return checkedActionResult(propType, action, state, state);
            }
            const node = chain(state.nodes.properties).values()
                .filter((n) => n._type._id === PAGE_NODE_TYPE)
                .map((page) => page.nodes)
                .flatten()
                .concat(toPairs(state.tree.nodes).map((pair) => assign({ _id: pair[0] }, pair[1])))
                .find((n) => n._id === action.payload.nodeId)
                .omit(["connections", "_outcomes"])
                .value();
            return checkedActionResult(propType, action, state, {
                nodes: {
                    ...state.nodes,
                    dimensions: omit(state.nodes.dimensions, [action.payload.nodeId]),
                    pages: {
                        ...state.nodes.pages,
                        childnodes: {
                            ...state.nodes.pages.childnodes,
                            [action.payload.nodeId]: action.payload.pageId
                        }
                    },
                    properties: {
                        ...mapValues(state.nodes.properties, (node) => {
                            return node._type._id === PAGE_NODE_TYPE ? {
                                ...node,
                                nodes: node.nodes.filter((n) => n._id !== action.payload.nodeId)
                            } : node;
                        }),
                        [action.payload.pageId]: {
                            ...state.nodes.properties[action.payload.pageId],
                            nodes: state.nodes.properties[action.payload.pageId].nodes.concat([node])
                        }
                    }
                },
                tree: {
                    nodes: mapValues(omit(state.tree.nodes, action.payload.nodeId), (node) => ({
                        ...node,
                        connections: omitBy(node.connections, (toNode) => toNode === action.payload.nodeId)
                    }))
                }
            });
        }
        case MOVE_NODE_TO_TREE: {
            const node = chain(state.nodes.properties).values().filter((n) => n._type._id === PAGE_NODE_TYPE)
                .map((page) => page.nodes)
                .flatten()
                .find((n) => n._id === action.payload)
                .value();
            if (!node) {
                return checkedActionResult(propType, action, state, state);
            }
            return checkedActionResult(propType, action, state, {
                nodes: {
                    ...state.nodes,
                    dimensions: omit(state.nodes.dimensions, [action.payload]),
                    pages: {
                        ...state.nodes.pages,
                        childnodes: omit(state.nodes.pages.childnodes, [action.payload])
                    },
                    properties: {
                        ...mapValues(state.nodes.properties, (node) => {
                            return node._type._id === PAGE_NODE_TYPE ? {
                                ...node,
                                nodes: node.nodes.filter((n) => n._id !== action.payload)
                            } : node;
                        })
                    }
                },
                tree: {
                    nodes: {
                        ...state.tree.nodes,
                        [action.payload]: assign({ _outcomes: {}, connections: {} }, omit(node, ["_id"]))
                    }
                }
            });
        }
        case REMOVE_CURRENT_TREE:
            return checkedActionResult(propType, action, state, {
                nodes: {
                    dimensions: {},
                    pages: {
                        childnodes: {},
                        positions: {}
                    },
                    properties: {},
                    selected: {}
                },
                tree: {
                    nodes: {}
                }
            });
        case REMOVE_NODE: {
            const affectedNodes = [
                ...keys(pickBy(state.nodes.pages.childnodes, (value) => value === action.payload)),
                action.payload
            ];
            return checkedActionResult(propType, action, state, {
                nodes: {
                    ...state.nodes,
                    dimensions: omit(state.nodes.dimensions, [action.payload]),
                    properties: mapValues(omit(state.nodes.properties, affectedNodes), (node) => {
                        return node._type._id === PAGE_NODE_TYPE ? ({
                            ...node,
                            nodes: filter(node.nodes, (childnode) => childnode._id !== action.payload)
                        }) : node;
                    }),
                    selected: (state.nodes.selected.id === action.payload) ? {} : state.nodes.selected,
                    pages: {
                        childnodes: omitBy(state.nodes.pages.childnodes,
                            (value, key) => value === action.payload || key === action.payload),
                        positions: omit(state.nodes.pages.positions, [action.payload])
                    }
                },
                tree: {
                    nodes: mapValues(omit(state.tree.nodes, affectedNodes), (node) => ({
                        ...node,
                        connections: omitBy(node.connections, (toNode) => toNode === action.payload)
                    }))
                }
            });
        }
        case SET_DISPLAY_NAME: {
            if (state.tree.nodes[action.payload.id]) {
                return checkedActionResult(propType, action, state, {
                    ...state,
                    tree: {
                        nodes: {
                            ...state.tree.nodes,
                            [action.payload.id]: {
                                ...state.tree.nodes[action.payload.id],
                                displayName: action.payload.displayName
                            }
                        }
                    }
                });
            } else {
                return checkedActionResult(propType, action, state, {
                    ...state,
                    nodes: {
                        ...state.nodes,
                        properties: {
                            ...mapValues(state.nodes.properties, (node) => {
                                return node._type._id === PAGE_NODE_TYPE ? {
                                    ...node,
                                    nodes: node.nodes.map((n) => {
                                        return n._id === action.payload.id
                                            ? {
                                                ...n,
                                                displayName: action.payload.displayName
                                            }
                                            : n;
                                    })
                                } : node;
                            })
                        }
                    }
                });
            }
        }
        default:
            return subReducer(state, action);
    }
}
