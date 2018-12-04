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

import reducer, { addInstance, removeInstance, setInstances } from "./instances";

describe("store/modules/remote/config/realm/applications/federation/circlesoftrust/instances", () => {
    const agentOne = { _id: "one" };
    const agentTwo = { _id: "two" };

    describe("actions", () => {
        describe("#addInstance", () => {
            it("creates an action", () => {
                expect(addInstance(agentOne)).eql({
                    type: "remote/config/realm/applications/federation/circlesoftrust/instances/ADD_INSTANCE",
                    payload: agentOne
                });
            });
        });
        describe("#removeInstance", () => {
            it("creates an action", () => {
                expect(removeInstance(agentOne)).eql({
                    type: "remote/config/realm/applications/federation/circlesoftrust/instances/REMOVE_INSTANCE",
                    payload: agentOne
                });
            });
        });
        describe("#setInstances", () => {
            it("creates an action", () => {
                expect(setInstances([agentOne, agentTwo])).eql({
                    type: "remote/config/realm/applications/federation/circlesoftrust/instances/SET_INSTANCES",
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

        it("handles #removeInstance action", () => {
            const initialState = {
                "one": agentOne,
                "two": agentTwo
            };
            expect(reducer(initialState, removeInstance(agentTwo))).eql({
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
