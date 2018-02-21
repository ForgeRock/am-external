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
    const startNode = {
        "startNode": {
            displayName: "Start",
            connections: { outcome: "one" },
            _outcomes: { outcome: "Outcome" }
        }
    };
    const nodeOne = { displayName: "one", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };
    const nodeTwo = { displayName: "two", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };
    const successNodeId = "70e691a5-1e33-4ac3-a356-e7b6d60d92e0";
    const successNode = { [successNodeId]: { displayName: "Success" } };
    const failureNodeId = "e301438c-0bd0-429c-ab0c-66126501069a";
    const failureNode = { [failureNodeId]: { displayName: "Failure" } };
    const connectionOne = { "outcome1": "nodeTwo" };
    const outcomes = [{ id: "true", displayName: "True" }, { id: "false", displayName: "False" }];

    describe("store/modules/local/authentication/trees/current/tree", () => {
        beforeEach((done) => {
            const injector = new Squire();

            const staticNodes = {
                failure: sinon.stub().returns(failureNode),
                start: sinon.stub().returns(startNode),
                success: sinon.stub().returns(successNode)
            };

            injector.mock("store/modules/local/authentication/trees/current/nodes/static", staticNodes)
                .require(["store/modules/local/authentication/trees/current/tree"], (subject) => {
                    module = subject;
                    done();
                });
        });

        describe("nodes", () => {
            describe("actions", () => {
                describe("#addOrUpdateNode", () => {
                    it("creates an action", () => {
                        expect(module.addOrUpdateNode({ one: nodeOne })).eql({
                            type: "local/authentication/trees/current/tree/ADD_OR_UPDATE_NODE",
                            payload: { one: nodeOne }
                        });
                    });
                });
                describe("#setNodes", () => {
                    it("creates an action", () => {
                        expect(module.setNodes({ one: nodeOne, two: nodeTwo }, "one")).eql({
                            type: "local/authentication/trees/current/tree/SET_NODES",
                            payload: { one: nodeOne, two: nodeTwo },
                            meta: {
                                addSucccessNode: false,
                                addFailureNode: false,
                                entryNodeId: "one"
                            }
                        });
                    });

                    it("creates an action with metadata", () => {
                        expect(module.setNodes({ one: nodeOne, two: nodeTwo }, "one", true, true)).eql({
                            type: "local/authentication/trees/current/tree/SET_NODES",
                            payload: { one: nodeOne, two: nodeTwo },
                            meta: {
                                addSucccessNode: true,
                                addFailureNode: true,
                                entryNodeId: "one"
                            }
                        });
                    });
                });
            });

            describe("reducer", () => {
                it("returns the initial state", () => {
                    expect(module.default(undefined, {})).eql({});
                });

                it("handles #addOrUpdateNode action", () => {
                    expect(module.default({ one: nodeOne }, module.addOrUpdateNode({ two: nodeTwo })))
                        .eql({ one: nodeOne, two: nodeTwo });
                });

                it("handles #setNodes action", () => {
                    expect(module.default({}, module.setNodes({ one: nodeOne, two: nodeTwo }, "one"))).eql({
                        startNode: startNode.startNode,
                        one: nodeOne,
                        two: nodeTwo
                    });
                });

                it("handles #setNodes action with metadata", () => {
                    expect(module.default({}, module.setNodes({ "one": nodeOne, "two": nodeTwo }, "one", true, true)))
                        .eql({
                            startNode: startNode.startNode,
                            one: nodeOne,
                            two: nodeTwo,
                            [successNodeId]: successNode[successNodeId],
                            [failureNodeId]: failureNode[failureNodeId]
                        });
                });
            });
        });

        describe("connections", () => {
            describe("actions", () => {
                describe("#addOrUpdateConnection", () => {
                    it("creates an action", () => {
                        expect(module.addOrUpdateConnection(connectionOne, "one")).eql({
                            type: "local/authentication/trees/current/tree/ADD_OR_UPDATE_CONNECTION",
                            payload: connectionOne,
                            meta: { nodeId: "one" }
                        });
                    });
                });
                describe("#removeConnection", () => {
                    it("creates an action", () => {
                        expect(module.removeConnection("outcome1", "one")).eql({
                            type: "local/authentication/trees/current/tree/REMOVE_CONNECTION",
                            payload: "outcome1",
                            meta: { nodeId: "one" }
                        });
                    });
                });
            });
            describe("reducer", () => {
                it("handles #addOrUpdateConnection action", () => {
                    const initialState = { one: nodeOne, two: nodeTwo };
                    expect(module.default(initialState, module.addOrUpdateConnection(connectionOne, "one"))).eql({
                        one: {
                            displayName: "one",
                            connections: { outcome1: "nodeTwo" },
                            _outcomes: [{ id: "outcome1", displayName: "Outcome" }]
                        },
                        two: nodeTwo
                    });
                });

                it("handles #removeConnection action", () => {
                    const initialState = {
                        one: { connections: { outcome1: "nodeTwo", outcome2: "nodeTwo" } },
                        two: nodeTwo
                    };
                    expect(module.default(initialState, module.removeConnection("outcome1", "one"))).eql({
                        one: { connections: { outcome2: "nodeTwo" } },
                        two: nodeTwo
                    });
                });
            });
        });

        describe("outcomes", () => {
            describe("actions", () => {
                describe("#setOutcomes", () => {
                    it("creates an action", () => {
                        expect(module.setOutcomes(outcomes, "one")).eql({
                            type: "local/authentication/trees/current/tree/SET_OUTCOMES",
                            payload: outcomes,
                            meta: { nodeId: "one" }
                        });
                    });
                });
            });
            describe("reducer", () => {
                it("handles #setOutcomes action", () => {
                    const initialState = { one: nodeOne };
                    expect(module.default(initialState, module.setOutcomes(outcomes, "one"))).eql({
                        one: {
                            displayName: "one",
                            connections: {},
                            _outcomes: outcomes
                        }
                    });
                });
            });
        });
    });
});
