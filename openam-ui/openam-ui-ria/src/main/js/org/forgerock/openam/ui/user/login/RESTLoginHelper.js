/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/user/services/AuthNService",
    "org/forgerock/openam/ui/user/services/SessionService",
    "org/forgerock/openam/ui/user/UserModel",
    "org/forgerock/openam/ui/user/login/logout",
    "org/forgerock/openam/ui/common/util/uri/query",
    "org/forgerock/openam/ui/user/login/gotoUrl"
], ($, _, Configuration, Router, ServiceInvoker, ViewManager, Constants, URIUtils, fetchUrl,
    AuthNService, SessionService, UserModel, logout, query, gotoUrl) => { // eslint-disable-line padded-blocks

    const obj = {};

    obj.login = function (params, successCallback, errorCallback) {
        AuthNService.getRequirements(params).then((requirements) => {
            // populate the current set of requirements with the values we have from params
            const populatedRequirements = _.clone(requirements);
            _.each(requirements.callbacks, (obj, i) => {
                if (params.hasOwnProperty(`callback_${i}`)) {
                    populatedRequirements.callbacks[i].input[0].value = params[`callback_${i}`];
                }
            });

            AuthNService.submitRequirements(populatedRequirements, params).then((result) => {
                if (result.hasOwnProperty("tokenId")) {
                    obj.getLoggedUser((user) => {
                        Configuration.setProperty("loggedUser", user);
                        if (gotoUrl.isNotDefaultPath(result.successUrl)) {
                            gotoUrl.setValidated(result.successUrl);
                        } else {
                            gotoUrl.remove();
                        }
                        successCallback(user);
                        AuthNService.resetProcess();
                    }, errorCallback);
                } else if (result.hasOwnProperty("authId")) {
                    // re-render login form for next set of required inputs
                    if (Router.currentRoute === Router.configuration.routes.login) {
                        ViewManager.refresh();
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
                    ViewManager.refresh();
                }
                errorCallback(errorMsg);
            });
        });
    };

    obj.getLoggedUser = function (successCallback, errorCallback) {
        const noSessionHandler = (xhr) => {
            if (_.get(xhr, "responseJSON.code") === 404) {
                errorCallback("loggedIn");
            } else {
                errorCallback();
            }
        };
        // TODO AME-11593 Call to idFromSession is required to populate the fullLoginURL, which we use later to
        // determine the parameters you logged in with. We should remove the support of fragment parameters and use
        // persistent url query parameters instead.
        ServiceInvoker.restCall({
            url: `${Constants.host}${Constants.context}/json${
                fetchUrl.default("/users?_action=idFromSession")}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
            type: "POST",
            errorsHandlers: { "serverError": { status: "503" }, "unauthorized": { status: "401" } }
        }).then((data) => {
            Configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
        });

        return SessionService.updateSessionInfo().then((data) => {
            return UserModel.fetchById(data.username).then(successCallback);
        }, noSessionHandler);
    };

    obj.getSuccessfulLoginUrlParams = function () {
        // The successfulLoginURL is populated by the server upon successful authentication,
        // not from window.location of the browser.
        const fullLoginURL = Configuration.globalData.auth.fullLoginURL;
        const paramString = fullLoginURL ? fullLoginURL.substring(fullLoginURL.indexOf("?") + 1) : "";
        return query.parseParameters(paramString);
    };

    obj.removeSuccessfulLoginUrlParams = function () {
        delete Configuration.globalData.auth.fullLoginURL;
    };

    obj.filterUrlParams = function (params) {
        const filtered = ["arg", "authIndexType", "authIndexValue", "goto", "gotoOnFail", "ForceAuth", "locale"];
        return _.reduce(_.pick(params, filtered), (result, value, key) => `${result}&${key}=${value}`, "");
    };

    // called by commons
    obj.logout = function (successCallback, errorCallback) {
        logout.default().then(successCallback, errorCallback);
    };

    return obj;
});
