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
 * @module org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoreMappingsService
 */

import { map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const getPath = (type, typeId) => `/global-config/secrets/stores/${type}/${encodeURIComponent(typeId)}/mappings`;

export function create (type, typeId, data, id) {
    return obj.serviceCall({
        url: `${getPath(type, typeId)}/${encodeURIComponent(id)}`,
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.0,resource=1.0",
            "If-None-Match": "*"
        },
        data: JSON.stringify(data)
    });
}

export function getAllByType (type, typeId) {
    return obj.serviceCall({
        url: `${getPath(type, typeId)}?_queryFilter=true`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
    });
}
export function getSchema (type, typeId) {
    return obj.serviceCall({
        url: `${getPath(type, typeId)}/?_action=schema`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true,
        type: "POST"
    });
}

export function getTemplate (type, typeId) {
    return obj.serviceCall({
        url: `${getPath(type, typeId)}/?_action=template`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true,
        type: "POST"
    });
}

export function update (type, typeId, data, id) {
    return obj.serviceCall({
        url: `${getPath(type, typeId)}/${encodeURIComponent(id)}`,
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function remove (type, typeId, items) {
    return Promise.all(map(items, (item) => obj.serviceCall({
        url: `${getPath(type, typeId)}/${encodeURIComponent(item._id)}`,
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    })));
}
