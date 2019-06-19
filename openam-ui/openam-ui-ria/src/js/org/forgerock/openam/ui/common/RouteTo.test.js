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
import sinon from "sinon";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import injector from "inject-loader!./RouteTo";

describe("org/forgerock/openam/ui/common/RouteTo", () => {
    let Configuration;
    let EventManager;
    let logout;
    let logoutPromise;
    let Router;
    let RouteTo;
    let URIUtils;

    beforeEach(() => {
        Configuration = {
            globalData: {
                authorizationFailurePending: true
            },
            setProperty: sinon.stub()
        };

        EventManager = {
            sendEvent: sinon.stub()
        };

        Router = {
            configuration: {
                routes: {
                    forbidden: {
                        url: /.*/
                    },
                    login: {
                        url: "loginUrl"
                    }
                }
            },
            routeTo: sinon.stub()
        };

        logoutPromise = $.Deferred();
        logout = sinon.stub().returns(logoutPromise);

        URIUtils = {
            getCurrentFragment: sinon.stub().returns("page")
        };

        RouteTo = injector({
            "org/forgerock/commons/ui/common/main/Configuration": Configuration,
            "org/forgerock/commons/ui/common/main/EventManager": EventManager,
            "org/forgerock/openam/ui/user/login/logout": logout,
            "org/forgerock/commons/ui/common/main/Router": Router,
            "org/forgerock/commons/ui/common/util/URIUtils": URIUtils
        }).default;
    });

    describe("#setGotoFragment", () => {
        context("when a gotoFragment is not set and the current hash does not match the login route's URL", () => {
            it("sets the gotoFragment to be the current hash", () => {
                RouteTo.setGotoFragment();
                expect(Configuration.setProperty).to.be.calledOnce.calledWith("gotoFragment", "#page");
            });
        });
    });

    describe("#forbiddenPage", () => {
        it("deletes \"authorizationFailurePending\" attribute Configuration.globalData", () => {
            RouteTo.forbiddenPage();

            expect(Configuration.globalData).to.not.have.ownProperty("authorizationFailurePending");
        });
        it("sends EVENT_CHANGE_VIEW event", () => {
            RouteTo.forbiddenPage();

            expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_CHANGE_VIEW, {
                route: Router.configuration.routes.forbidden,
                fromRouter: true
            });
        });
    });

    describe("#forbiddenError", () => {
        it("sends EVENT_DISPLAY_MESSAGE_REQUEST event", () => {
            RouteTo.forbiddenError();

            expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
                "unauthorized");
        });
    });

    describe("#logout", () => {

        beforeEach(() => {
            logoutPromise.resolve();
            sinon.spy(RouteTo, "setGotoFragment");
        });

        afterEach(() => {
            RouteTo.setGotoFragment.restore();
        });

        it("invokes #setGotoFragment", () => {
            RouteTo.logout();

            expect(RouteTo.setGotoFragment).to.be.calledOnce;
        });

        context("when logout is successful", () => {
            it("invoked Router.routeTo login ", () => {
                logoutPromise.resolve();
                RouteTo.logout();

                expect(Router.routeTo).to.be.calledWith(Router.configuration.routes.login, { trigger: true });
            });
        });

        context("when logout is unsuccessful", () => {
            it("invoked Router.routeTo login", () => {
                logoutPromise.resolve();
                RouteTo.logout();

                expect(Router.routeTo).to.be.calledWith(Router.configuration.routes.login, { trigger: true });
            });
        });
    });

    describe("#loginDialog", () => {
        it("sends EVENT_SHOW_LOGIN_DIALOG event", () => {
            RouteTo.loginDialog();

            expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_SHOW_LOGIN_DIALOG);
        });
    });
});
