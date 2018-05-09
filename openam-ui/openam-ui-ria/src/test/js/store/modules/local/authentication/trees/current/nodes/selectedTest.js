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
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/selected"
], (propTypesChecked, subject) => {
    const selected = {
        id: "id",
        type: "MyType"
    };
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/selected", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    expect(module.set(selected)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/selected/SET",
                        payload: selected
                    });
                });
            });
            describe("#remove", () => {
                it("creates an action", () => {
                    expect(module.remove()).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/selected/REMOVE"
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
                expect(module.default(initialState, module.set(selected))).eql(selected);
            });

            it("handles #remove action", () => {
                expect(module.default(selected, module.remove())).eql(initialState);
            });
        });
    });
});
