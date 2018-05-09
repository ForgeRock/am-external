/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "sinon"
], ($, sinon) => {
    let getTimeLeftPromise;
    let MaxIdleTimeLeftStrategy;
    let SessionService;
    describe("org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy", () => {
        beforeEach(() => {
            const injector =
                require("inject-loader!org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy");

            getTimeLeftPromise = $.Deferred();

            SessionService = {
                getTimeLeft: sinon.stub().returns(getTimeLeftPromise)
            };

            MaxIdleTimeLeftStrategy = injector({
                "org/forgerock/openam/ui/user/services/SessionService": SessionService
            });
        });

        it("returns a promise", () => {
            getTimeLeftPromise.resolve();
            const func = MaxIdleTimeLeftStrategy();

            expect(func.then).to.not.be.undefined;

            return func;
        });

        context("when invoked", () => {
            it("returns the idle expiration time from session service", () => {
                getTimeLeftPromise.resolve(300);
                return MaxIdleTimeLeftStrategy().then((seconds) => {
                    expect(seconds).to.be.eq(300);
                });
            });
        });
    });
});
