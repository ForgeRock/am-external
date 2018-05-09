/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/identities/UsersServicesService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getSchema (realm, type, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services/${type}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    });
}

export function get (realm, type, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services/${type}`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
}

export function update (realm, type, userId, data) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services/${type}`, { realm }),
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        data: data.toJSON()
    });
}

export function remove (realm, userId, types) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=unassignServices`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data: JSON.stringify({ serviceNames: types })
    });
}

export function getAllTypes (realm, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=getAllTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => response.result);
}

export function getAllInstances (realm, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=nextdescendents`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        errorsHandlers: { "internalServerError": { status: 500 } }
    }).then((response) => response.result);
}

export function getCreatables (realm, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=getCreatableTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        errorsHandlers: { "internalServerError": { status: 500 } }
    }).then((response) => response.result);
}

export function getTemplate (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${encodeURIComponent(id)}/services/${encodeURIComponent(type)}?_action=template`,
            { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=1.0" },
        type: "POST"
    });
}

export function create (realm, id, type, data) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${encodeURIComponent(id)}/services/${encodeURIComponent(type)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=1.0" },
        type: "POST",
        data: JSON.stringify(data)
    });
}
