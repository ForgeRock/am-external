/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/authentication/trees/current/nodes/properties"
], (module) => {
    const properties = { _id: "one" };

    describe("store/modules/remote/config/realm/authentication/trees/current/nodes/properties", () => {
        describe("actions", () => {
            describe("#addOrUpdate", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdate(properties)).eql({
                        type: "remote/config/realm/authentication/trees/current/nodes/properties/ADD_OR_UPDATE",
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
