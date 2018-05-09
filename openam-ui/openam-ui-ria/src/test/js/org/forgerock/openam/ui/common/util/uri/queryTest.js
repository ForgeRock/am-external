/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "sinon"
], (sinon) => {
    let query;
    let URIUtils;

    beforeEach(() => {
        const injector = require("inject-loader!org/forgerock/openam/ui/common/util/uri/query");

        URIUtils = {
            getCurrentQueryString: sinon.stub()
        };

        query = injector({
            "org/forgerock/commons/ui/common/util/URIUtils": URIUtils
        });
    });

    describe("org/forgerock/openam/ui/common/uri/query", () => {
        describe("#urlParamsFromObject", () => {
            describe("when the argument is an object of key value pairs", () => {
                it("returns a query string", () => {
                    const params = { foo:"bar", alice:"bob" };
                    expect(query.urlParamsFromObject(params)).eql("foo=bar&alice=bob");
                });
            });
            describe("when the argument is an empty object", () => {
                it("returns an empty string", () => {
                    const params = {};
                    expect(query.urlParamsFromObject(params)).eql("");
                });
            });
            describe("when no argument is provided", () => {
                it("returns an empty string", () => {
                    expect(query.urlParamsFromObject()).eql("");
                });
            });
        });

        describe("#parseParameters", () => {
            describe("when param string is provided", () => {
                it("returns an empty object.", () => {
                    const string = "";
                    expect(query.parseParameters(string)).eql({});
                });
            });

            describe("when a param string is provided", () => {
                it("returns an object of key pair values", () => {
                    const string = "foo=bar&alice=bob";
                    expect(query.parseParameters(string)).eql({ foo:"bar", alice:"bob" });
                });
            });
        });

        describe("#getCurrentQueryParameters", () => {
            describe("when the current url contains a query", () => {
                it("returns an object of key pair values", () => {
                    URIUtils.getCurrentQueryString.returns("foo=bar&alice=bob");
                    expect(query.getCurrentQueryParameters()).eql({ foo:"bar", alice:"bob" });
                });
            });

            describe("when the current url has no query", () => {
                it("returns an empty object.", () => {
                    URIUtils.getCurrentQueryString.returns("");
                    expect(query.getCurrentQueryParameters()).eql({});
                });
            });
        });
    });
});
