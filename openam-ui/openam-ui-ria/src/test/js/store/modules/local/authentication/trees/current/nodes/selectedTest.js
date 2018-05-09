/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/selected"
], (propTypesChecked, subject) => {
    const selected = {
        id: "id",
        type: "MyType"
    };
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/selected", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set(selected)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/selected/SET",
                        payload: selected
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove()).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/selected/REMOVE"
                    });
                });
            });
        });
        describe("reducer", () => {
            const initialState = {};

            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql(initialState);
            });

            it("handles #set action", () => {
                expect(module.default(initialState, module.set(selected))).eql(selected);
            });

            it("handles #remove action", () => {
                expect(module.default(selected, module.remove())).eql(initialState);
            });
        });
    });
});
