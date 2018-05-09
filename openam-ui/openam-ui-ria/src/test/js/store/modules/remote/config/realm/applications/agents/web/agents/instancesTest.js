/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/applications/agents/web/agents/instances"
], (module) => {
    const agentOne = { _id: "one" };
    const agentTwo = { _id: "two" };

    describe("store/modules/remote/config/realm/applications/agents/web/agents/instances", () => {
        describe("actions", () => {
            describe("#addInstance", () => {
                it("creates an action", () => {
                    expect(module.addInstance(agentOne)).eql({
                        type: "remote/config/realm/applications/agents/web/agents/instances/ADD_INSTANCE",
                        payload: agentOne
                    });
                });
            });
            describe("#setInstances", () => {
                it("creates an action", () => {
                    expect(module.setInstances([agentOne, agentTwo])).eql({
                        type: "remote/config/realm/applications/agents/web/agents/instances/SET_INSTANCES",
                        payload: [agentOne, agentTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addInstance action", () => {
                expect(module.default({}, module.addInstance(agentOne))).eql({
                    "one": agentOne
                });
            });

            it("handles #setInstances action", () => {
                expect(module.default({}, module.setInstances([agentOne, agentTwo]))).eql({
                    one: agentOne,
                    two: agentTwo
                });
            });
        });
    });
});
