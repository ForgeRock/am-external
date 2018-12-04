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
 * Copyright 2018 ForgeRock AS.
 */

import { expect } from "chai";

import reducer, { addOrUpdate } from "./creatables";

describe("store/modules/remote/config/realm/secretStores/types/creatables", () => {
    const creatables = { key: "value" };
    const secretStoreType = "secretStoreType";

    describe("actions", () => {
        describe("#addOrUpdate", () => {
            it("creates an action", () => {
                expect(addOrUpdate(creatables, secretStoreType)).eql({
                    meta: { type: secretStoreType },
                    payload: creatables,
                    type: "remote/config/realm/secretStores/types/creatables/ADD_OR_UPDATE"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addOrUpdate action", () => {
            expect(reducer({}, addOrUpdate(creatables, secretStoreType))).eql({
                [secretStoreType]: creatables
            });
        });
    });
});
