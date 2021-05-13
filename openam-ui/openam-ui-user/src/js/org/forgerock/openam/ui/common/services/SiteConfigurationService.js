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
 * Copyright 2014-2019 ForgeRock AS.
 */

import { getConfiguration } from "org/forgerock/openam/ui/common/services/ServerService";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import UserProfileView from "org/forgerock/commons/ui/user/profile/UserProfileView";

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

export default SiteConfigurationService;
