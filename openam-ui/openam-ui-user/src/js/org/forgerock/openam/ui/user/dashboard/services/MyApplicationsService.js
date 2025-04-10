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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

const MyApplicationsService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

function sortApps (apps) {
    return _.map(_.sortBy(_.keys(apps), (key) => { return key; }), (key) => {
        const app = {
            id: key
        };
        _.each(apps[key], (v, k) => { app[k] = v[0]; });
        return app;
    });
}

MyApplicationsService.getMyApplications = function () {
    return MyApplicationsService.serviceCall({
        url: fetchUrl("/dashboard/assigned"),
        headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then(sortApps);
};

MyApplicationsService.getAvailableApplications = function () {
    return MyApplicationsService.serviceCall({
        url: fetchUrl("/dashboard/available"),
        headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then(sortApps);
};

export default MyApplicationsService;
