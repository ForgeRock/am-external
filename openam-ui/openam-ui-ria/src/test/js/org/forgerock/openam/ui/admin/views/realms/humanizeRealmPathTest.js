/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "sinon"
], (sinon) => {
    describe("org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath", () => {
        let i18next;
        let humanizeRealmPath;

        beforeEach(() => {
            const injector =
                require("inject-loader!org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath");

            i18next = {
                t: sinon.stub().withArgs("console.common.topLevelRealm").returns("Top Level Realm")
            };

            humanizeRealmPath = injector({
                i18next
            }).default;
        });

        context("When the realm is '/'", () => {
            it("returns a string of `Top Level Realm`", sinon.test(() => {
                const realm = "/";
                expect(humanizeRealmPath(realm)).eql("Top Level Realm");
            }));
        });

        context("When the realm is '/Foo'", () => {
            it("returns a string of `Foo`", () => {
                const realm = "/Foo";
                expect(humanizeRealmPath(realm)).eql("Foo");
            });
        });

        context("When the realm is '/Foo/Bar'", () => {
            it("returns a string of `Bar`", () => {
                const realm = "/Foo/Bar";
                expect(humanizeRealmPath(realm)).eql("Bar");
            });
        });
    });
});
