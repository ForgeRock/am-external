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
 * Copyright 2015-2018 ForgeRock AS.
 */

import { expect } from "chai";
import $ from "jquery";

import injector from "inject-loader!./SessionValidator";
import sinon from "sinon";

describe("org/forgerock/openam/ui/common/sessions/SessionValidator", () => {
    let logout;
    let Strategy;
    let validatePromise;
    let Validator;

    beforeEach(() => {
        validatePromise = $.Deferred();

        Strategy = sinon.stub().returns(validatePromise);

        logout = sinon.stub().returns($.Deferred());

        Validator = injector({
            "org/forgerock/openam/ui/user/login/logout": logout
        }).default;
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

                expect(logout).to.be.calledOnce;
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
        beforeEach(() => sinon.spy(window, "clearTimeout"));
        afterEach(() => window.clearTimeout.restore());

        it("invokes #clearTimeout", () => {
            Validator.stop();

            expect(window.clearTimeout).to.be.calledOnce;
        });
    });
});