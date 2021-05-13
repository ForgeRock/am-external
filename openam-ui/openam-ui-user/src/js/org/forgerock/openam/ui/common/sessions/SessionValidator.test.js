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
 * Copyright 2015-2019 ForgeRock AS.
 */

import { expect } from "chai";
import $ from "jquery";

import injector from "inject-loader!./SessionValidator";
import sinon from "sinon";

describe("org/forgerock/openam/ui/common/sessions/SessionValidator", () => {
    let Router;
    let removeLocalUserData;
    let Strategy;
    let validatePromise;
    let Validator;
    const DELAYED_START_SECONDS = 5;
    const DELAYED_START_MILLISECONDS = DELAYED_START_SECONDS * 1000;
    const LONG_DELAYED_START_SECONDS = 20;
    const LONG_DELAYED_START_MILLISECONDS = LONG_DELAYED_START_SECONDS * 1000;

    beforeEach(() => {
        validatePromise = $.Deferred();
        Strategy = sinon.stub().returns(validatePromise);
        Router = {
            routeTo: sinon.stub(),
            configuration: {
                routes: {
                    sessionExpired: {}
                }
            }
        };
        removeLocalUserData = sinon.stub();

        Validator = injector({
            "org/forgerock/commons/ui/common/main/Router" : Router,
            "org/forgerock/openam/ui/user/login/removeLocalUserData" : removeLocalUserData
        }).default;
    });

    describe("#start", () => {
        let clock;

        beforeEach(() => {
            clock = sinon.useFakeTimers({
                toFake:["setTimeout", "clearTimeout", "setInterval", "clearInterval"]
            });
        });

        afterEach(() => {
            clock.restore();
        });

        it("does not invoke strategy before the delay start time", () => {
            Validator.start(Strategy, DELAYED_START_SECONDS);
            clock.tick(DELAYED_START_MILLISECONDS - 1);

            expect(Strategy).have.not.been.called;
        });

        it("invokes strategy after the delay start time", () => {
            Validator.start(Strategy, DELAYED_START_SECONDS);
            clock.tick(DELAYED_START_MILLISECONDS);

            expect(Strategy).be.calledOnce;
        });

        context("when strategy rejects", () => {
            it("invokes Router.routeTo", () => {
                validatePromise.reject();
                Validator.start(Strategy, DELAYED_START_SECONDS);
                clock.tick(DELAYED_START_MILLISECONDS + 1);

                expect(Router.routeTo).to.be.calledOnce.calledWith(Router.configuration.routes.sessionExpired,
                    { trigger: true }
                );
            });

            it("invokes #removeLocalUserData", () => {
                validatePromise.reject();
                Validator.start(Strategy, DELAYED_START_SECONDS);
                clock.tick(DELAYED_START_MILLISECONDS + 1);

                expect(removeLocalUserData).to.be.calledOnce;
            });
        });

        context("when invoked for the 2nd time", () => {
            beforeEach(() => {
                sinon.spy(window, "clearTimeout")
                Validator.start(Strategy, DELAYED_START_SECONDS);
            });
            afterEach(() => window.clearTimeout.restore());


            it("invokes #clearTimeout", () => {
                Validator.start(Strategy, LONG_DELAYED_START_SECONDS);
                expect(window.clearTimeout).to.be.calledOnce;
            });

            it("only invokes the strategy once", () => {
                Validator.start(Strategy, LONG_DELAYED_START_SECONDS);
                clock.tick(LONG_DELAYED_START_MILLISECONDS);
                expect(Strategy).be.calledOnce;
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
