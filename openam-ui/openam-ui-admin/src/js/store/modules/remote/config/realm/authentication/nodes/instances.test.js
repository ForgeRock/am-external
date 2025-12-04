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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

import { expect } from "chai";

import reducer, { add, remove, set } from "./instances";

describe("store/modules/remote/config/realm/authentication/nodes/instances", () => {
    const webhookOne = { _id: "one" };
    const webhookTwo = { _id: "two" };

    describe("actions", () => {
        describe("#add", () => {
            it("creates an action", () => {
                expect(add(webhookOne)).eql({
                    type: "remote/config/realm/authentication/nodes/instances/ADD",
                    payload: webhookOne
                });
            });
        });
        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove("one")).eql({
                    type: "remote/config/realm/authentication/nodes/instances/REMOVE",
                    payload: "one"
                });
            });
        });
        describe("#set", () => {
            it("creates an action", () => {
                expect(set([webhookOne, webhookTwo])).eql({
                    type: "remote/config/realm/authentication/nodes/instances/SET",
                    payload: [webhookOne, webhookTwo]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #add action", () => {
            expect(reducer({}, add(webhookOne))).eql({
                one: webhookOne
            });
        });

        it("handles #remove action", () => {
            const initialState = {
                one: webhookOne,
                two: webhookTwo
            };
            expect(reducer(initialState, remove("two"))).eql({
                one: webhookOne
            });
        });

        it("handles #set action", () => {
            expect(reducer({}, set([webhookOne, webhookTwo]))).eql({
                one: webhookOne,
                two: webhookTwo
            });
        });
    });
});
