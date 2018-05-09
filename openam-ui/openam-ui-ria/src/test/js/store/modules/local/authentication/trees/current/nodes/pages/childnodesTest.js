/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes"
], (propTypesChecked, subject) => {
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#add", () => {
                it("creates an action", () => {
                    expect(module.add("childId", "pageId")).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/pages/childnodes/ADD",
                        payload: { childId: "pageId" }
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove("childId")).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/pages/childnodes/REMOVE",
                        payload: "childId"
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            const initialState = {
                childId1: "pageId",
                childId2: "pageId"
            };

            it("handles #add action", () => {
                expect(module.default(initialState, module.add("childId", "pageId"))).eql({
                    childId: "pageId",
                    childId1: "pageId",
                    childId2: "pageId"
                });
            });

            it("handles #remove action", () => {
                expect(module.default(initialState, module.remove("childId1"))).eql({
                    childId2: "pageId"
                });
            });
        });
    });
});
