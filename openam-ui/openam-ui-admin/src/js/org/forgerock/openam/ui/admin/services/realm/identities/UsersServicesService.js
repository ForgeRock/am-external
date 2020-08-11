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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/identities/UsersServicesService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

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
    });
}

export function getAllInstances (realm, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=nextdescendents`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        errorsHandlers: { "internalServerError": { status: 500 } }
    });
}

export function getCreatables (realm, userId) {
    return obj.serviceCall({
        url: fetchUrl(`/users/${userId}/services?_action=getCreatableTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        errorsHandlers: { "internalServerError": { status: 500 } }
    });
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
