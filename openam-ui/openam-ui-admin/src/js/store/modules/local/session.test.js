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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import reducer, { addRealm, removeRealm } from "./session";

describe("store/modules/local/session", () => {
    describe("actions", () => {
        describe("#addRealm", () => {
            it("creates an action", () => {
                const realm = "/realmA";

                expect(addRealm(realm)).eql({
                    type: "local/session/ADD_REALM",
                    payload: realm
                });
            });
        });
        describe("#removeRealm", () => {
            it("creates an action", () => {
                expect(removeRealm()).eql({
                    type: "local/session/REMOVE_REALM"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({
                realm: undefined
            });
        });
        it("handles #addRealm action", () => {
            const realm = "/realmA";

            expect(reducer({}, addRealm(realm))).eql({
                realm: realm.toLowerCase()
            });
        });
        it("handles #removeRealm action", () => {
            expect(reducer({}, removeRealm())).eql({});
        });
    });
});
