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

import reducer, { addInstance, setInstances } from "./instances";

describe("store/modules/remote/config/realm/applications/oauth2/clients/groups/instances", () => {
    const groupOne = { _id: "one" };
    const groupTwo = { _id: "two" };

    describe("actions", () => {
        describe("#addInstance", () => {
            it("creates an action", () => {
                expect(addInstance(groupOne)).eql({
                    type: "remote/config/realm/applications/oauth2/groups/instances/ADD_INSTANCE",
                    payload: groupOne
                });
            });
        });
        describe("#setInstances", () => {
            it("creates an action", () => {
                expect(setInstances([groupOne, groupTwo])).eql({
                    type: "remote/config/realm/applications/oauth2/groups/instances/SET_INSTANCES",
                    payload: [groupOne, groupTwo]
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).eql({});
        });

        it("handles #addInstance action", () => {
            expect(reducer({}, addInstance(groupOne))).eql({
                "one": groupOne
            });
        });

        it("handles #setInstances action", () => {
            expect(reducer({}, setInstances([groupOne, groupTwo]))).eql({
                one: groupOne,
                two: groupTwo
            });
        });
    });
});
