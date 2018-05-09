/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
