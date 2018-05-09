/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/NodeService
 */
import { omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const PATH = "/realm-config/authentication/authenticationtrees/nodes";
const HEADERS = { "Accept-API-Version": "protocol=2.0,resource=1.0" };

export function createOrUpdate (realm, data, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        type: "PUT",
        headers: HEADERS,
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function get (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        headers: HEADERS,
        suppressSpinner: true
    });
}

export function getAllTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=getAllTypes`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=schema`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function getTemplate (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=template`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function listOutcomes (realm, data, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=listOutcomes`, { realm }),
        headers: HEADERS,
        type: "POST",
        data: JSON.stringify(data)
    });
}

export function remove (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "DELETE"
    });
}
