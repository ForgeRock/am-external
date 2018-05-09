/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
