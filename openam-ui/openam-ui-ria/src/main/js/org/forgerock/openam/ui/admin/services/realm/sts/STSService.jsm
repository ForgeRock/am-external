/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/sts/STSService
 */

import { map } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/realm-config/services/sts/";

export function getTemplate (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}?_action=template`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getInitialState (realm, type) {
    return Promise.all([getSchema(realm, type), getTemplate(realm, type)]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}

export function create (realm, type, id, data) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}/${encodeURIComponent(id)}`, { realm }),
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        type: "PUT",
        data: JSON.stringify(data)
    });
}

export function get (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "GET"
    });
}

export function update (realm, type, id, data) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}/${encodeURIComponent(id)}`, { realm }),
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-Match": "*"
        },
        type: "PUT",
        data: JSON.stringify(data)
    });
}

export function getAll (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}${type}?_queryFilter=true`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "GET"
    });
}

export function remove (realm, type, items) {
    const promises = map(items, (item) => obj.serviceCall({
        url: fetchUrl(`${path}${type}/${encodeURIComponent(item._id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}
