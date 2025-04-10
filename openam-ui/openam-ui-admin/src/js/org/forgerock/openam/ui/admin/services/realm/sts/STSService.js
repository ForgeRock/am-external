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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/sts/STSService
 */

import { map } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

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
