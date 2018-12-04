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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { expect } from "chai";
import { omit } from "lodash";

import reducer, { addOrUpdate, remove, set } from "./list";

describe("store/modules/remote/config/realm/authentication/trees/list", () => {
    const treeOne = { _id: "one", nodes: {} };
    const treeTwo = { _id: "two", nodes: {} };

    describe("actions", () => {
        describe("#addOrUpdate", () => {
            it("creates an action", () => {
                expect(addOrUpdate(treeOne)).eql({
                    type: "remote/config/realm/authentication/trees/list/ADD_OR_UPDATE",
                    payload: treeOne
                });
            });
        });
        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove("one")).eql({
                    type: "remote/config/realm/authentication/trees/list/REMOVE",
                    payload: "one"
                });
            });
        });
        describe("#set", () => {
            it("creates an action", () => {
                expect(set([treeOne, treeTwo])).eql({
                    type: "remote/config/realm/authentication/trees/list/SET",
                    payload: [treeOne, treeTwo]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addOrUpdate action", () => {
            expect(reducer({}, addOrUpdate(treeOne))).eql({
                one: omit(treeOne, "nodes")
            });
        });

        it("handles #remove action", () => {
            const initialState = {
                one: treeOne,
                two: treeTwo
            };
            expect(reducer(initialState, remove("two"))).eql({
                one: treeOne
            });
        });

        it("handles #set action", () => {
            expect(reducer({}, set([treeOne, treeTwo]))).eql({
                one: omit(treeOne, "nodes"),
                two: omit(treeTwo, "nodes")
            });
        });
    });
});
