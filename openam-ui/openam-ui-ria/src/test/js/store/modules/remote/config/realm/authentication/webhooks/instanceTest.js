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

define([
    "store/modules/remote/config/realm/authentication/webhooks/instances"
], (module) => {
    const webhookOne = { _id: "one" };
    const webhookTwo = { _id: "two" };

    describe("store/modules/remote/config/realm/authentication/webhooks/instances", () => {
        describe("actions", () => {
            describe("#add", () => {
                it("creates an action", () => {
                    expect(module.add(webhookOne)).eql({
                        type: "remote/config/realm/authentication/webhooks/instances/ADD",
                        payload: webhookOne
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove("one")).eql({
                        type: "remote/config/realm/authentication/webhooks/instances/REMOVE",
                        payload: "one"
                    });
                });
            });
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set([webhookOne, webhookTwo])).eql({
                        type: "remote/config/realm/authentication/webhooks/instances/SET",
                        payload: [webhookOne, webhookTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #add action", () => {
                expect(module.default({}, module.add(webhookOne))).eql({
                    one: webhookOne
                });
            });

            it("handles #remove action", () => {
                const initialState = {
                    one: webhookOne,
                    two: webhookTwo
                };
                expect(module.default(initialState, module.remove("two"))).eql({
                    one: webhookOne
                });
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set([webhookOne, webhookTwo]))).eql({
                    one: webhookOne,
                    two: webhookTwo
                });
            });
        });
    });
});
