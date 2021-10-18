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

import { expect } from "chai";

import reducer, { add, remove } from "./childnodes";

describe("store/modules/local/config/realm/authentication/trees/current/nodes/pages/childnodes", () => {
    describe("actions", () => {
        describe("#add", () => {
            it("creates an action", () => {
                expect(add("childId", "pageId")).eql({
                    type: "local/config/realm/authentication/trees/current/nodes/pages/childnodes/ADD",
                    payload: { childId: "pageId" }
                });
            });
        });
        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove("childId")).eql({
                    type: "local/config/realm/authentication/trees/current/nodes/pages/childnodes/REMOVE",
                    payload: "childId"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        const initialState = {
            childId1: "pageId",
            childId2: "pageId"
        };

        it("handles #add action", () => {
            expect(reducer(initialState, add("childId", "pageId"))).eql({
                childId: "pageId",
                childId1: "pageId",
                childId2: "pageId"
            });
        });

        it("handles #remove action", () => {
            expect(reducer(initialState, remove("childId1"))).eql({
                childId2: "pageId"
            });
        });
    });
});