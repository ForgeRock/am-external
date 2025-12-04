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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

import { expect } from "chai";

import getDefaultValues from "./getDefaultValues"

describe("#getDefaultValues", () => {
    it("flattens hierarchy", () => {
        const schema = {
            type: "object",
            properties: {
                assertionTime: {
                    type: "object",
                    properties: {
                        notBeforeTimeSkew: {
                            type: "integer",
                            minimum: 0,
                            default: 600
                        }
                    }
                }
            }
        };
        const defaults = getDefaultValues(schema);

        expect(defaults).to.deep.equal({
            assertionTime: {
                notBeforeTimeSkew: 600
            }
        });
    });

    it("excludes properties without default values", () => {
        const schema = {
            type: "object",
            properties: {
                assertionTime: {
                    type: "object",
                    properties: {
                        notBeforeTimeSkew: {
                            type: "integer",
                            minimum: 0
                        }
                    }
                }
            }
        };
        const defaults = getDefaultValues(schema);
        expect(defaults).to.contain.keys("assertionTime");
        expect(defaults.assertionTime).not.to.have.keys("notBeforeTimeSkew");
    });
});
