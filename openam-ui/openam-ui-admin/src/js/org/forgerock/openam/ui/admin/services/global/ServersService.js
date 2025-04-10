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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";

/**
 * @module org/forgerock/openam/ui/admin/services/global/ServersService
 */
const ServersService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const DEFAULT_SERVER = "server-default";
const ADVANCED_SECTION = "advanced";
const isDefaultServer = (serverId) => serverId === "serverDefaults";
const normalizeServerId = (serverId) => {
    return isDefaultServer(serverId) ? DEFAULT_SERVER : serverId;
};

const objectToArray = (valuesObject) => _.map(valuesObject, (value, key) => ({ key, value }));
const arrayToObject = (valuesArray) => _.reduce(valuesArray, (result, item) => {
    result[item.key] = item.value;
    return result;
}, {});

const removeEncryptedValues = (valuesObject) => _.omitBy(valuesObject, (value, key) => {
    return key.endsWith("-encrypted");
});

const getSchema = (server, section) => ServersService.serviceCall({
    url: fetchUrl(`/global-config/servers/${server}/properties/${section}?_action=schema`,
        { realm: false }),
    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
    type: "POST"
}).then((response) => new JSONSchema(response));

const getValues = (server, section) => ServersService.serviceCall({
    url: fetchUrl(`/global-config/servers/${server}/properties/${section}`, { realm: false }),
    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
}).then((response) => {
    if (section === ADVANCED_SECTION) {
        response = _.sortBy(objectToArray(removeEncryptedValues(response)), (value) => value.key);
    }
    return new JSONValues(response);
});

const updateServer = (section, data, id = DEFAULT_SERVER) => {
    let modifiedData = data;
    if (section === ADVANCED_SECTION) {
        modifiedData = removeEncryptedValues(arrayToObject(data[ADVANCED_SECTION]));
    }
    return ServersService.serviceCall({
        url: fetchUrl(`/global-config/servers/${id}/properties/${section}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "PUT",
        data: JSON.stringify(modifiedData)
    });
};

ServersService.servers = {
    clone: (id, clonedUrl) => ServersService.serviceCall({
        url: fetchUrl(`/global-config/servers/${id}?_action=clone`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data: JSON.stringify({ clonedUrl })
    }),
    get: (server, section) => {
        return Promise.all([
            getSchema(server, section),
            getValues(server, section)
        ]).then((response) => ({
            schema: response[0],
            values: response[1]
        }));
    },
    getWithDefaults: (server, section) => {
        const normalizedServerId = normalizeServerId(server);
        const promises = [ServersService.servers.get(normalizedServerId, section)];

        if (!isDefaultServer(server) && section !== "directoryConfiguration") {
            promises.push(getValues(DEFAULT_SERVER, section));
        }
        return Promise.all(promises).then(([instance, defaultValues = {}]) => {
            return {
                schema: instance.schema,
                values: instance.values,
                defaultValues
            };
        });
    },
    getUrl: (id) => ServersService.serviceCall({
        url: fetchUrl(`/global-config/servers/${id}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => response.url, () => undefined),
    getAll: () => ServersService.serviceCall({
        url: fetchUrl("/global-config/servers?_queryFilter=true", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => _.reject(response.result, { "_id" : DEFAULT_SERVER })),
    remove: (id) => ServersService.serviceCall({
        url: fetchUrl(`/global-config/servers/${id}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "DELETE"
    }),
    create:  (data) => ServersService.serviceCall({
        url: fetchUrl("/global-config/servers?_action=create", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data: JSON.stringify(data)
    }),
    update: (section, data, id) => updateServer(section, data, normalizeServerId(id)),
    ADVANCED_SECTION,
    DEFAULT_SERVER
};

export default ServersService;
