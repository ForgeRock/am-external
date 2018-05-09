/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/identities/GroupsService
 */
import { map, omit, startsWith } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import constructFieldParams from "org/forgerock/openam/ui/admin/services/constructFieldParams";
import constructPaginationParams from "org/forgerock/openam/ui/admin/services/constructPaginationParams";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getSchema (realm) {
    return obj.serviceCall({
        url: fetchUrl("/groups?_action=schema", { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function get (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=4.0" }
    });
}

export function getAll (realm, additionalParams = {}) {
    const pagination = constructPaginationParams(additionalParams.pagination);
    const fields = constructFieldParams(additionalParams.fields);
    return obj.serviceCall({
        url: fetchUrl(`/groups?_queryFilter=true${pagination}${fields}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=4.0" }
    });
}

export function remove (realm, ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=3.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}

export function create (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=4.0",
            "If-None-Match": "*"
        },
        data: "{}"
    });
}

export function update (realm, data, id) {
    const omitSchemaConfigViolationProperties = (obj) => omit(obj, ["dn", "objectclass"]);
    const omitReadOnlyProperties = (obj) => omit(obj, (prop, key) => startsWith(key, "_"));

    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=4.0",
            "If-Match": "*"
        },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omitReadOnlyProperties(omitSchemaConfigViolationProperties(data)))
    });
}
