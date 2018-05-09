/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/applications/agents/soapSts/agents/schema"
], (module) => {
    const schema = { key: "value" };

    describe("store/modules/remote/config/realm/applications/agents/soapSts/agents/schema", () => {
        describe("actions", () => {
            describe("#setSchema", () => {
                it("creates an action", () => {
                    expect(module.setSchema(schema)).eql({
                        type: "remote/config/realm/applications/agents/soapSts/agents/schema/SET_SCHEMA",
                        payload: schema
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).to.be.null;
            });

            it("handles #setSchema action", () => {
                expect(module.default({}, module.setSchema(schema))).eql(schema);
            });
        });
    });
});
