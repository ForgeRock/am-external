/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const delegate = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const getPath = function () {
    return `/users/${Configuration.loggedUser.get("username")}/devices/2fa/push/`;
};

export function getAll () {
    return delegate.serviceCall({
        url: fetchUrl(`${getPath()}?_queryFilter=true`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        suppressEvents: true
    }).then((value) => value.result);
}

export function remove (uuid) {
    return delegate.serviceCall({
        url: fetchUrl(getPath() + uuid),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        suppressEvents: true,
        method: "DELETE"
    });
}
