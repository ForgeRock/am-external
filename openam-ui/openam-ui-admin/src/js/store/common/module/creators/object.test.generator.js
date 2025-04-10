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

import createPayload from "__test__/crest/createPayload";

/**
 * Tests that a Redux module is defined and behaves as follows:
 * 1. Reducer initial state is `null`.
 * 2. Standard set of actions are available (`#set`).
 * 3. Reducer behaviour as the result of actions are correct.
 * @module store/common/module/creators/objectTestGenerator
 * @param {string} branch Redux module branch.
 * @param {Object} module Redux module.
 * @param {Function} module.set `set` action.
 * @param {Function} module.default reducer.
 * @example
 * import objectTestGenerator from "store/common/module/creators/object.test.generator";
 * import * as schema from "./schema";
 *
 * objectTestGenerator("remote/config/realm/secrets/values/schema", schema);
 */
const object = (branch, { set, "default": reducer }) => {
    describe(`store/modules/${branch} (object)`, () => {
        describe("actions", () => {
            describe("#set", () => {
                it("creates an action", () => {
                    const payloadOne = createPayload();

                    expect(set(payloadOne)).eql({
                        type: `${branch}/SET`,
                        payload: payloadOne
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(reducer(undefined, {})).to.be.null;
            });

            it("handles #set action", () => {
                const payloadOne = createPayload();

                expect(reducer({}, set(payloadOne))).eql(payloadOne);
            });
        });
    });
};

export default object;
