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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { expect } from "chai";

import reducer, { set } from "./list";

describe("store/modules/remote/config/realm/authentication/trees/nodeTypes/list", () => {
    const nodeTypeOne = { _id: "one" };
    const nodeTypeTwo = { _id: "two" };

    describe("actions", () => {
        describe("#set", () => {
            it("creates an action", () => {
                expect(set([nodeTypeOne, nodeTypeTwo])).eql({
                    type: "remote/config/realm/authentication/trees/nodeTypes/list/SET",
                    payload: [nodeTypeOne, nodeTypeTwo]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #set action", () => {
            expect(reducer({}, set([nodeTypeOne, nodeTypeTwo]))).eql({
                one: nodeTypeOne,
                two: nodeTypeTwo
            });
        });
    });
});
