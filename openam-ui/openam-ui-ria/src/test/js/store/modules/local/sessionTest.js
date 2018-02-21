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
    "store/modules/local/session"
], (module) => {
    describe("store/modules/local/session", () => {
        describe("actions", () => {
            describe("#addRealm", () => {
                it("creates an action", () => {
                    const realm = "/realmA";

                    expect(module.addRealm(realm)).eql({
                        type: "local/session/ADD_REALM",
                        payload: realm
                    });
                });
            });
            describe("#removeRealm", () => {
                it("creates an action", () => {
                    expect(module.removeRealm()).eql({
                        type: "local/session/REMOVE_REALM"
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({
                    realm: undefined
                });
            });
            it("handles #addRealm action", () => {
                const realm = "/realmA";

                expect(module.default({}, module.addRealm(realm))).eql({
                    realm: realm.toLowerCase()
                });
            });
            it("handles #removeRealm action", () => {
                expect(module.default({}, module.removeRealm())).eql({});
            });
        });
    });
});
