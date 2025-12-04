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

import { expect } from "chai";

import reducer, {
    addOrUpdateConnection,
    addOrUpdateNode,
    removeConnection,
    setNodes,
    setOutcomes,
    updateNodePosition
} from "store/modules/local/config/realm/authentication/trees/current/tree";

describe("store/modules/local/config/realm/authentication/trees/current/tree", () => {
    const outcome = { id: "outcome1", displayName: "Outcome" };
    const nodeOne = { displayName: "one", nodeType: "MyType", connections: {}, _outcomes: [outcome], x:20, y: 20 };
    const nodeTwo = { displayName: "two", nodeType: "MyType", connections: {}, _outcomes: [outcome], x:10, y: 10 };
    const connectionOne = { "outcome1": "nodeTwo" };
    const outcomes = [{ id: "true", displayName: "True" }, { id: "false", displayName: "False" }];
    const position = { x: 100, y: 100 };

    describe("nodes", () => {
        describe("actions", () => {
            describe("#addOrUpdateNode", () => {
                it("creates an action", () => {
                    expect(addOrUpdateNode({ one: nodeOne })).eql({
                        type: "local/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_NODE",
                        payload: { one: nodeOne }
                    });
                });
            });
            describe("#setNodes", () => {
                it("creates an action", () => {
                    expect(setNodes({ one: nodeOne, two: nodeTwo })).eql({
                        type: "local/config/realm/authentication/trees/current/tree/SET_NODES",
                        payload: { one: nodeOne, two: nodeTwo }
                    });
                });
            });

            describe("#updateNodePosition", () => {
                it("creates an action", () => {
                    expect(updateNodePosition(position, "one")).eql({
                        type: "local/config/realm/authentication/trees/current/tree/UPDATE_NODE_POSITION",
                        payload: position,
                        meta: { nodeId: "one" }
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(reducer(undefined, {})).eql({ nodes: {} });
            });

            it("handles #addOrUpdateNode action", () => {
                expect(reducer({ nodes: { one: nodeOne } }, addOrUpdateNode({ two: nodeTwo }))).eql({
                    nodes: {
                        one: nodeOne,
                        two: nodeTwo
                    }
                });
            });

            it("handles #setNodes action", () => {
                expect(reducer({ nodes: {} }, setNodes({ one: nodeOne, two: nodeTwo }, "one"))).eql({
                    nodes: {
                        one: nodeOne,
                        two: nodeTwo
                    }
                });
            });

            it("handles #updateNodePosition action", () => {
                const initialState = { nodes: { one: nodeOne, two: nodeTwo } };
                expect(reducer(initialState, updateNodePosition(position, "two"))).eql({
                    nodes: {
                        one: nodeOne,
                        two: {
                            displayName: "two",
                            nodeType: "MyType",
                            connections: {},
                            _outcomes: [{ id: "outcome1", displayName: "Outcome" }],
                            x: position.x,
                            y: position.y
                        }
                    }
                });
            });
        });
    });

    describe("connections", () => {
        describe("actions", () => {
            describe("#addOrUpdateConnection", () => {
                it("creates an action", () => {
                    expect(addOrUpdateConnection(connectionOne, "one")).eql({
                        type: "local/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_CONNECTION",
                        payload: connectionOne,
                        meta: { nodeId: "one" }
                    });
                });
            });
            describe("#removeConnection", () => {
                it("creates an action", () => {
                    expect(removeConnection("outcome1", "one")).eql({
                        type: "local/config/realm/authentication/trees/current/tree/REMOVE_CONNECTION",
                        payload: "outcome1",
                        meta: { nodeId: "one" }
                    });
                });
            });
        });
        describe("reducer", () => {
            it("handles #addOrUpdateConnection action", () => {
                const initialState = { nodes: { one: nodeOne, two: nodeTwo } };
                expect(reducer(initialState, addOrUpdateConnection(connectionOne, "one"))).eql({
                    nodes: {
                        one: {
                            displayName: "one",
                            nodeType: "MyType",
                            connections: { outcome1: "nodeTwo" },
                            _outcomes: [{ id: "outcome1", displayName: "Outcome" }],
                            x: 20,
                            y: 20
                        },
                        two: nodeTwo
                    }
                });
            });

            it("handles #removeConnection action", () => {
                const initialState = {
                    nodes: {
                        one: {
                            connections: { outcome1: "nodeTwo", outcome2: "nodeTwo" },
                            displayName: "one",
                            nodeType: "MyType"
                        },
                        two: nodeTwo
                    }
                };
                expect(reducer(initialState, removeConnection("outcome1", "one"))).eql({
                    nodes: {
                        one: {
                            connections: { outcome2: "nodeTwo" },
                            displayName: "one",
                            nodeType: "MyType"
                        },
                        two: nodeTwo
                    }
                });
            });
        });
    });

    describe("outcomes", () => {
        describe("actions", () => {
            describe("#setOutcomes", () => {
                it("creates an action", () => {
                    expect(setOutcomes(outcomes, "one")).eql({
                        type: "local/config/realm/authentication/trees/current/tree/SET_OUTCOMES",
                        payload: outcomes,
                        meta: { nodeId: "one" }
                    });
                });
            });
        });
        describe("reducer", () => {
            it("handles #setOutcomes action", () => {
                const initialState = { nodes: { one: nodeOne } };
                expect(reducer(initialState, setOutcomes(outcomes, "one"))).eql({
                    nodes: {
                        one: {
                            connections: {},
                            displayName: "one",
                            nodeType: "MyType",
                            _outcomes: outcomes,
                            x: 20,
                            y: 20
                        }
                    }
                });
            });
        });
    });
});
