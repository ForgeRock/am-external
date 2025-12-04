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

import reducer, { addOrUpdate, remove, set } from "./list";

describe("remote/config/global/secretStores/singletons/instances/list", () => {
    const instanceOne = {
        _type: {
            name: "Type One",
            _id: "typeOne"
        }
    };
    const instanceTwo = {
        file: "fileOne",
        _type: {
            name: "Type Two",
            _id: "typeTwo"
        }
    };

    const instanceTwoUpdate = {
        file: "fileTwo",
        _type: {
            name: "Type Two",
            _id: "typeTwo"
        }
    };

    describe("actions", () => {
        describe("#addOrUpdate", () => {
            it("creates an action", () => {
                expect(addOrUpdate(instanceOne)).eql({
                    type: "remote/config/global/secretStores/singletons/instances/list/ADD_OR_UPDATE",
                    payload: instanceOne
                });
            });
        });

        describe("#set", () => {
            it("creates an action", () => {
                expect(set([instanceOne, instanceTwo])).eql({
                    type: "remote/config/global/secretStores/singletons/instances/list/SET",
                    payload: [instanceOne, instanceTwo]
                });
            });
        });

        describe("#remove", () => {
            it("creates an action", () => {
                expect(remove("typeOne")).eql({
                    type: "remote/config/global/secretStores/singletons/instances/list/REMOVE",
                    payload: "typeOne"
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql([]);
        });

        it("handles #addOrUpdate action", () => {
            expect(reducer([instanceOne, instanceTwo], addOrUpdate(instanceTwoUpdate))).eql([
                instanceOne,
                instanceTwoUpdate
            ]);
        });

        it("handles #set action", () => {
            expect(reducer([], set([instanceOne, instanceTwo]))).eql([
                instanceOne,
                instanceTwo
            ]);
        });

        it("handles #remove action", () => {
            expect(reducer([instanceOne, instanceTwo], remove("typeOne"))).eql([
                instanceTwo
            ]);
        });
    });
});
