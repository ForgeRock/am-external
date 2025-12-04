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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

import { expect } from "chai";

import convertToDraft4PlusRequired from "./convertToDraft4PlusRequired";
import createDraft03 from "__test__/json/schema/createDraft03";
import createDraft04 from "__test__/json/schema/createDraft04";

describe("components/form/schema/convertToDraft4PlusRequired", () => {
    context("converting a Draft 03 JSON Schema", () => {
        it("creates a Draft 04+ JSON Schema", () => {
            const schema = createDraft03();

            expect(convertToDraft4PlusRequired(schema)).eql({
                properties: {
                    one: {},
                    two: {},
                    three: {}
                },
                required: ["one", "two"],
                type: "object"
            });
        });
    });

    context("converting a Draft 04 JSON Schema", () => {
        it("returns the Draft 04+ JSON Schema untouched", () => {
            const schema = createDraft04();

            expect(convertToDraft4PlusRequired(schema)).eql(schema);
        });
    });
});
