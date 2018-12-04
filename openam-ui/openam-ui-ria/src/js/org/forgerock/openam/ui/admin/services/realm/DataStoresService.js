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
 * Copyright 2017-2018 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/DataStoresService
 */

import { map, omit, find } from "lodash";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/realm-config/services/id-repositories";

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

export function getSchema (realm, type) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${type}?_action=schema`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getInitialState (realm, type) {
    function getTemplate () {
        return obj.serviceCall({
            url: fetchUrl(`${path}/${type}?_action=template`, { realm }),
            headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
            type: "POST"
        });
    }

    return Promise.all([getSchema(realm, type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}

export function getCreatableTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_action=getCreatableTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getAll (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_action=nextdescendents`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function getTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl(`${path}?_action=getAllTypes`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function remove (realm, items) {
    const promises = map(items, (item) => obj.serviceCall({
        url: fetchUrl(`${path}/${item._type._id}/${encodeURIComponent(item._id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}

export function get (realm, type, id) {
    const getInstance = () => {
        return obj.serviceCall({
            url: fetchUrl(`${path}/${encodeURIComponent(type)}/${encodeURIComponent(id)}`, { realm }),
            headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
        });
    };

    return Promise.all([getSchema(realm, type), getInstance()]).then((response) => {
        return response[1];
    });
}

export function update (realm, data) {
    return obj.serviceCall({
        url: fetchUrl(`${path}/${encodeURIComponent(data._type._id)}/${encodeURIComponent(data._id)}`, { realm }),
        type: "PUT",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(data, "_rev"))
    });
}

export function getTypeDisplayName (realm, type) {
    return getTypes(realm).then((types) => {
        const typeObject = find(types.result, (typeEntry) => typeEntry._id === type);
        const name = typeObject ? typeObject.name : null;

        return name;
    });
}

export function loadSchema (realm, data) {
    const itemUrl = `${path}/${encodeURIComponent(data._type._id)}/${encodeURIComponent(data._id)}`;

    return obj.serviceCall({
        url: fetchUrl(`${itemUrl}?_action=loadSchema`, { realm }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    });
}
