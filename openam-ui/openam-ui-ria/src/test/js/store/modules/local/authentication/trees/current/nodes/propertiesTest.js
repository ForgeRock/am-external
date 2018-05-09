/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/properties"
], (propTypesChecked, subject) => {
    const properties = { _id: "one", _type: { _id: "MyType" } };
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/properties", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#addOrUpdate", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdate(properties)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/properties/ADD_OR_UPDATE",
                        payload: properties
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addOrUpdate action", () => {
                expect(module.default({}, module.addOrUpdate(properties))).eql({
                    one: properties
                });
            });
        });
    });
});
