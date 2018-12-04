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

import reducer, { add } from "./positions";

const id = "pageId";
const position = { height: 100, width: 100, x: 50, y: 75 };

describe("store/modules/local/config/realm/authentication/trees/current/nodes/pages/positions", () => {
    describe("actions", () => {
        describe("#add", () => {
            it("creates an action", () => {
                expect(add(id, position)).eql({
                    type: "local/config/realm/authentication/trees/current/nodes/pages/positions/ADD",
                    payload: {
                        [id]: position
                    }
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #set action", () => {
            expect(reducer({}, add(id, position))).eql({
                pageId: position
            });
        });
    });
});
