/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/AgentsService
 */
import { map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import constructFieldParams from "org/forgerock/openam/ui/admin/services/constructFieldParams";
import constructPaginationParams from "org/forgerock/openam/ui/admin/services/constructPaginationParams";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getCreatableTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl("/realm-config/agents?_action=getCreatableTypes", { realm }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((data) => data.result);
}

export function create (realm, type, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        data: JSON.stringify(data),
        suppressSpinner: true,
        errorsHandlers: { "incorrectRevisionError": { status: 412 } }
    });
}

export function update (realm, type, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-Match": "*"
        },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function get (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
    });
}

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getAll (realm, type, additionalParams = {}) {
    const pagination = constructPaginationParams(additionalParams.pagination);
    const fields = constructFieldParams(additionalParams.fields);
    return obj.serviceCall({
        url: fetchUrl(
            `/realm-config/agents/${type}?_queryFilter=true${pagination}${fields}`, { realm }
        ),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}

export function getInitialState (realm, type) {
    function getTemplate () {
        return obj.serviceCall({
            url: fetchUrl(`/realm-config/agents/${type}?_action=template`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    }

    return Promise.all([getSchema(realm, type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}

export function remove (realm, type, ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}
