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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import $ from "jquery";

import { getConfiguration } from "org/forgerock/openam/ui/common/services/ServerService";
import { updateSessionInfo } from "org/forgerock/openam/ui/user/services/SessionService";
import hasLoggedAdminChanged from "org/forgerock/openam/ui/common/util/hasLoggedAdminChanged";
import isRealmChanged from "org/forgerock/openam/ui/common/util/isRealmChanged";
import redirectToUserLogin from "org/forgerock/openam/ui/common/redirectToUser/login";
import redirectToUserSwitchRealm from "org/forgerock/openam/ui/common/redirectToUser/switchRealm";
import redirectToUserServerError from "org/forgerock/openam/ui/common/redirectToUser/serverError";

const SiteConfigurationService = {};

/**
 * Makes a HTTP request to the server to get its configuration
 * @param {Function} successCallback Success callback function
 * @param {Function} errorCallback   Error callback function
 */
SiteConfigurationService.getConfiguration = function (successCallback, errorCallback) {
    getConfiguration({ suppressEvents: true }).then((response) => {
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
            redirectToUserSwitchRealm();
        }
        if (hasLoggedAdminChanged(sessionInfo.username)) {
            redirectToUserLogin();
        } else {
            deferred.resolve(sessionInfo);
        }
    }, (error) => {
        if (error.status === 503) {
            redirectToUserServerError();
        } else {
            deferred.resolve();
        }
    });

    return deferred;
};

export default SiteConfigurationService;
