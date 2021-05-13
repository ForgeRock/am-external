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
 * Copyright 2015-2019 ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

const TrustedDevicesService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

TrustedDevicesService.getTrustedDevices = function () {
    return TrustedDevicesService.serviceCall({
        url: fetchUrl(`/users/${Configuration.loggedUser.get("username")}/devices/trusted/?_queryId=*`),
        headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

TrustedDevicesService.deleteTrustedDevice = function (id) {
    return TrustedDevicesService.serviceCall({
        url: fetchUrl(`/users/${Configuration.loggedUser.get("username")}/devices/trusted/${id}`),
        type: "DELETE",
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

export default TrustedDevicesService;
