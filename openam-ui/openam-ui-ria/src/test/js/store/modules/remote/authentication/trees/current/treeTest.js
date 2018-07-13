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

define(["store/modules/remote/authentication/trees/current/tree"], (module) => {
    const nodeOne = { displayName: "one", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };
    const nodeTwo = { displayName: "two", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };

    describe("store/modules/remote/authentication/trees/current/tree", () => {
        describe("actions", () => {
            describe("#addOrUpdateNode", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdateNode({ one: nodeOne })).eql({
                        type: "remote/authentication/trees/current/tree/ADD_OR_UPDATE_NODE",
                        payload: { one: nodeOne }
                    });
                });
            });
            describe("#removeNode", () => {
                it("creates an action", () => {
                    expect(module.removeNode("one")).eql({
                        type: "remote/authentication/trees/current/tree/REMOVE_NODE",
                        payload: "one"
                    });
                });
            });
            describe("#setNodes", () => {
                it("creates an action", () => {
                    expect(module.setNodes({ one: nodeOne, two: nodeTwo }, "one")).eql({
                        type: "remote/authentication/trees/current/tree/SET_NODES",
                        payload: { one: nodeOne, two: nodeTwo }
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addOrUpdateNode action", () => {
                expect(module.default({}, module.addOrUpdateNode({ one: nodeOne }))).eql({ one: nodeOne });
            });

            it("handles #removeNode action", () => {
                const initialState = { one: nodeOne, two: nodeTwo };
                expect(module.default(initialState, module.removeNode("two"))).eql({ one: nodeOne });
            });

            it("handles #setNodes action", () => {
                expect(module.default({}, module.setNodes({ one: nodeOne, two: nodeTwo }, "one"))).eql({
                    one: nodeOne,
                    two: nodeTwo
                });
            });
        });
    });
});
