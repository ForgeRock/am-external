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
 * Copyright 2016-2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/AgentsService
 */
import { map, omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
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
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
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

export function getAll (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`/realm-config/agents/${type}?_queryFilter=true`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        suppressSpinner: true
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
