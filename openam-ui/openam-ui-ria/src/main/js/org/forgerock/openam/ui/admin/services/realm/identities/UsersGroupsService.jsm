/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/identities/UsersGroupsService
 */
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getSchema (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${encodeURIComponent(id)}/groups?_action=schema`, { realm }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=2.1,resource=1.0" }
    });
}

export function get (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${encodeURIComponent(id)}/groups?_queryFilter=true`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=1.0" }
    }).then((response) => response.result);
}

export function update (realm, id, groups) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${encodeURIComponent(id)}/groups?_action=updateMemberships`, { realm }),
        type: "POST",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=1.0"
        },
        data: JSON.stringify({ groups })
    });
}
