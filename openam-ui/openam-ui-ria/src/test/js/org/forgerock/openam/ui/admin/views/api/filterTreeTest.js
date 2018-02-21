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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/admin/views/api/filterTree"
], (filterTree) => {
    let data;
    beforeEach(() => {
        data = [{
            id : "aa",
            path : "aa"
        }, {
            id : "bb",
            path : "bb"
        }, {
            id : "cc",
            path : "cc",
            children: [{
                id : "aa",
                path : "cc/aa"
            }, {
                id : "bb",
                path : "cc/bb",
                children: [{
                    id : "aa",
                    path : "cc/bb/aa"
                }, {
                    id : "bb",
                    path : "cc/bb/bb"
                }]
            }]
        }];
    });

    describe("org/forgerock/openam/ui/admin/views/api/filterTree", () => {
        context("filter is empty", () => {
            it("returns the original values", () => {
                const expected = data;
                expect(filterTree.default(data)).to.be.eql(expected);
            });
        });

        context("filter on 'a'", () => {
            it("returns all the objects with 'a' in their path plus their parent nodes", () => {
                const expected = [{
                    id : "aa",
                    path : "aa"
                }, {
                    id : "cc",
                    path : "cc",
                    children: [{
                        id : "aa",
                        path : "cc/aa"
                    }, {
                        id : "bb",
                        path : "cc/bb",
                        children: [{
                            id : "aa",
                            path : "cc/bb/aa"
                        }]
                    }]
                }];
                expect(filterTree.default(data, "a")).to.be.eql(expected);
            });
        });

        context("filter on 'bb'", () => {
            it("returns all the objects with 'bb' in their path plus their parent nodes", () => {
                const expected = [{
                    id : "bb",
                    path : "bb"
                }, {
                    id : "cc",
                    path : "cc",
                    children: [{
                        id : "bb",
                        path : "cc/bb",
                        children: [{
                            id : "aa",
                            path : "cc/bb/aa"
                        }, {
                            id : "bb",
                            path : "cc/bb/bb"
                        }]
                    }]
                }];
                expect(filterTree.default(data, "bb")).to.be.eql(expected);
            });
        });

        context("filter on 'C'", () => {
            it("returns all the objects with 'C' in their path plus their parent nodes", () => {
                const expected = [{
                    id : "cc",
                    path : "cc",
                    children: [{
                        id : "aa",
                        path : "cc/aa"
                    }, {
                        id : "bb",
                        path : "cc/bb",
                        children: [{
                            id : "aa",
                            path : "cc/bb/aa"
                        }, {
                            id : "bb",
                            path : "cc/bb/bb"
                        }]
                    }]
                }];
                expect(filterTree.default(data, "C")).to.be.eql(expected);
            });
        });
    });
});
