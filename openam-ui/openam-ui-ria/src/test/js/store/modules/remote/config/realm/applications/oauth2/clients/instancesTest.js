/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/applications/oauth2/clients/instances"
], (module) => {
    const clientOne = { _id: "one" };
    const clientTwo = { _id: "two" };

    describe("store/modules/remote/config/realm/applications/oauth2/clients/instances", () => {
        describe("actions", () => {
            describe("#addInstance", () => {
                it("creates an action", () => {
                    expect(module.addInstance(clientOne)).eql({
                        type: "remote/config/realm/applications/oauth2/clients/instances/ADD_INSTANCE",
                        payload: clientOne
                    });
                });
            });
            describe("#setInstances", () => {
                it("creates an action", () => {
                    expect(module.setInstances([clientOne, clientTwo])).eql({
                        type: "remote/config/realm/applications/oauth2/clients/instances/SET_INSTANCES",
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

            it("handles #setInstances action", () => {
                expect(module.default({}, module.setInstances([clientOne, clientTwo]))).eql({
                    one: clientOne,
                    two: clientTwo
                });
            });
        });
    });
});
