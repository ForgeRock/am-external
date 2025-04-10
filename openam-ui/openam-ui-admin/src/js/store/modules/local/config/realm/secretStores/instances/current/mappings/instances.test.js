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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import reducer, { addOrUpdate, remove, set } from "./instances";

describe("store/modules/local/config/realm/secretStores/instances/current/mappings/instances", () => {
    const instanceOne = {
        _id: "one",
        purpose: "purposeOne",
        alias: "aliasOne"
    };
    const instanceTwo = {
        _id: "two",
        purpose: "purposeTwo",
        alias: "aliasTwo"
    };
    const instanceTwoModified = {
        _id: "two",
        purpose: "purposeTwoModified",
        alias: "aliasTwoModified"
    };

    describe("actions", () => {
        describe("#addOrUpdate", () => {
            it("creates an action", () => {
                expect(addOrUpdate(instanceOne)).eql({
                    payload: instanceOne,
                    type: "local/config/realm/secretStores/instances/current/mappings/instances/ADD_OR_UPDATE"
                });
            });
        });

        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove("one")).eql({
                    type: "local/config/realm/secretStores/instances/current/mappings/instances/REMOVE",
                    payload: "one"
                });
            });
        });

        describe("#set", () => {
            it("creates an action", () => {
                expect(set([instanceOne])).eql({
                    payload: [instanceOne],
                    type: "local/config/realm/secretStores/instances/current/mappings/instances/SET"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql([]);
        });

        it("handles #addOrUpdate action", () => {
            expect(reducer([instanceOne, instanceTwo], addOrUpdate(instanceTwoModified))).eql([
                instanceOne, instanceTwoModified
            ]);
        });

        it("handles #remove action", () => {
            expect(reducer([instanceOne, instanceTwo], remove("one"))).eql([
                instanceTwo
            ]);
        });

        it("handles #set action", () => {
            expect(reducer([], set([instanceOne, instanceTwo]))).eql([instanceOne, instanceTwo]);
        });
    });
});
