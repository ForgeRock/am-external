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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

/* eslint-disable max-len */

import { expect } from "chai";
import { decodeJwt } from "./JwtUtils";

describe("Jwt Utils", () => {
    it("Decodes a jwt token", () => {
        const jwts = [
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJuYW1lIjoiSm9obiBEb2UifQ.vRAEAC06-_uIFl9QnpmxAGvPYg5-eHAHY7mo1i3Uuh8",
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJyZWFsbSI6Ii8iLCJzZXNzaW9uSWQiOiIxMjMifQ.pSWgo2-wBVuC1LHgesvpie2khtbvb8uRLAq9Ul0gjSA",
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdXRoSW5kZXhWYWx1ZSI6IlRlc3RUcmVlIiwiYXV0aEluZGV4VHlwZSI6InNlcnZpY2UiLCJleHAiOjE2ODYwNDI0MzIsImlhdCI6MTY4NjA0MjEzMn0.zL9BdI0aU3ike9zvencmsAVpRvf_MFdP-KKvTUjHDBc"
        ];

        expect(decodeJwt(jwts[0])).to.deep.equal({ sub: "123", name: "John Doe" });
        expect(decodeJwt(jwts[1])).to.deep.equal({ realm: "/", sessionId: "123" });
        expect(decodeJwt(jwts[2])).to.deep.equal({ authIndexValue: "TestTree", authIndexType: "service", exp: 1686042432, iat: 1686042132 });
    });

    it("Returns an empty object if decodeJwt fails", () => {
        const jwts = [
            "",
            "123",
            {},
            true,
            123
        ];

        expect(decodeJwt(jwts[0])).to.deep.equal({ });
        expect(decodeJwt(jwts[1])).to.deep.equal({ });
        expect(decodeJwt(jwts[2])).to.deep.equal({ });
        expect(decodeJwt(jwts[3])).to.deep.equal({ });
        expect(decodeJwt(jwts[4])).to.deep.equal({ });
    });
});
