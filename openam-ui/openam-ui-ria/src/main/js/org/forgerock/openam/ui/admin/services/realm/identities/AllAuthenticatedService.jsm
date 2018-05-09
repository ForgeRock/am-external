/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/identities/AllAuthenticatedService
 */
import { omit, startsWith } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getSchema (realm) {
    return obj.serviceCall({
        url: fetchUrl("/groups/allauthenticatedusers?_action=schema", { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function get (realm) {
    return obj.serviceCall({
        url: fetchUrl("/groups/allauthenticatedusers", { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=1.0" }
    });
}

export function update (realm, data) {
    const omitReadOnlyProperties = (obj) => omit(obj, (prop, key) => startsWith(key, "_"));
    return obj.serviceCall({
        url: fetchUrl("/groups/allauthenticatedusers", { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=1.0",
            "If-Match": "*"
        },
        data: JSON.stringify(omitReadOnlyProperties(data))
    });
}
