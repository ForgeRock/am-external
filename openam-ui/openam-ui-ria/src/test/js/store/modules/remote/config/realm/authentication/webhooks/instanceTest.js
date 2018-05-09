/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
