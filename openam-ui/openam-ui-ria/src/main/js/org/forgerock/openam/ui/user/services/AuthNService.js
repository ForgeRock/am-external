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
 * Copyright 2011-2017 ForgeRock AS.
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/user/login/tokens/AuthenticationToken",
    "store/modules/local/session",
    "store/index",
    "org/forgerock/openam/ui/common/util/uri/query"
], ($, _, Messages, AbstractDelegate, Configuration, EventManager, URIUtils, fetchUrl, Constants, AuthenticationToken,
    session, store, query) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
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
        store.default.dispatch(session.addRealm(realm));
    }

    /**
     * Creates the URL for the authenticate end-point with parameters passed to the client appended.
     * @returns {string} The authenticate end-point with query parameters appended
     */
    const createAuthenticateURL = () => {
        const parameters = handleFragmentParameters(query.parseParameters(URIUtils.getCurrentFragmentQueryString()));
        /**
         * Explicity remove 'realm' to ensure the authenticate end-point is not called with it
         * TODO Remove this with AME-11109
         */
        delete parameters.realm;
        return addQueryStringToUrl(
            fetchUrl.default("/authenticate", { realm: store.default.getState().remote.info.realm }),
            query.urlParamsFromObject(parameters)
        );
    };

    obj.begin = function (options) {
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
        return obj.serviceCall(serviceCall).then((requirements) => requirements,
            (jqXHR) => {
                // some auth processes might throw an error fail immediately
                const errorBody = $.parseJSON(jqXHR.responseText);
                // if the error body contains an authId, then we might be able to
                // continue on after this error to the next module in the chain
                if (errorBody.hasOwnProperty("authId")) {
                    return obj.submitRequirements(errorBody)
                        .then((requirements) => {
                            obj.resetProcess();
                            return requirements;
                        }, () => errorBody
                        );
                } else if (errorBody.code && errorBody.code === 400) {
                    return {
                        message: errorBody.message,
                        type: Messages.TYPE_DANGER
                    };
                }
                return errorBody;
            });
    };
    obj.handleRequirements = function (requirements) {
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
            if (!AuthenticationToken.get() && _.find(requirements.callbacks, callbackTracking)) {
                AuthenticationToken.set(requirements.authId);
            }
        } else if (isAuthenticated) {
            addRealmToStore(requirements.realm);
        }
    };
    obj.submitRequirements = function (requirements, options) {
        const timeOutErrorCode = "110";
        const processSucceeded = (requirements) => {
            obj.handleRequirements(requirements);
            return requirements;
        };
        const processFailed = (reason) => {
            const failedStage = requirementList.length;
            obj.resetProcess();
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
        return obj.serviceCall(serviceCall).then(processSucceeded, (jqXHR) => {
            let oldReqs;
            let errorBody;
            const currentStage = requirementList.length;
            let message;
            const reasonThatWillNotBeDisplayed = 1;

            if (_.get(jqXHR, "responseJSON.detail.errorCode") === timeOutErrorCode) {
                // we timed out, so let's try again with a fresh session
                oldReqs = requirementList[0];
                obj.resetProcess();
                return obj.begin().then((requirements) => {
                    obj.handleRequirements(requirements);
                    if (requirements.hasOwnProperty("authId")) {
                        if (currentStage === 1) {
                            /**
                             * if we were at the first stage when the timeout occurred,
                             * try to do it again immediately.
                             */
                            oldReqs.authId = requirements.authId;
                            return obj.submitRequirements(oldReqs).then(processSucceeded, processFailed);
                        } else {
                            // restart the process at the beginning
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loginTimeout");
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
                    return obj.submitRequirements(errorBody).then(processSucceeded, processFailed);
                } else {
                    obj.resetProcess();
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
    obj.resetProcess = function () {
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
    obj.getRequirements = function (args) {
        if (AuthenticationToken.get()) {
            const paramsWithAuthToken = _.extend({ authId: AuthenticationToken.get() },
                Configuration.globalData.auth.urlParams);
            AuthenticationToken.remove();
            return obj.submitRequirements(paramsWithAuthToken).done(() => {
                knownAuth = _.clone(Configuration.globalData.auth);
            });
        } else if (requirementList.length === 0 || hasRealmChanged() || hasAuthIndexChanged()) {
            obj.resetProcess();
            return obj.begin(args)
                .then((requirements) => {
                    obj.handleRequirements(requirements);
                    return requirements;
                }, (error) => error);
        } else {
            return $.Deferred().resolve(requirementList[requirementList.length - 1]);
        }
    };
    return obj;
});
