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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

import { expect } from "chai";

import reducer, { addOrUpdateNode, removeNode, setNodes } from "./tree";

describe("store/modules/remote/config/realm/authentication/trees/current/tree", () => {
    const nodeOne = { displayName: "one", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };
    const nodeTwo = { displayName: "two", connections: {}, _outcomes: [{ id: "outcome1", displayName: "Outcome" }] };

    describe("actions", () => {
        describe("#addOrUpdateNode", () => {
            it("creates an action", () => {
                expect(addOrUpdateNode({ one: nodeOne })).eql({
                    type: "remote/config/realm/authentication/trees/current/tree/ADD_OR_UPDATE_NODE",
                    payload: { one: nodeOne }
                });
            });
        });
        describe("#removeNode", () => {
            it("creates an action", () => {
                expect(removeNode("one")).eql({
                    type: "remote/config/realm/authentication/trees/current/tree/REMOVE_NODE",
                    payload: "one"
                });
            });
        });
        describe("#setNodes", () => {
            it("creates an action", () => {
                expect(setNodes({ one: nodeOne, two: nodeTwo }, "one")).eql({
                    type: "remote/config/realm/authentication/trees/current/tree/SET_NODES",
                    payload: { one: nodeOne, two: nodeTwo }
                });
            });
        });
    });

    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addOrUpdateNode action", () => {
            expect(reducer({}, addOrUpdateNode({ one: nodeOne }))).eql({ one: nodeOne });
        });

        it("handles #removeNode action", () => {
            const initialState = { one: nodeOne, two: nodeTwo };
            expect(reducer(initialState, removeNode("two"))).eql({ one: nodeOne });
        });

        it("handles #setNodes action", () => {
            expect(reducer({}, setNodes({ one: nodeOne, two: nodeTwo }, "one"))).eql({
                one: nodeOne,
                two: nodeTwo
            });
        });
    });
});
