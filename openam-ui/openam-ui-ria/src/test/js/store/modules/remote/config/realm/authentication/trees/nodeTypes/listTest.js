/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/authentication/trees/nodeTypes/list"
], (module) => {
    const nodeTypeOne = { _id: "one" };
    const nodeTypeTwo = { _id: "two" };

    describe("store/modules/remote/config/realm/authentication/trees/nodeTypes/list", () => {
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set([nodeTypeOne, nodeTypeTwo])).eql({
                        type: "remote/config/realm/authentication/trees/nodeTypes/list/SET",
                        payload: [nodeTypeOne, nodeTypeTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set([nodeTypeOne, nodeTypeTwo]))).eql({
                    one: nodeTypeOne,
                    two: nodeTypeTwo
                });
            });
        });
    });
});
