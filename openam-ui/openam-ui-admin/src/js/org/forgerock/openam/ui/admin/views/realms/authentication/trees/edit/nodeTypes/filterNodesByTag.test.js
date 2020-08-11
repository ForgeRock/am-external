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
 * Copyright 2019 ForgeRock AS.
 */

import { expect } from "chai";

import filterNodesByTag from "./filterNodesByTag";

describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/nodeTypes/filterNodesByTag", () => {
    const nodeTypes = {
        one: { name: "one", tags: ["banana"] },
        two: { name: "two", tags: ["banana"] },
        three: { name: "three", tags: ["banana", "apples"] },
        four: { name: "four", tags: [] },
        five: { name: "five" },
        six: { name: "six", tags: ["figs", "Flower"] },
        seven: { name: "seven", tags: ["Flower"] }
    };

    context("When the filter is lowercase character", () => {
        it("returns a filtered array of nodeTypes where the 'name' or 'tags' contains the character", () => {
            const filter = "f";
            expect(filterNodesByTag(nodeTypes, filter)).eql([
                { name: "four", tags: [] },
                { name: "five" },
                { name: "six", tags: ["figs", "Flower"] },
                { name: "seven", tags: ["Flower"] }
            ]);
        });
    });

    context("When the filter is an uppercase character", () => {
        it("returns a filtered array of nodeTypes where the 'name' or 'tags' contains the character", () => {
            const filter = "F";
            expect(filterNodesByTag(nodeTypes, filter)).eql([
                { name: "four", tags: [] },
                { name: "five" },
                { name: "six", tags: ["figs", "Flower"] },
                { name: "seven", tags: ["Flower"] }
            ]);
        });
    });

    context("When no matches are found by the filter", () => {
        it("returns an empty array", () => {
            const filter = "non matching filter";
            expect(filterNodesByTag(nodeTypes, filter)).eql([]);
        });
    });

    context("When an empty filter is used", () => {
        it("returns the full array of nodeTypes", () => {
            const filter = "";
            expect(filterNodesByTag(nodeTypes, filter)).eql([
                { name: "one", tags: ["banana"] },
                { name: "two", tags: ["banana"] },
                { name: "three", tags: ["banana", "apples"] },
                { name: "four", tags: [] },
                { name: "five" },
                { name: "six", tags: ["figs", "Flower"] },
                { name: "seven", tags: ["Flower"] }
            ]);
        });
    });

    context("When no filter is defined", () => {
        it("throws an error", () => {
            expect(() =>
                filterNodesByTag(nodeTypes)).to.throw(TypeError, "Cannot read property 'toLowerCase' of undefined");
        });
    });

    context("When no nodeTypes are defined", () => {
        it("returns an empty array", () => {
            expect(filterNodesByTag(undefined, "f")).eql([]);
        });
    });

    context("When there are no nodeTypes", () => {
        it("returns an empty array", () => {
            expect(filterNodesByTag({}, "f")).eql([]);
        });
    });
});
