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

import reducer, { reset, set } from "./selected";

describe("store/modules/local/config/realm/secretStores/types/selected", () => {
    const selectedType = "selectedType";

    describe("actions", () => {
        describe("#reset", () => {
            it("creates an action", () => {
                expect(reset()).eql({
                    type: "local/config/realm/secretStores/types/selected/RESET"
                });
            });
        });

        describe("#set", () => {
            it("creates an action", () => {
                expect(set(selectedType)).eql({
                    payload: selectedType,
                    type: "local/config/realm/secretStores/types/selected/SET"
                });
            });
        });
    });
    describe("reducer", () => {
        const initialState = "";

        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql(initialState);
        });

        it("handles #reset action", () => {
            expect(reducer("test", reset())).eql(initialState);
        });

        it("handles #set action", () => {
            expect(reducer("", set(selectedType))).eql(selectedType);
        });
    });
});
