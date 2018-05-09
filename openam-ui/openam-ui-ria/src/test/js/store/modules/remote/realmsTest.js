/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "store/modules/remote/realms"
], (_, module) => {
    const rootRealm = { _id: "one", parentPath: null, name: "/" };
    const subRealm = { _id: "two", parentPath: "/", name: "subRealm" };
    const subSubRealm = { _id: "three", parentPath: "/subRealm", name: "subSubRealm" };

    describe("store/modules/remote/realms", () => {
        describe("actions", () => {
            describe("#addRealm", () => {
                it("creates an action", () => {
                    expect(module.addRealm(rootRealm)).eql({
                        type: "remote/realms/ADD_REALM",
                        payload: rootRealm
                    });
                });
            });
            describe("#removeRealm", () => {
                it("creates an action", () => {
                    expect(module.removeRealm(subRealm)).eql({
                        type: "remote/realms/REMOVE_REALM",
                        payload: subRealm
                    });
                });
            });
            describe("#setRealms", () => {
                it("creates an action", () => {
                    expect(module.setRealms([rootRealm, subRealm, subSubRealm])).eql({
                        type: "remote/realms/SET_REALMS",
                        payload: [rootRealm, subRealm, subSubRealm]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addRealm action", () => {
                expect(module.default({}, module.addRealm(rootRealm))).eql({
                    "/": _.assign({}, rootRealm, { path: "/" })
                });
            });

            it("handles #removeRealm action", () => {
                const initialState = {
                    "/": rootRealm,
                    "/subRealm": subRealm
                };
                expect(module.default(initialState, module.removeRealm(subRealm))).eql({
                    "/": rootRealm
                });
            });

            it("handles #setRealms action", () => {
                expect(module.default({}, module.setRealms([rootRealm, subRealm, subSubRealm]))).eql({
                    "/": _.assign({}, rootRealm, { path: "/" }),
                    "/subRealm": _.assign({}, subRealm, { path: "/subRealm" }),
                    "/subRealm/subSubRealm": _.assign({}, subSubRealm, { path: "/subRealm/subSubRealm" })
                });
            });
        });
    });
});
