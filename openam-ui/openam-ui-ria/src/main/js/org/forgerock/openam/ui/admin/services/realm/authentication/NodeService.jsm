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
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/NodeService
 */
import { omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const PATH = "/realm-config/authentication/authenticationtrees/nodes";
const HEADERS = { "Accept-API-Version": "protocol=2.0,resource=1.0" };

export function createOrUpdate (realm, data, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        type: "PUT",
        headers: HEADERS,
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function get (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        headers: HEADERS,
        suppressSpinner: true
    });
}

export function getAllTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}?_action=getAllTypes`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=schema`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function getTemplate (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=template`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "POST"
    });
}

export function listOutcomes (realm, data, type) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}?_action=listOutcomes`, { realm }),
        headers: HEADERS,
        type: "POST",
        data: JSON.stringify(data)
    });
}

export function remove (realm, type, id) {
    return obj.serviceCall({
        url: fetchUrl(`${PATH}/${type}/${id}`, { realm }),
        headers: HEADERS,
        suppressSpinner: true,
        type: "DELETE"
    });
}
