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
 * @module org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresService
 */

import { filter, map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/global-config/secrets/stores";

export function get (type, id) {
    return obj.serviceCall({
        url: `${path}/${type}/${encodeURIComponent(id)}`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}

export function getSingleton (type) {
    return obj.serviceCall({
        url: `${path}/${type}`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}

export function create (type, data, id) {
    return obj.serviceCall({
        url: `${path}/${type}/${encodeURIComponent(id)}`,
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        data: JSON.stringify(data)
    });
}

export function getAll (type) {
    return obj.serviceCall({
        url: `${path}/${type}?_queryFilter=true`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}

export function getCreatableTypesByType (type) {
    return obj.serviceCall({
        url: `${path}/${type}?_action=getCreatableTypes`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getSingletonTypes () {
    return obj.serviceCall({
        url: `${path}?_action=getAllTypes`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    }).then(({ result }) => {
        return filter(result, (type) => type.collection === false);
    });
}

export function getCreatableTypes () {
    return obj.serviceCall({
        url: `${path}?_action=getCreatableTypes`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getSchema (type) {
    return obj.serviceCall({
        url: `${path}/${type}?_action=schema`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getInitialState (type) {
    function getTemplate () {
        return obj.serviceCall({
            url: `${path}/${type}?_action=template`,
            headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
            type: "POST"
        });
    }

    return Promise.all([getSchema(type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}

export function remove (items) {
    const promises = map(items, (item) => obj.serviceCall({
        url: `${path}/${item._type._id}/${encodeURIComponent(item._id)}`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE",
        suppressSpinner: true
    }));

    return Promise.all(promises);
}

export function update (type, id, data) {
    return obj.serviceCall({
        url: `${path}/${type}/${encodeURIComponent(id)}`,
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function updateSingleton (type, data) {
    return obj.serviceCall({
        url: `${path}/${type}`,
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}
