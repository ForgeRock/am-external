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
    "store/modules/remote/oauth2/clients/instances"
], (module) => {
    const clientOne = { _id: "one" };
    const clientTwo = { _id: "two" };

    describe("store/modules/remote/oauth2/clients/instances", () => {
        describe("actions", () => {
            describe("#addInstance", () => {
                it("creates an action", () => {
                    expect(module.addInstance(clientOne)).eql({
                        type: "remote/oauth2/clients/instances/ADD_INSTANCE",
                        payload: clientOne
                    });
                });
            });
            describe("#removeInstance", () => {
                it("creates an action", () => {
                    expect(module.removeInstance(clientOne)).eql({
                        type: "remote/oauth2/clients/instances/REMOVE_INSTANCE",
                        payload: clientOne
                    });
                });
            });
            describe("#setInstances", () => {
                it("creates an action", () => {
                    expect(module.setInstances([clientOne, clientTwo])).eql({
                        type: "remote/oauth2/clients/instances/SET_INSTANCES",
                        payload: [clientOne, clientTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addInstance action", () => {
                expect(module.default({}, module.addInstance(clientOne))).eql({
                    "one": clientOne
                });
            });

            it("handles #removeInstance action", () => {
                const initialState = {
                    "one": clientOne,
                    "two": clientTwo
                };
                expect(module.default(initialState, module.removeInstance(clientTwo))).eql({
                    "one": clientOne
                });
            });

            it("handles #setInstances action", () => {
                expect(module.default({}, module.setInstances([clientOne, clientTwo]))).eql({
                    one: clientOne,
                    two: clientTwo
                });
            });
        });
    });
});
