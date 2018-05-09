/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () => {
    describe("org/forgerock/openam/ui/user/login/RESTLoginHelper", () => {
        let RESTLoginHelper;

        before(() => {
            const injector = require("inject-loader!org/forgerock/openam/ui/user/login/RESTLoginHelper");

            RESTLoginHelper = injector({
                "org/forgerock/openam/ui/user/services/AuthNService": {},
                "org/forgerock/openam/ui/user/UserModel": {},
                "org/forgerock/commons/ui/common/main/ViewManager": {}
            });
        });

        describe("#filterUrlParams", () => {
            it("returns a string", () => {
                expect(RESTLoginHelper.filterUrlParams()).to.be.a("string");
            });

            it("coverts an object to parameter string", () => {
                const params = {
                    arg: "argValue",
                    locale: "localeValue"
                };

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq("&arg=argValue&locale=localeValue");
            });

            it("filters out non-allowed parameters", () => {
                const params = {
                    arg: "argValue",
                    authIndexType: "authIndexTypeValue",
                    authIndexValue: "authIndexValueValue",
                    "goto": "gotoValue",
                    gotoOnFail: "gotoOnFailValue",
                    ForceAuth: "ForceAuthValue",
                    locale: "localeValue",
                    unknown: "unknown"
                };
                const expected = "&arg=argValue&authIndexType=authIndexTypeValue&authIndexValue=authIndexValueValue" +
                               "&goto=gotoValue&gotoOnFail=gotoOnFailValue&ForceAuth=ForceAuthValue&locale=localeValue";

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq(expected);
            });

            context("when all parameters are filtered out", () => {
                it("returns an empty string", () => {
                    const params = {
                        unknown: "unknown"
                    };

                    expect(RESTLoginHelper.filterUrlParams(params)).to.eq("");
                });
            });

            context("when params is \"undefined\"", () => {
                it("returns an empty string", () => {
                    expect(RESTLoginHelper.filterUrlParams(undefined)).to.eq("");
                });
            });
        });
    });
});
