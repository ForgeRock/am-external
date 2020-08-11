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
import sinon from "sinon";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import injector from "inject-loader!./RouteTo";

describe("org/forgerock/openam/ui/common/RouteTo", () => {
    let EventManager;
    let Router;
    let RouteTo;

    beforeEach(() => {
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
            }
        };

        RouteTo = injector({
            "org/forgerock/commons/ui/common/main/EventManager": EventManager,
            "org/forgerock/commons/ui/common/main/Router": Router
        }).default;
    });

    describe("#forbiddenPage", () => {
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
});
