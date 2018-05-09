/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/authentication/trees/nodeTypes/schema"
], (module) => {
    const schema = { _id: "one" };
    const nodeType = "nodeType";

    describe("store/modules/remote/config/realm/authentication/trees/nodeTypes/schema", () => {
        describe("actions", () => {
            describe("#addOrUpdateSchema", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdateSchema(schema, nodeType)).eql({
                        type: "remote/config/realm/authentication/trees/nodeTypes/schema/ADD_OR_UPDATE_SCHEMA",
                        payload: schema,
                        meta: { nodeType }
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addOrUpdate action", () => {
                expect(module.default({}, module.addOrUpdateSchema(schema, nodeType))).eql({
                    [nodeType]: schema
                });
            });
        });
    });
});
