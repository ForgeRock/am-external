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
    "squire",
    "sinon"
], (Squire, sinon) => {
    describe("org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath", () => {
        let i18next;
        let humanizeRealmPath;

        beforeEach((done) => {
            const injector = new Squire();

            i18next = {
                t: sinon.stub().withArgs("console.common.topLevelRealm").returns("Top Level Realm")
            };

            injector
                .mock("i18next", i18next)
                .require(["org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath"], (obj) => {
                    humanizeRealmPath = obj.default;
                    done();
                });
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
