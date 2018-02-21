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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "lodash",
    "store/modules/remote/authentication/trees/list"
], ({ omit }, module) => {
    const treeOne = { _id: "one", nodes: {} };
    const treeTwo = { _id: "two", nodes: {} };

    describe("store/modules/remote/authentication/trees/list", () => {
        describe("actions", () => {
            describe("#addOrUpdate", () => {
                it("creates an action", () => {
                    expect(module.addOrUpdate(treeOne)).eql({
                        type: "remote/authentication/trees/list/ADD_OR_UPDATE",
                        payload: treeOne
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove("one")).eql({
                        type: "remote/authentication/trees/list/REMOVE",
                        payload: "one"
                    });
                });
            });
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set([treeOne, treeTwo])).eql({
                        type: "remote/authentication/trees/list/SET",
                        payload: [treeOne, treeTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addOrUpdate action", () => {
                expect(module.default({}, module.addOrUpdate(treeOne))).eql({
                    one: omit(treeOne, "nodes")
                });
            });

            it("handles #remove action", () => {
                const initialState = {
                    one: treeOne,
                    two: treeTwo
                };
                expect(module.default(initialState, module.remove("two"))).eql({
                    one: treeOne
                });
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set([treeOne, treeTwo]))).eql({
                    one: omit(treeOne, "nodes"),
                    two: omit(treeTwo, "nodes")
                });
            });
        });
    });
});
