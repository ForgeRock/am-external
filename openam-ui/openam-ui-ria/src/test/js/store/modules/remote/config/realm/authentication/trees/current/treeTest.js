/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define(["store/modules/remote/config/realm/authentication/trees/current/tree"], (module) => {
    const nodeOne = { displayName: "one", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };
    const nodeTwo = { displayName: "two", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };

    describe("store/modules/remote/config/realm/authentication/trees/current/tree", () => {
        describe("actions", () => {
            describe("#addOrUpdateNode", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdateNode({ one: nodeOne })).eql({
                        type: "remote/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_NODE",
                        payload: { one: nodeOne }
                    });
                });
            });
            describe("#removeNode", () => {
                it("creates an action", () => {
                    expect(module.removeNode("one")).eql({
                        type: "remote/config/realm/authentication/trees/current/tree/REMOVE_NODE",
                        payload: "one"
                    });
                });
            });
            describe("#setNodes", () => {
                it("creates an action", () => {
                    expect(module.setNodes({ one: nodeOne, two: nodeTwo }, "one")).eql({
                        type: "remote/config/realm/authentication/trees/current/tree/SET_NODES",
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
