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
 * Copyright 2011-2020 ForgeRock AS.
 */

import _ from "lodash";

import { getTimeLeftFromSessionInfo, updateSessionInfo } from "org/forgerock/openam/ui/user/services/SessionService";
import { isNotDefaultPath, remove, setValidated } from "org/forgerock/openam/ui/user/login/gotoUrl";
import { parseParameters } from "org/forgerock/openam/ui/common/util/uri/query";
import { refresh } from "org/forgerock/commons/ui/common/main/ViewManager";
import AuthNService from "org/forgerock/openam/ui/user/services/AuthNService";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import UserModel from "org/forgerock/openam/ui/user/UserModel";

const RESTLoginHelper = {};

RESTLoginHelper.login = function (params, successCallback, errorCallback) {
    AuthNService.getRequirements(params).then((requirements) => {
        // populate the current set of requirements with the values we have from params
        const populatedRequirements = _.clone(requirements);
        requirements.callbacks.forEach((obj, i) => {
            if (params.hasOwnProperty(`callback_${i}`)) {
                // If params for the callback are in an array form as with the
                // KbaCreateCallback, unpack the array values into each input
                if (Array.isArray(params[`callback_${i}`])) {
                    params[`callback_${i}`].forEach((obj, j) => {
                        populatedRequirements.callbacks[i].input[j].value = params[`callback_${i}`][j];
                    });
                } else {
                    populatedRequirements.callbacks[i].input[0].value = params[`callback_${i}`];
                }
            }
        });

        AuthNService.submitRequirements(populatedRequirements, params).then((result) => {
            if (result.hasOwnProperty("tokenId")) {
                RESTLoginHelper.getLoggedUser((user, remainingSessionTime) => {
                    Configuration.setProperty("loggedUser", user);
                    if (isNotDefaultPath(result.successUrl)) {
                        setValidated(result.successUrl);
                    } else {
                        remove();
                    }
                    successCallback(user, remainingSessionTime);
                    AuthNService.resetProcess();
                }, () => {
                    // Authentication successful, however the user is not authorised to access the session info.
                    errorCallback("unauthorized");
                });
            } else if (result.hasOwnProperty("authId")) {
                // re-render login form for next set of required inputs
                if (Router.currentRoute === Router.configuration.routes.login) {
                    refresh();
                } else {
                    // TODO: If using a module chain with autologin the user is
                    // currently routed to the first login screen.
                    let href = "#login";
                    const realm = Configuration.globalData.auth.subRealm;
                    if (realm) {
                        href += `/${realm}`;
                    }
                    location.href = href;
                }
            }
        }, (failedStage, errorMsg) => {
            if (failedStage > 1) {
                // re-render login form, sending back to the start of the process.
                refresh();
            }
            errorCallback(errorMsg);
        });
    });
};

RESTLoginHelper.getLoggedUser = function (successCallback, errorCallback) {
    return updateSessionInfo().then((sessionInfo) => {
        // TODO AME-11593 Call to idFromSession is required to populate the fullLoginURL, which we use later to
        // determine the parameters you logged in with. We should remove the support of fragment parameters and use
        // persistent url query parameters instead.
        ServiceInvoker.restCall({
            url: `${Constants.host}${Constants.context}/json${
                fetchUrl("/users?_action=idFromSession")}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
            type: "POST",
            errorsHandlers: { "serverError": { status: "503" }, "unauthorized": { status: "401" } }
        }).then((data) => {
            Configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
        });
        const remainingSessionTime = getTimeLeftFromSessionInfo(sessionInfo);
        return UserModel.fetchById(sessionInfo.username).then((user) => successCallback(user, remainingSessionTime));
    }, errorCallback);
};

RESTLoginHelper.getSuccessfulLoginUrlParams = function () {
    // The successfulLoginURL is populated by the server upon successful authentication,
    // not from window.location of the browser.
    const fullLoginURL = Configuration.globalData.auth.fullLoginURL;
    const paramString = fullLoginURL ? fullLoginURL.substring(fullLoginURL.indexOf("?") + 1) : "";
    return parseParameters(paramString);
};

RESTLoginHelper.removeSuccessfulLoginUrlParams = function () {
    delete Configuration.globalData.auth.fullLoginURL;
};

RESTLoginHelper.filterUrlParams = function (params) {
    const filtered = ["arg", "authIndexType", "authIndexValue", "goto", "gotoOnFail", "ForceAuth", "locale"];
    return _.reduce(_.pick(params, filtered), (result, value, key) => `${result}&${key}=${value}`, "");
};

export default RESTLoginHelper;
