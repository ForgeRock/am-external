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
    "store/modules/local/authentication/trees/current/nodes/selected"
], (module) => {
    const selected = "id";

    describe("store/modules/local/authentication/trees/current/nodes/selected", () => {
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set(selected)).eql({
                        type: "local/authentication/trees/current/nodes/selected/SET",
                        payload: selected
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove()).eql({
                        type: "local/authentication/trees/current/nodes/selected/REMOVE"
                    });
                });
            });
        });
        describe("reducer", () => {
            const initialState = {};

            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql(initialState);
            });

            it("handles #set action", () => {
                expect(module.default({}, module.set(selected))).eql(selected);
            });

            it("handles #remove action", () => {
                expect(module.default({}, module.remove())).eql(initialState);
            });
        });
    });
});
