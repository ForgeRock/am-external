/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/applications/agents/java/groups/instances"
], (module) => {
    const groupOne = { _id: "one" };
    const groupTwo = { _id: "two" };

    describe("store/modules/remote/config/realm/applications/agents/java/groups/instances", () => {
        describe("actions", () => {
            describe("#addInstance", () => {
                it("creates an action", () => {
                    expect(module.addInstance(groupOne)).eql({
                        type: "remote/config/realm/applications/agents/java/groups/instances/ADD_INSTANCE",
                        payload: groupOne
                    });
                });
            });
            describe("#setInstances", () => {
                it("creates an action", () => {
                    expect(module.setInstances([groupOne, groupTwo])).eql({
                        type: "remote/config/realm/applications/agents/java/groups/instances/SET_INSTANCES",
                        payload: [groupOne, groupTwo]
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #addInstance action", () => {
                expect(module.default({}, module.addInstance(groupOne))).eql({
                    "one": groupOne
                });
            });

            it("handles #setInstances action", () => {
                expect(module.default({}, module.setInstances([groupOne, groupTwo]))).eql({
                    one: groupOne,
                    two: groupTwo
                });
            });
        });
    });
});
