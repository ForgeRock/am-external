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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import _ from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";

/**
 * @module org/forgerock/openam/ui/admin/services/global/SitesService
 */
const SitesService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

const filterUnEditableProperties = (data) => _.pick(data, ["url", "secondaryURLs"]);

const getSchema = () => SitesService.serviceCall({
    url: fetchUrl("/global-config/sites?_action=schema", { realm: false }),
    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
    type: "POST",
    success: (data) => {
        const filteredProperties = filterUnEditableProperties(data.properties);
        data.properties = filteredProperties;
        return data;
    }
});

const getValues = (id) => SitesService.serviceCall({
    url: fetchUrl(`/global-config/sites/${id}`, { realm: false }),
    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
    success: (data, jqXHR) => {
        data.etag = jqXHR.getResponseHeader("ETag");
        return data;
    }
});

const getTemplate = () =>
    SitesService.serviceCall({
        url: fetchUrl("/global-config/sites?_action=template", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    });

SitesService.sites = {
    getAll: () =>
        SitesService.serviceCall({
            url: fetchUrl("/global-config/sites?_queryFilter=true", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((data) => _.sortBy(data.result, "_id")),
    get: (id) =>
        Promise.all([getSchema(), getValues(id)]).then((response) => ({
            schema: new JSONSchema(response[0]),
            values: new JSONValues(response[1])
        })),
    create: (data) =>
        SitesService.serviceCall({
            url: fetchUrl("/global-config/sites?_action=create", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(_.omit(data, ["servers"]))
        }),
    update: (id, data, etag) =>
        SitesService.serviceCall({
            url: fetchUrl(`/global-config/sites/${id}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0", "If-Match": etag },
            type: "PUT",
            data: JSON.stringify(filterUnEditableProperties(data))
        }),
    remove: (id, etag) => {
        const remove = (id, etag) => SitesService.serviceCall({
            url: fetchUrl(`/global-config/sites/${id}`, { realm: false }),
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
            schema: new JSONSchema(response[0]),
            values: new JSONValues(response[1])
        }))
};

export default SitesService;
