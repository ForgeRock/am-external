/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/SitesService
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

    const filterUnEditableProperties = (data) => _.pick(data, ["url", "secondaryURLs"]);

    const getSchema = () => obj.serviceCall({
        url: fetchUrl.default("/global-config/sites?_action=schema", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        success: (data) => {
            const filteredProperties = filterUnEditableProperties(data.properties);
            data.properties = filteredProperties;
            return data;
        }
    });

    const getValues = (id) => obj.serviceCall({
        url: fetchUrl.default(`/global-config/sites/${id}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        success: (data, jqXHR) => {
            data.etag = jqXHR.getResponseHeader("ETag");
            return data;
        }
    });

    const getTemplate = () =>
        obj.serviceCall({
            url: fetchUrl.default("/global-config/sites?_action=template", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });

    obj.sites = {
        getAll: () =>
            obj.serviceCall({
                url: fetchUrl.default("/global-config/sites?_queryFilter=true", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then((data) => _.sortBy(data.result, "_id")),
        get: (id) =>
            Promise.all([getSchema(), getValues(id)]).then((response) => ({
                schema: new JSONSchema(response[0][0]),
                values: new JSONValues(response[1][0])
            })),
        create: (data) =>
            obj.serviceCall({
                url: fetchUrl.default("/global-config/sites?_action=create", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: JSON.stringify(_.omit(data, ["servers"]))
            }),
        update: (id, data, etag) =>
            obj.serviceCall({
                url: fetchUrl.default(`/global-config/sites/${id}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0", "If-Match": etag },
                type: "PUT",
                data: JSON.stringify(filterUnEditableProperties(data))
            }),
        remove: (id, etag) => {
            const remove = (id, etag) => obj.serviceCall({
                url: fetchUrl.default(`/global-config/sites/${id}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0", "If-Match": etag },
                type: "DELETE"
            });

            if (_.isUndefined(etag)) {
                return getValues(id).then((response) => remove(id, response.etag));
            } else {
                return remove(id, etag);
            }
        },
        getInitialState: () =>
            Promise.all([getSchema(), getTemplate()]).then((response) => ({
                schema: new JSONSchema(response[0][0]),
                values: new JSONValues(response[1][0])
            }))
    };

    return obj;
});
