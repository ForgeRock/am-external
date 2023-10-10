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
 * Copyright 2023 ForgeRock AS.
 */

import { expect } from "chai";
import fetchUrl from "./fetchUrl";

describe("api/fetchUrl", () => {
    describe("#fetchUrl", () => {
        it("returns path", () => {
            expect(fetchUrl("/authentication")).to.be.equals("/authentication");
        });
        it("returns realm", () => {
            expect(fetchUrl("/authentication", {realm: "/myRealm" })).to.be.equals("/realms/root/realms/myRealm/authentication");
        });
        it("returns realm", () => {
            expect(fetchUrl("/authentication", {realm: "myAlias" })).to.be.equals("/realms/myAlias/authentication");
        });
        it("stops path traversal using \\..\\", () => {
            expect(fetchUrl("/authentication", {realm: "/..\\.%2e\\%2E.\\%2e%2E\\openam\\oauth2\\mysubrealm\\authorize" }))
                .to.be.equals("/realms/root/realms/openam/realms/oauth2/realms/mysubrealm/realms/authorize/authentication");
        });
        it("stops path traversal using \\..\\ and realm alias", () => {
            expect(fetchUrl("/authentication", {realm: "..\\.%2e\\%2E.\\%2e%2E\\openam\\oauth2\\mysubrealm\\authorize" }))
                .to.be.equals("/realms/root/realms/openam/realms/oauth2/realms/mysubrealm/realms/authorize/authentication");
        });
        it("stops path traversal being possible while using space chars", () => {
            expect(fetchUrl("/authentication", {realm: "/.\n./.\r./%2e\t./%2e    %2E/openam\\oauth2\\mysubrealm\\authorize" }))
                .to.be.equals("/realms/root/realms/openam/realms/oauth2/realms/mysubrealm/realms/authorize/authentication");
        });
        it("stops path traversal being possible while using space chars and backspace", () => {
            expect(fetchUrl("/authentication", {realm: "/.\n.\\.\r.\\%2e\t.\\%2e    %2E\\openam\\oauth2\\mysubrealm\\authorize" }))
                .to.be.equals("/realms/root/realms/openam/realms/oauth2/realms/mysubrealm/realms/authorize/authentication");
        });
        it("stops path traversal being possible while using space chars using realm alias", () => {
            expect(fetchUrl("/authentication", {realm: ".\n./.\r./%2e\t./%2e    %2E/openam\\oauth2\\mysubrealm\\authorize" }))
                .to.be.equals("/realms/root/realms/openam/realms/oauth2/realms/mysubrealm/realms/authorize/authentication");
        });
        it("doesn't mangle realm containing '..'", () => {
            expect(fetchUrl("/authentication", {realm: "/myRealm..WithDots" }))
                .to.be.equals("/realms/root/realms/myRealm..WithDots/authentication");
        });
        it("doesn't mangle realm containing '..' and removes path traversal", () => {
            expect(fetchUrl("/authentication", {realm: "/myRealm..WithDots\\..\\.." }))
                .to.be.equals("/realms/root/realms/myRealm..WithDots/authentication");
        });
        it("doesn't mangle realms containing '..' and removes path traversal", () => {
            expect(fetchUrl("/authentication", {realm: "/myRealm..WithDots\\..\\..\\..mySubRealm" }))
                .to.be.equals("/realms/root/realms/myRealm..WithDots/realms/..mySubRealm/authentication");
        });
        it("doesn't mangle realm starting with '..'", () => {
            expect(fetchUrl("/authentication", {realm: "/..myRealmStaringWithDots" }))
                .to.be.equals("/realms/root/realms/..myRealmStaringWithDots/authentication");
        });
        it("doesn't mangle realm ending with '..'", () => {
            expect(fetchUrl("/authentication", {realm: "/myRealmEndingWithDots../..subRealm" }))
                .to.be.equals("/realms/root/realms/myRealmEndingWithDots../realms/..subRealm/authentication");
        });
        it("doesn't mangle realm alias containing '..'", () => {
            expect(fetchUrl("/authentication", {realm: "myRealmAlias..WithDots" }))
                .to.be.equals("/realms/myRealmAlias..WithDots/authentication");
        });
        it("doesn't mangle realm alias starting with '..'", () => {
            expect(fetchUrl("/authentication", {realm: "..myRealmAliasStaringWithDots" }))
                .to.be.equals("/realms/..myRealmAliasStaringWithDots/authentication");
        });
        it("doesn't mangle realm alias ending with '..'", () => {
            expect(fetchUrl("/authentication", {realm: "myRealmAliasEndingWithDots.." }))
                .to.be.equals("/realms/myRealmAliasEndingWithDots../authentication");
        });
        it("throws if path has no leading slash", () => {
            expect(() => fetchUrl("authentication")).to.throw(Error)
        });
        it("converts a backslash to a forward slash", () => {
            expect(fetchUrl("/authentication", {realm: "realmWithBackslash\\" }))
                .to.be.equals("/realms/realmWithBackslash//authentication");
        });
        it("throws an error if realm contains invalid characters ' @#$%&+?:;,=<>\"'", () => {
            expect(() => fetchUrl("/authentication", {realm: "realmWithInvalidCharacters @#$%&+?:;,=<>\"" }))
                .to.throw(Error, "[realm] parameter contains invalid characters: [ @#$%&+?:;,=<>\"]");
        });
        it("throws an error if realm containers encoded invalid characters: ' @#$%&+?:;,=\\<>\"'", () => {
            expect(() => fetchUrl("/authentication", {realm:
                    "realmWithEncodedInvalidCharacters%20%40%23%24%25%26%2B%3F%3A%3B%2C%3D%5C%3C%3E%2b%3f%3a%3b%2c%3d%5c%3c%3e%22" }))
                .to.throw(Error, "[realm] parameter contains invalid encoded characters: [ @#$%&+?:;,=<>\"]");
        });
    });
});
