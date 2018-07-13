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
 * Copyright 2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/TreeService
 */
import { map, mapValues, omit, startsWith } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const PATH = "/realm-config/authentication/authenticationtrees/trees";
const HEADERS = { "Accept-API-Version": "protocol=2.0,resource=1.0" };

function getCreateOrUpdatePayload (data) {
    const omitReadOnlyProperties = (obj) => omit(obj, (prop, key) => startsWith(key, "_"));
    return {
        ...omitReadOnlyProperties(data),
        nodes: mapValues(data.nodes, (node) => omitReadOnlyProperties(node))
    };
}

export function create (realm, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}`, { realm }),
        type: "PUT",
        headers: { ...HEADERS, "If-None-Match": "*" },
        data: JSON.stringify(getCreateOrUpdatePayload(data)),
        // Prevent the default message for 412 to be shown. Instead print a custom message coming from the HTTP response
        errorsHandlers: { "incorrectRevisionError": { status: 412 } }
    });
}

export function update (realm, data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}`, { realm }),
        type: "PUT",
        headers: HEADERS,
        data: JSON.stringify(getCreateOrUpdatePayload(data))
    });
}

export function get (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}?forUI=true`, { realm }),
        headers: HEADERS
    });
}

export function getAll (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=getIds`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function getInitialState (realm) {
    const schemaPromise = obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=schema`, { realm }),
        headers: HEADERS,
        type: "POST"
    });

    const templatePromise = obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=template`, { realm }),
        headers: HEADERS,
        type: "POST"
    });

    return Promise.all([schemaPromise, templatePromise]).then(([schema, template]) => ({ schema, template }));
}

export function remove (realm, ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}`, { realm }),
        headers: HEADERS,
        type: "DELETE"
    }));

    return Promise.all(promises);
}
