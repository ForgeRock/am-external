/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService
 */

import { map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/realm-config/federation/circlesoftrust";

export function getAll (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_queryFilter=true`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}

export function remove (realm, ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`${path}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}

export function update (realm, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function get (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
    });
}

export function getSchema (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function create (realm, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        data: JSON.stringify(data)
    });
}

export function getInitialState (realm) {
    function getTemplate () {
        return obj.serviceCall({
            url: fetchUrl(`${path}?_action=template`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    }

    return Promise.all([getSchema(realm), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}
