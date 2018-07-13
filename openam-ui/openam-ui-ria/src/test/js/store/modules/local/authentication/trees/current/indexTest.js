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

define([
    "sinon",
    "squire"
], (sinon, Squire) => {
    let module;

    const nodesReducer = (state = {}) => state;
    const treeReducer = (state = {}) => state;

    describe("store/modules/local/authentication/trees/current/index", () => {
        beforeEach((done) => {
            const injector = new Squire();

            injector.mock("store/modules/local/authentication/trees/current/nodes/index", nodesReducer)
                .mock("store/modules/local/authentication/trees/current/tree", treeReducer)
                .require(["store/modules/local/authentication/trees/current/index"], (subject) => {
                    module = subject;
                    done();
                });
        });

        describe("actions", () => {
            describe("#removeNode", () => {
                it("creates an action", () => {
                    expect(module.removeNode("node-id")).eql({
                        type: "local/authentication/trees/current/REMOVE_NODE",
                        payload: "node-id"
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({
                    nodes: {},
                    tree: {}
                });
            });

            const state = {
                nodes: {
                    measurements: {
                        node1: {},
                        node2: {},
                        node3: {}
                    },
                    properties: {
                        node1: {
                            key: "value"
                        },
                        node2: {
                            key: "value"
                        },
                        node3: {
                            key: "value"
                        }
                    },
                    selected: {}
                },
                tree: {
                    node1: {
                        connections: {
                            "outcome1": "node2"
                        }
                    },
                    node2: {
                        connections: {
                            "outcome1": "node3"
                        }
                    },
                    node3: {
                        connections: {
                            "outcome1": "node1",
                            "outcome2": "node2"
                        }
                    }
                }
            };

            let newState;
            describe("#removeNode", () => {
                beforeEach(() => {
                    newState = module.default(state, module.removeNode("node1"));
                });
                it("removes the node from the tree and any connections to the deleted node", () => {
                    expect(newState.tree).eql({
                        node2: {
                            connections: {
                                "outcome1": "node3"
                            }
                        },
                        node3: {
                            connections: {
                                "outcome2": "node2"
                            }
                        }
                    });
                });
                it("removes the node properties", () => {
                    expect(newState.nodes.properties).eql({
                        node2: {
                            key: "value"
                        },
                        node3: {
                            key: "value"
                        }
                    });
                });
                it("removes the node measurements", () => {
                    expect(newState.nodes.measurements).eql({
                        node2: {},
                        node3: {}
                    });
                });
                it("clears the selection if the deleted node was selected", () => {
                    newState = module.default({
                        nodes: {
                            properties: {},
                            selected: {
                                id: "node1"
                            }
                        },
                        tree: {}
                    }, module.removeNode("node1"));
                    expect(newState.nodes.selected).eql({});
                });
                it("preserves the selection if the deleted node was not selected", () => {
                    newState = module.default({
                        nodes: {
                            properties: {},
                            selected: {
                                id: "node2"
                            }
                        },
                        tree: {}
                    }, module.removeNode("node1"));
                    expect(newState.nodes.selected).eql({
                        id: "node2"
                    });
                });
            });
        });
    });
});
