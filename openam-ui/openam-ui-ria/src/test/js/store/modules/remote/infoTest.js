/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/info"
], (module) => {
    describe("store/modules/remote/info", () => {
        describe("actions", () => {
            describe("#addRealm", () => {
                it("creates an action", () => {
                    const realm = "/realmA";

                    expect(module.addRealm(realm)).eql({
                        type: "remote/info/ADD_REALM",
                        payload: realm
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
        });
    });
});
