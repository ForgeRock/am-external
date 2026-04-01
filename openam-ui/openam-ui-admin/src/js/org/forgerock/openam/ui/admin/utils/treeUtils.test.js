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
 * Copyright 2026 Ping Identity Corporation.
 */

import { expect } from "chai";
import { 
    filterNodesByTag,
    shouldValidateAsStandaloneNode
} from "./treeUtils";

const stateMock = {
    local: {
        config: {
            realm: {
                authentication: {
                    trees: {
                        nodeTypes: {
                            list: {}
                        }
                    }
                }
            }
        }
    }
};

describe("treeUtils", () => {
    describe("filterNodesByTag", () => {
        const nodeTypes = [
            { name: "one", tags: ["banana"] },
            { name: "two", tags: ["banana"] },
            { name: "three", tags: ["banana", "apples"] },
            { name: "four", tags: [] },
            { name: "five" },
            { name: "six", tags: ["figs", "Flower"] },
            { name: "seven", tags: ["Flower"] }
        ];
    
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
            it("returns the whole array of nodeTypes", () => {
                expect(filterNodesByTag(nodeTypes)).eql(nodeTypes);
            });
        });
    
        context("When no nodeTypes are defined", () => {
            it("returns an empty array", () => {
                expect(filterNodesByTag(undefined, "f")).eql([]);
            });
        });
    
        context("When there are no nodeTypes", () => {
            it("returns an empty array", () => {
                expect(filterNodesByTag([], "f")).eql([]);
            });
        });
    });

    describe("shouldValidateAsStandaloneNode", () => {
        it("should return false if node type is undefined", () => {
            expect(shouldValidateAsStandaloneNode(undefined, "2.0")).to.equal(false);
        });

        it("should return false if pageNodeVersion is '1.0'", () => {
            const nodeType = {
                metadata: {
                    pageNodeCompatibility: "standalone"
                }
            };
            expect(shouldValidateAsStandaloneNode(nodeType, "1.0")).to.equal(false);
        });

        it("should return false if pageNodeCompatibility is not 'standalone'", () => {
            const nodeType = {
                metadata: {
                    pageNodeCompatibility: "unsupported"
                }
            };
            expect(shouldValidateAsStandaloneNode(nodeType, "2.0")).to.equal(false);
        });

        it("should return true if pageNodeVersion is not '1.0' and compatibility is 'standalone'", () => {
            const nodeType = {
                metadata: {
                    pageNodeCompatibility: "standalone"
                }
            };
            expect(shouldValidateAsStandaloneNode(nodeType, "2.0")).to.equal(true);
        });
    });
});

