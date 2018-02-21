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
    "store/modules/remote/oauth2/groups/instances"
], (module) => {
    const groupOne = { _id: "one" };
    const groupTwo = { _id: "two" };

    describe("store/modules/remote/oauth2/groups/instances", () => {
        describe("actions", () => {
            describe("#addInstance", () => {
                it("creates an action", () => {
                    expect(module.addInstance(groupOne)).eql({
                        type: "remote/oauth2/groups/instances/ADD_INSTANCE",
                        payload: groupOne
                    });
                });
            });
            describe("#removeInstance", () => {
                it("creates an action", () => {
                    expect(module.removeInstance(groupOne)).eql({
                        type: "remote/oauth2/groups/instances/REMOVE_INSTANCE",
                        payload: groupOne
                    });
                });
            });
            describe("#setInstances", () => {
                it("creates an action", () => {
                    expect(module.setInstances([groupOne, groupTwo])).eql({
                        type: "remote/oauth2/groups/instances/SET_INSTANCES",
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

            it("handles #removeInstance action", () => {
                const initialState = {
                    "one": groupOne,
                    "two": groupTwo
                };
                expect(module.default(initialState, module.removeInstance(groupTwo))).eql({
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
