/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/services/global/ServersService
  */
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Promise"
], (_, AbstractDelegate, Constants, JSONSchema, JSONValues, fetchUrl, Promise) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
    const DEFAULT_SERVER = "server-default";
    const ADVANCED_SECTION = "advanced";
    const isDefaultServer = (serverId) => serverId === "server-defaults";
    const normalizeServerId = (serverId) => {
        return isDefaultServer(serverId) ? DEFAULT_SERVER : serverId;
    };

    const objectToArray = (valuesObject) => _.map(valuesObject, (value, key) => ({ key, value }));
    const arrayToObject = (valuesArray) => _.reduce(valuesArray, (result, item) => {
        result[item.key] = item.value;
        return result;
    }, {});

    const getSchema = (server, section) => obj.serviceCall({
        url: fetchUrl.default(`/global-config/servers/${server}/properties/${section}?_action=schema`,
            { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(response));

    const getValues = (server, section) => obj.serviceCall({
        url: fetchUrl.default(`/global-config/servers/${server}/properties/${section}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => {
        if (section === ADVANCED_SECTION) {
            response = _.sortBy(objectToArray(response), (value) => value.key);
        }
        return new JSONValues(response);
    });

    const updateServer = (section, data, id = DEFAULT_SERVER) => {
        let modifiedData = data;
        if (section === ADVANCED_SECTION) {
            modifiedData = arrayToObject(data[ADVANCED_SECTION]);
        }
        return obj.serviceCall({
            url: fetchUrl.default(`/global-config/servers/${id}/properties/${section}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(modifiedData)
        });
    };

    obj.servers = {
        clone: (id, clonedUrl) => obj.serviceCall({
            url: fetchUrl.default(`/global-config/servers/${id}?_action=clone`, { realm: false }),
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
            const promises = [obj.servers.get(normalizedServerId, section)];

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
        getUrl: (id) => obj.serviceCall({
            url: fetchUrl.default(`/global-config/servers/${id}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => response.url, () => undefined),
        getAll: () => obj.serviceCall({
            url: fetchUrl.default("/global-config/servers?_queryFilter=true", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => _.reject(response.result, { "_id" : DEFAULT_SERVER })),
        remove: (id) => obj.serviceCall({
            url: fetchUrl.default(`/global-config/servers/${id}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "DELETE"
        }),
        create:  (data) => obj.serviceCall({
            url: fetchUrl.default("/global-config/servers?_action=create", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(data)
        }),
        update: (section, data, id) => updateServer(section, data, normalizeServerId(id)),
        ADVANCED_SECTION,
        DEFAULT_SERVER
    };

    return obj;
});
