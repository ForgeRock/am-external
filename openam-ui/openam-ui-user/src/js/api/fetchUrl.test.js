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
        it("throws if path has no leading slash", () => {
            expect(() => fetchUrl("authentication")).to.throw(Error)
        });
    });
});
