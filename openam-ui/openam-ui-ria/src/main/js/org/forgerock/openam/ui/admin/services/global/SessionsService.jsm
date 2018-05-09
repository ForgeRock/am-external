/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/SessionsService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getByRealmAndUsername (realm, username) {
    const queryFilter = encodeURIComponent(`username eq "${username}" and realm eq "${realm}"`);

    return obj.serviceCall({
        url: fetchUrl(`/sessions?_queryFilter=${queryFilter}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" }
    }).then((response) => response.result);
}

export function invalidateByHandles (handles) {
    return obj.serviceCall({
        url: fetchUrl("/sessions?_action=logoutByHandle", { realm: false }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
        data: JSON.stringify({ sessionHandles: handles })
    }).then((response) => response.result);
}
