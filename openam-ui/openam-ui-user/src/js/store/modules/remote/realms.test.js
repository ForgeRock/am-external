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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { expect } from "chai";
import _ from "lodash";

import reducer, { setRealms } from "./realms";

describe("store/modules/remote/realms", () => {
    const rootRealm = { _id: "one", parentPath: null, name: "/" };
    const subRealm = { _id: "two", parentPath: "/", name: "subRealm" };
    const subSubRealm = { _id: "three", parentPath: "/subRealm", name: "subSubRealm" };

    describe("actions", () => {
        describe("#setRealms", () => {
            it("creates an action", () => {
                expect(setRealms([rootRealm, subRealm, subSubRealm])).eql({
                    type: "remote/realms/SET_REALMS",
                    payload: [rootRealm, subRealm, subSubRealm]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #setRealms action", () => {
            expect(reducer({}, setRealms([rootRealm, subRealm, subSubRealm]))).eql({
                "/": _.assign({}, rootRealm, { path: "/" }),
                "/subRealm": _.assign({}, subRealm, { path: "/subRealm" }),
                "/subRealm/subSubRealm": _.assign({}, subSubRealm, { path: "/subRealm/subSubRealm" })
            });
        });
    });
});
