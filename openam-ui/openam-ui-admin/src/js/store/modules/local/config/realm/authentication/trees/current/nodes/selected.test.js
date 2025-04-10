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

import reducer, { remove, set } from "./selected";

const selected = {
    id: "id",
    type: "MyType"
};

describe("store/modules/local/config/realm/authentication/trees/current/nodes/selected", () => {
    describe("actions", () => {
        describe("#set", () => {
            it("creates an action", () => {
                expect(set(selected)).eql({
                    type: "local/config/realm/authentication/trees/current/nodes/selected/SET",
                    payload: selected
                });
            });
        });
        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove()).eql({
                    type: "local/config/realm/authentication/trees/current/nodes/selected/REMOVE"
                });
            });
        });
    });
    describe("reducer", () => {
        const initialState = {};

        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql(initialState);
        });

        it("handles #set action", () => {
            expect(reducer(initialState, set(selected))).eql(selected);
        });

        it("handles #remove action", () => {
            expect(reducer(selected, remove())).eql(initialState);
        });
    });
});
