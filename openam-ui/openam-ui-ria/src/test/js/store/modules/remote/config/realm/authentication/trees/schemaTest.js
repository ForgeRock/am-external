/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/authentication/trees/schema"
], (module) => {
    const schema = { key: "value" };

    describe("store/modules/remote/config/realm/authentication/trees/schema", () => {
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set(schema)).eql({
                        type: "remote/config/realm/authentication/trees/schema/SET",
                        payload: schema
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).to.be.null;
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set(schema))).eql(schema);
            });
        });
    });
});
