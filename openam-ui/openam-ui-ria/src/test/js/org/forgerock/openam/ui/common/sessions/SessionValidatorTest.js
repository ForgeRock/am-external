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
    let logout;
    let Strategy;
    let validatePromise;
    let Validator;
    describe("org/forgerock/openam/ui/common/sessions/SessionValidator", () => {
        beforeEach(() => {
            const injector = require("inject-loader!org/forgerock/openam/ui/common/sessions/SessionValidator");

            validatePromise = $.Deferred();

            Strategy = sinon.stub().returns(validatePromise);

            logout = {
                "default": sinon.stub().returns($.Deferred())
            };

            Validator = injector({
                "org/forgerock/openam/ui/user/login/logout": logout
            });
        });

        describe("#start", () => {
            let clock;

            beforeEach(() => {
                clock = sinon.useFakeTimers();
            });

            afterEach(() => {
                clock.restore();
            });

            it("invokes strategy immediately", () => {
                Validator.start(Strategy);

                clock.tick(1000);

                expect(Strategy).be.calledOnce;
            });

            context("when strategy rejects", () => {
                it("invokes #logout", () => {
                    validatePromise.reject();

                    Validator.start(Strategy);

                    clock.tick(1000);

                    expect(logout.default).to.be.calledOnce;
                });
            });

            context("when invoked for the 2nd time", () => {
                beforeEach(() => {
                    Validator.start(Strategy);
                });

                it("throws error", () => {
                    expect(() => {
                        Validator.start(Strategy);
                    }).to.throw(Error, "Validator has already been started");
                });

                context("when #stop has been invoked beforehand", () => {
                    it("doesn not throw error", () => {
                        Validator.stop();

                        expect(() => {
                            Validator.start(Strategy);
                        }).to.not.throw(Error);
                    });
                });
            });
        });

        describe("#stop", () => {
            it("invokes #clearTimeout", () => {
                sinon.spy(window, "clearTimeout");

                Validator.stop();

                expect(clearTimeout).to.be.calledOnce;
            });
        });
    });
});
