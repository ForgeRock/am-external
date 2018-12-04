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
 * Copyright 2014-2018 ForgeRock AS.
 */

import $ from "jquery";

import { getConfiguration } from "org/forgerock/openam/ui/common/services/ServerService";
import { updateSessionInfo } from "org/forgerock/openam/ui/user/services/SessionService";
import getCurrentFragmentParamString from "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString";
import isRealmChanged from "org/forgerock/openam/ui/common/util/isRealmChanged";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import UserProfileView from "UserProfileView";

const setRequireMapConfig = function (serverInfo) {
    if (serverInfo.kbaEnabled === "true") {
        import("org/forgerock/commons/ui/user/profile/UserProfileKBATab").then(unwrapDefaultExport).then((tab) => {
            UserProfileView.registerTab(tab);
        });
    }
    return serverInfo;
};

const SiteConfigurationService = {};

/**
 * Makes a HTTP request to the server to get its configuration
 * @param {Function} successCallback Success callback function
 * @param {Function} errorCallback   Error callback function
 */
SiteConfigurationService.getConfiguration = function (successCallback, errorCallback) {
    getConfiguration({ suppressEvents: true }).then((response) => {
        setRequireMapConfig(response);
        successCallback(response);
    }, errorCallback);
};

/**
 * Checks if realm has changed. Redirects to switch realm page if so.
 * @returns {Promise} promise empty promise
 */
SiteConfigurationService.checkForDifferences = function () {
    const deferred = $.Deferred();

    updateSessionInfo().then((sessionInfo) => {
        if (isRealmChanged()) {
            window.location.replace(`#switchRealm/${getCurrentFragmentParamString()}`);
        }
        deferred.resolve(sessionInfo);
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

export default SiteConfigurationService;
