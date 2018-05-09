/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
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
