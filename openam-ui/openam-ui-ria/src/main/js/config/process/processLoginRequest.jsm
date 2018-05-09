/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
