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
 * Copyright 2011-2019 ForgeRock AS.
 */
import $ from "jquery";
import _ from "lodash";

import { addRealm } from "store/modules/local/session";
import { get, remove, set } from "org/forgerock/openam/ui/user/login/tokens/AuthenticationToken";
import { parseParameters, urlParamsFromObject } from "org/forgerock/openam/ui/common/util/uri/query";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import store from "store";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const AuthNService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
let requirementList = [];
// to be used to keep track of the attributes associated with whatever requirementList contains
let knownAuth = {};

/**
 * Merges the provided parameters with anything present in Configuration.globalData.auth.urlParams.
 * This might be the query string or fragment query string depending on what's present on the client URL.
 * @param   {Object} params parameters to merge into
 * @returns {Object} Merge parameters
 */
function handleFragmentParameters (params) {
    if (Configuration.globalData.auth.urlParams) {
        _.extend(params, Configuration.globalData.auth.urlParams);
    }

    return params;
}

function addQueryStringToUrl (url, queryString) {
    if (_.isEmpty(queryString)) {
        return url;
    }

    const delimiter = url.indexOf("?") === -1 ? "?" : "&";
    return `${url}${delimiter}${queryString}`;
}

function addRealmToStore (realm) {
    store.dispatch(addRealm(realm));
}

/**
 * Creates the URL for the authenticate end-point with parameters passed to the client appended.
 * @returns {string} The authenticate end-point with query parameters appended
 */
const createAuthenticateURL = () => {
    const parameters = handleFragmentParameters(parseParameters(URIUtils.getCurrentFragmentQueryString()));
    /**
     * Explicity remove 'realm' to ensure the authenticate end-point is not called with it
     * TODO Remove this with AME-11109
     */
    delete parameters.realm;
    return addQueryStringToUrl(
        fetchUrl("/authenticate", { realm: store.getState().remote.info.realm }),
        urlParamsFromObject(parameters)
    );
};

