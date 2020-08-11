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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { expect } from "chai";

import reducer, { addOrUpdate } from "./schema";

describe("store/modules/remote/config/realm/secretStores/types/schema", () => {
    const schema = {
        type: "object",
        properties: {}
    };
    const secretStoreType = "secretStoreType";

    describe("actions", () => {
        describe("#addOrUpdate", () => {
            it("creates an action", () => {
                expect(addOrUpdate(schema, secretStoreType)).eql({
                    meta: { type: secretStoreType },
                    payload: schema,
                    type: "remote/config/realm/secretStores/types/schema/ADD_OR_UPDATE"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addOrUpdate action", () => {
            expect(reducer({}, addOrUpdate(schema, secretStoreType))).eql({
                [secretStoreType]: schema
            });
        });
    });
});
