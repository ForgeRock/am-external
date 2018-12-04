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
 * Copyright 2018 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService
 */

import { map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/realm-config/secrets/stores";

export function create (realm, type, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        data: JSON.stringify(data)
    });
}

export function get (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
    });
}

export function getAllByType (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}?_queryFilter=true`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
    });
}

export function getCreatableTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_action=getCreatableTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true,
        type: "POST"
    });
}

export function getCreatableTypesByType (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}?_action=getCreatableTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true,
        type: "POST"
    });
}

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true,
        type: "POST"
    });
}

export function getInitialState (realm, type) {
    function getTemplate () {
        return obj.serviceCall({
            url: fetchUrl(`${path}/${type}?_action=template`, { realm }),
            headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
            suppressSpinner: true,
            type: "POST"
        });
    }

    return Promise.all([getSchema(realm, type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}

export function remove (realm, items) {
    const promises = map(items, (item) => obj.serviceCall({
        url: fetchUrl(`${path}/${item._type._id}/${encodeURIComponent(item._id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}

export function update (realm, type, id, data) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}
