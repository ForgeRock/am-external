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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/authentication/NodeDesignerService
 */
import { map, omitBy, startsWith } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const PATH = "/node-designer/node-type";
const HEADERS = { "Accept-API-Version": "protocol=2.0,resource=1.0" };

function getCreateOrUpdatePayload (data) {
    const omitReadOnlyProperties = (obj) => omitBy(obj, (prop, key) => startsWith(key, "_"));
    data["properties"] = JSON.parse(data["properties"]);
    return {
        ...omitReadOnlyProperties(data)
    };
}

export function getSchema () {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/?_action=schema`, { realm: false }),
        headers: HEADERS,
        type: "POST"
    });
}

export function create (id, name) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}`, { realm: false }),
        type: "POST",
        data: `{ "serviceName": "${id}",
            "version": 1,
            "displayName": "${name}",
            "outcomes": [ "outcome" ],
            "script": "outcome=\\"outcome\\";"
        }`,
        // Prevent the default message for 412 to be shown. Instead print a custom message coming from the HTTP response
        errorsHandlers: { "incorrectRevisionError": { status: 412 } }
    });
}

export function update (data, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}`, { realm: false }),
        type: "PUT",
        headers: { ...HEADERS, "If-Match": "*" },
        data: JSON.stringify(getCreateOrUpdatePayload(data))
    });
}

export function get (id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}?forUI=true`, { realm: false }),
        headers: HEADERS
    });
}

export function getAll () {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}?_queryFilter=true`, { realm: false }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "GET"
    });
}

export function getWithSchema (id) {
    const schemaPromise = getSchema();

    const dataPromise = get(id);

    return Promise.all([schemaPromise, dataPromise]).then(([schema, data]) => ({ schema, data }));
}

export function getInitialState () {
    const schemaPromise = obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=schema`, { realm: false }),
        headers: HEADERS,
        type: "POST"
    });

    const templatePromise = obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=template`, { realm: false }),
        headers: HEADERS,
        type: "POST"
    });

    return Promise.all([schemaPromise, templatePromise]).then(([schema, template]) => ({ schema, template }));
}

export function remove (ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`${PATH}/${id}`, { realm: false }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "DELETE"
    }));

    return Promise.all(promises);
}

/**
 * Exports custom nodes
 * @param {Array} nodeIds the ids of the node to export
 * @returns {Promise} Returns a promise that resolves to an object containing
 * the exported nodes
 */
export function exportNodes (nodeIds) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/?_action=export&selected=${nodeIds.join()}`, { realm: false }),
        headers: HEADERS,
        type: "POST"
    });
}

/**
 * Import custom nodes
 * @param {string} nodesJson JSON string of the uploaded json file
 * @returns {Promise} Returns a promise that resolves to an object containing
 * the imported node
 */
export function importNodes (nodesJson) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/?_action=import`, { realm: false }),
        headers: HEADERS,
        type: "POST",
        data: nodesJson
    });
}
