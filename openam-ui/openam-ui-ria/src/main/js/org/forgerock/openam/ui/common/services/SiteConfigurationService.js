/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/services/ServerService",
    "org/forgerock/openam/ui/common/util/isRealmChanged",
    "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString",
    "org/forgerock/openam/ui/user/services/SessionService",
    "UserProfileView"
], ($, Configuration, ServerService, isRealmChanged, getCurrentFragmentParamString, SessionService,
    UserProfileView) => {
    isRealmChanged = isRealmChanged.default;
    getCurrentFragmentParamString = getCurrentFragmentParamString.default;

    const obj = {};
    const setRequireMapConfig = function (serverInfo) {
        if (serverInfo.kbaEnabled === "true") {
            require(["org/forgerock/commons/ui/user/profile/UserProfileKBATab"], (tab) => {
                UserProfileView.registerTab(tab);
            });
        }
        return serverInfo;
    };

    /**
     * Makes a HTTP request to the server to get its configuration
     * @param {Function} successCallback Success callback function
     * @param {Function} errorCallback   Error callback function
     */
    obj.getConfiguration = function (successCallback, errorCallback) {
        ServerService.getConfiguration({ suppressEvents: true }).then((response) => {
            setRequireMapConfig(response);
            successCallback(response);
        }, errorCallback);
    };

    /**
     * Checks if realm has changed. Redirects to switch realm page if so.
     * @returns {Promise} promise empty promise
     */
    obj.checkForDifferences = function () {
        const deferred = $.Deferred();

        SessionService.updateSessionInfo().then((response) => {
            if (isRealmChanged()) {
                window.location.replace(`#switchRealm/${getCurrentFragmentParamString()}`);
            }
            deferred.resolve(response);
        }, (error) => {
            if (error.status === 503) {
                window.location.pathname = `${window.location.pathname}503.html`;
                deferred.reject(error);
            } else {
                deferred.resolve();
            }
        });

        return deferred;
    };

    return obj;
});