AuthNService.begin = function (options) {
    knownAuth = _.clone(Configuration.globalData.auth);
    const serviceCall = {
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.1" },
        data: "",
        url: createAuthenticateURL(),
        errorsHandlers: {
            "unauthorized": { status: "401" },
            "bad request": { status: "400" }
        }
    };
    _.assign(serviceCall, options);
    return AuthNService.serviceCall(serviceCall).then((requirements) => requirements,
        (jqXHR) => {
            // some auth processes might throw an error fail immediately
            let errorBody;
            try {
                errorBody = $.parseJSON(jqXHR.responseText);
            } catch (parseErr) {
                console.warn(parseErr);
                return {
                    message: $.t("config.messages.CommonMessages.unknown"),
                    type: Messages.TYPE_DANGER
                };
            }
            // if the error body contains an authId, then we might be able to
            // continue on after this error to the next module in the chain
            if (errorBody.hasOwnProperty("authId")) {
                return AuthNService.submitRequirements(errorBody)
                    .then((requirements) => {
                        AuthNService.resetProcess();
                        return requirements;
                    }, () => errorBody
                    );
            } else if (errorBody.code && errorBody.code === 400) {
                return {
                    message: errorBody.message,
                    type: Messages.TYPE_DANGER
                };
            }
            throw errorBody;
        });
};
AuthNService.handleRequirements = function (requirements) {
    //callbackTracking allows us to determine if we're expecting to return having gone away
    function callbackTracking (callback) {
        return callback.type === "RedirectCallback" && _.find(callback.output, {
            name: "trackingCookie",
            value: true
        });
    }

    const isAuthenticated = requirements.hasOwnProperty("tokenId");

    if (requirements.hasOwnProperty("authId")) {
        requirementList.push(requirements);
        Configuration.globalData.auth.currentStage = requirementList.length;
        if (!get() && _.find(requirements.callbacks, callbackTracking)) {
            set(requirements.authId);
        }
    } else if (isAuthenticated) {
        Messages.hideMessages();
        addRealmToStore(requirements.realm);
    }
};
AuthNService.submitRequirements = function (requirements, options) {
    const timeOutErrorCode = "110";
    const processSucceeded = (requirements) => {
        AuthNService.handleRequirements(requirements);
        return requirements;
    };
    const processFailed = (reason) => {
        const failedStage = requirementList.length;
        AuthNService.resetProcess();
        return [failedStage, reason];
    };
    const goToFailureUrl = (errorBody) => {
        if (errorBody.detail && errorBody.detail.failureUrl) {
            console.log(errorBody.detail.failureUrl);
            window.location.href = errorBody.detail.failureUrl;
        }
    };
    const serviceCall = {
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.1" },
        data: JSON.stringify(requirements),
        url: createAuthenticateURL(),
        errorsHandlers: {
            "unauthorized": { status: "401" },
            "Internal Server Error ": { status: "500" }
        }
    };
    _.assign(serviceCall, options);
    return AuthNService.serviceCall(serviceCall).then(processSucceeded, (jqXHR) => {
        let oldReqs;
        let errorBody;
        const currentStage = requirementList.length;
        let message;
        const reasonThatWillNotBeDisplayed = 1;

        if (_.get(jqXHR, "responseJSON.detail.errorCode") === timeOutErrorCode) {
            // we timed out, so let's try again with a fresh session
            oldReqs = requirementList[0];
            AuthNService.resetProcess();
            return AuthNService.begin().then((requirements) => {
                AuthNService.handleRequirements(requirements);
                if (requirements.hasOwnProperty("authId")) {
                    if (currentStage === 1) {
                        /**
                         * if we were at the first stage when the timeout occurred,
                         * try to do it again immediately.
                         */
                        oldReqs.authId = requirements.authId;
                        return AuthNService.submitRequirements(oldReqs).then(processSucceeded, processFailed);
                    } else {
                        // restart the process at the beginning
                        message = $.t("config.messages.CommonMessages.loginTimeout");
                        Messages.addMessage({ message, type: Messages.TYPE_INFO });
                        return requirements;
                    }
                } else {
                    return requirements;
                }
            /**
             * this is very unlikely, since it would require a call to .begin() to fail
             * after having succeeded once before
             */
            }, processFailed);
        } else if (jqXHR.status === 500) {
            if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
                message = jqXHR.responseJSON.message;
            } else {
                message = $.t("config.messages.CommonMessages.unknown");
            }
            Messages.addMessage({ message, type: Messages.TYPE_DANGER });
        } else { // we have a 401 unauthorized response
            errorBody = $.parseJSON(jqXHR.responseText);
            // if the error body has an authId property, then we may be
            // able to advance beyond this error
            if (errorBody.hasOwnProperty("authId")) {
                return AuthNService.submitRequirements(errorBody).then(processSucceeded, processFailed);
            } else {
                AuthNService.resetProcess();
                Messages.addMessage({
                    message: errorBody.message,
                    type: Messages.TYPE_DANGER
                });
                goToFailureUrl(errorBody);
                // The reason used here will not be translated into a common message and hence not displayed.
                // This is so that only the message above is displayed.
                return $.Deferred().reject(currentStage, reasonThatWillNotBeDisplayed).promise();
            }
        }
    });
};
AuthNService.resetProcess = function () {
    requirementList = [];
};
function hasRealmChanged () {
    const auth = Configuration.globalData.auth;
    return auth.subRealm !== knownAuth.subRealm ||
        _.get(auth, "urlParams.realm") !== _.get(knownAuth, "urlParams.realm");
}
function hasAuthIndexChanged () {
    const auth = Configuration.globalData.auth;
    return _.get(auth, "urlParams.authIndexType") !== _.get(knownAuth, "urlParams.authIndexType") ||
        _.get(auth, "urlParams.authIndexValue") !== _.get(knownAuth, "urlParams.authIndexValue");
}
/**
 * Checks if a RedirectCallback is present in the "callbacks" object of the provided requirementList.
 * @param {Array.<Object>} requirementList list of requirements to check.
 * @returns {Boolean} if a RedirectCallback is present in the last requirement callbacks.
 */
function hasRedirectCallback (requirementList) {
    return requirementList.length !== 0 &&
        _.some(_.last(requirementList).callbacks, "type", "RedirectCallback");
}
AuthNService.getRequirements = function (args) {
    if (get() && !hasRedirectCallback(requirementList)) {
        const paramsWithAuthToken = _.extend({ authId: get() },
            Configuration.globalData.auth.urlParams);
        remove();
        return AuthNService.submitRequirements(paramsWithAuthToken).then((requirements) => {
            knownAuth = _.clone(Configuration.globalData.auth);
            return requirements;
        });
    } else if (requirementList.length === 0 || hasRealmChanged() || hasAuthIndexChanged()) {
        AuthNService.resetProcess();
        return AuthNService.begin(args).then((requirements) => {
            AuthNService.handleRequirements(requirements);
            return requirements;
        });
    } else {
        return $.Deferred().resolve(requirementList[requirementList.length - 1]);
    }
};
export default AuthNService;
