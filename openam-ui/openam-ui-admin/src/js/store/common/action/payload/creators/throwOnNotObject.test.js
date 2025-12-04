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

import throwOnNotObject from "./throwOnNotObject";

describe("store/common/action/payload/creators/throwOnNotObject", () => {
    it("returns the payload", () => {
        const payload = {};

        expect(throwOnNotObject(payload)).to.eq(payload);
    });

    context("payload is not an object", () => {
        it("throws an error", () => {
            expect(() => throwOnNotObject()).to.throw(Error,
                "Invalid payload of type `undefined` supplied to action creator, expected `object`"
            );
        });
    });

    context("payload is an array", () => {
        it("throws an error", () => {
            expect(() => throwOnNotObject([])).to.throw(Error,
                "Invalid payload of type `array` supplied to action creator, expected `object`"
            );
        });
    });
});
