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
import { random } from "faker";

import createPayload from "__test__/crest/createPayload";

/**
 * Tests that a Redux module is defined and behaves as follows:
 * 1. Reducer initial state is an `Array`.
 * 2. Standard set of actions are available (`#addOrUpdate`, `#remove`, `#set`).
 * 3. Reducer behaviour as the result of actions are correct.
 * @module store/common/module/creators/arrayTestGenerator
 * @param {string} branch Redux module branch.
 * @param {Object} module Redux module.
 * @param {Function} module.addOrUpdate `addOrUpdate` action.
 * @param {Function} module.remove `remove` action.
 * @param {Function} module.set `set` action.
 * @param {Function} module.default reducer.
 * @example
 * import arrayTestGenerator from "store/common/module/creators/array.test.generator";
 * import * as list from "./list";
 *
 * arrayTestGenerator("remote/config/global/secrets/stores/types/list", list);
 */
const array = (branch, { addOrUpdate, remove, set, "default": reducer }) => {
    describe(`store/modules/${branch} (array)`, () => {
        describe("actions", () => {
            describe("#addOrUpdate", () => {
                it("creates an action", () => {
                    const payload = createPayload();

                    expect(addOrUpdate(payload)).eql({
                        type: `${branch}/ADD_OR_UPDATE`,
                        payload
                    });
                });

                context("payload is empty", () => {
                    it("throws an error", () => {
                        expect(() => addOrUpdate()).to.throw();
                    });
                });
            });

            describe("#remove", () => {
                it("creates an action", () => {
                    const payload = createPayload();

                    expect(remove(payload)).eql({
                        type: `${branch}/REMOVE`,
                        payload
                    });
                });

                context("payload is empty", () => {
                    it("throws an error", () => {
                        expect(() => remove()).to.throw();
                    });
                });
            });

            describe("#set", () => {
                it("creates an action", () => {
                    const payloadOne = createPayload();
                    const payloadTwo = createPayload();

                    expect(set([payloadOne, payloadTwo])).eql({
                        type: `${branch}/SET`,
                        payload: [payloadOne, payloadTwo]
                    });
                });

                context("payload is invalid type", () => {
                    it("throws an error", () => {
                        expect(() => set({})).to.throw();
                    });
                });
            });
        });

        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(reducer(undefined, {})).eql([]);
            });

            it("handles #addOrUpdate action (add)", () => {
                const payload = createPayload();

                expect(reducer([], addOrUpdate(payload))).eql([
                    payload
                ]);
            });

            it("handles #addOrUpdate action (update)", () => {
                const id = random.uuid();
                const payloadOne = createPayload(id);
                const payloadTwo = createPayload(id);

                expect(reducer([
                    payloadOne
                ], addOrUpdate(payloadTwo))).eql([
                    payloadTwo
                ]);
            });

            it("handles #remove action", () => {
                const payloadOne = createPayload();
                const payloadTwo = createPayload();

                expect(reducer([
                    payloadOne,
                    payloadTwo
                ], remove(payloadOne._id))).eql([
                    payloadTwo
                ]);
            });

            it("handles #set action", () => {
                const payloadOne = createPayload();
                const payloadTwo = createPayload();

                expect(reducer([], set([payloadOne, payloadTwo]))).eql([
                    payloadOne,
                    payloadTwo
                ]);
            });
        });
    });
};

export default array;
