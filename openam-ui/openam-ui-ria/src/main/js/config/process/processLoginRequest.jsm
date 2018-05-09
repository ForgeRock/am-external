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
 * Copyright 2018 ForgeRock AS.
 */

import { has, indexOf } from "lodash";
import $ from "jquery";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SessionManager from "org/forgerock/commons/ui/common/main/SessionManager";

const processLoginRequest = (event) => {
    const promise = $.Deferred();
    const submitContent = (event && event.submitContent) || event || {};

    SessionManager.login(submitContent, (user) => {
        Configuration.setProperty("loggedUser", user);

        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false });

        if (!Configuration.backgroundLogin) {
            if (has(Configuration, "globalData.auth.validatedGoto")) {
                window.location.href = decodeURIComponent(Configuration.globalData.auth.validatedGoto);
                return false;
            }

            if (Configuration.gotoFragment &&
                  indexOf(["#", "", "#/", "/#"], Configuration.gotoFragment) === -1) {
                Router.navigate(Configuration.gotoFragment, { trigger: true });
                delete Configuration.gotoFragment;
            } else if (Router.checkRole(Router.configuration.routes["default"])) {
                EventManager.sendEvent(Constants.ROUTE_REQUEST, { routeName: "default", args: [] });
            } else {
                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED);
                return;
            }
        } else if (typeof $.prototype.modal === "function") {
            $(".modal.in").modal("hide");
            // There are some cases, when user is presented with login modal panel,
            // rather than a normal login view. backgroundLogin property is used to
            // indicate such cases. It should be deleted afterwards for correct
            // display of the login view later
            delete Configuration.backgroundLogin;
        }

        promise.resolve(user);
    }, (reason) => {
        reason = reason ? reason : "authenticationFailed";
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, reason);

        if (event.failureCallback) {
            event.failureCallback(reason);
        }

        promise.reject(reason);
    });

    return promise;
};

export default processLoginRequest;
