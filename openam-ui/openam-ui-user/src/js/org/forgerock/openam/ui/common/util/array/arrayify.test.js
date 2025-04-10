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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import arrayify from "./arrayify";

describe("org/forgerock/openam/ui/common/array/arrayify", () => {
    context("when argument is", () => {
        context("an array", () => {
            context("of length 0", () => {
                it("it returns an empty array", () => {
                    const args = [];

                    expect(arrayify(args)).to.be.an.instanceOf(Array).and.be.empty;
                });
            });
            context("of length 1", () => {
                it("it returns an array that contains the same elements", () => {
                    const args = ["a"];

                    expect(arrayify(args)).to.be.an.instanceOf(Array).and.have.members(args);
                });
            });
        });
    });
    context("when argument is not an array", () => {
        it("it returns the argument wrapped in an array", () => {
            const args = "a";

            expect(arrayify(args)).to.be.eql([args]);
        });
    });
});
