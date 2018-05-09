/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/pages/positions"
], (propTypesChecked, subject) => {
    const id = "pageId";
    const position = { height: 100, width: 100, x: 50, y: 75 };
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/pages/positions", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#add", () => {
                it("creates an action", () => {
                    expect(module.add(id, position)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/pages/positions/ADD",
                        payload: {
                            [id]: position
                        }
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #set action", () => {
                expect(module.default({}, module.add(id, position))).eql({
                    pageId: position
                });
            });
        });
    });
});
