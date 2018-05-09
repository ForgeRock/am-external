/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "store/modules/local/config/realm/authentication/trees/list"
], ({ omit }, module) => {
    const treeOne = { _id: "one", nodes: {} };
    const treeTwo = { _id: "two", nodes: {} };

    describe("store/modules/local/config/realm/authentication/trees/list", () => {
        describe("actions", () => {
            describe("#addOrUpdate", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdate(treeOne)).eql({
                        type: "local/config/realm/authentication/trees/list/ADD_OR_UPDATE",
                        payload: treeOne
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove("one")).eql({
                        type: "local/config/realm/authentication/trees/list/REMOVE",
                        payload: "one"
                    });
                });
            });
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set([treeOne, treeTwo])).eql({
                        type: "local/config/realm/authentication/trees/list/SET",
                        payload: [treeOne, treeTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addOrUpdate action", () => {
                expect(module.default({}, module.addOrUpdate(treeOne))).eql({
                    one: omit(treeOne, "nodes")
                });
            });

            it("handles #remove action", () => {
                const initialState = {
                    one: treeOne,
                    two: treeTwo
                };
                expect(module.default(initialState, module.remove("two"))).eql({
                    one: treeOne
                });
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set([treeOne, treeTwo]))).eql({
                    one: omit(treeOne, "nodes"),
                    two: omit(treeTwo, "nodes")
                });
            });
        });
    });
});
