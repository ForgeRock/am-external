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

import reducer, { addInstance, setInstances } from "./instances";

const agentOne = { _id: "one" };
const agentTwo = { _id: "two" };

describe("store/modules/remote/config/realm/applications/agents/java/agents/instances", () => {
    describe("actions", () => {
        describe("#addInstance", () => {
            it("creates an action", () => {
                expect(addInstance(agentOne)).eql({
                    type: "remote/config/realm/applications/agents/java/agents/instances/ADD_INSTANCE",
                    payload: agentOne
                });
            });
        });
        describe("#setInstances", () => {
            it("creates an action", () => {
                expect(setInstances([agentOne, agentTwo])).eql({
                    type: "remote/config/realm/applications/agents/java/agents/instances/SET_INSTANCES",
                    payload: [agentOne, agentTwo]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addInstance action", () => {
            expect(reducer({}, addInstance(agentOne))).eql({
                "one": agentOne
            });
        });

        it("handles #setInstances action", () => {
            expect(reducer({}, setInstances([agentOne, agentTwo]))).eql({
                one: agentOne,
                two: agentTwo
            });
        });
    });
});
