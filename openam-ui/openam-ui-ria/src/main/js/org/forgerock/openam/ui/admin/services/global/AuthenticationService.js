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
 * @module org/forgerock/openam/ui/admin/services/global/AuthenticationService
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

    function getModuleUrl (id) {
        return id === "core" ? "" : `/modules/${id}`;
    }

    obj.authentication = {
        getAll () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/authentication/modules?_action=getAllTypes", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((data) => _.sortBy(data.result, "name"));
        },
        schema () {
            const serviceCall = (action) => obj.serviceCall({
                url: fetchUrl.default(`/global-config/authentication?_action=${action}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });

            return Promise.all([serviceCall("schema"), serviceCall("template")]).then((response) => ({
                schema: response[0][0],
                values: response[1][0]
            }));
        },
        get: (id) => {
            const moduleUrl = getModuleUrl(id);

            const getSchema = () => obj.serviceCall({
                url: fetchUrl.default(`/global-config/authentication${moduleUrl}?_action=schema`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((response) => new JSONSchema(response));

            const getValues = () => obj.serviceCall({
                url: fetchUrl.default(`/global-config/authentication${moduleUrl}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then((response) => new JSONValues(response));

            return Promise.all([getSchema(), getValues()]).then((response) => ({
                schema: response[0],
                values: response[1],
                name: response[1].raw._type.name
            }));
        },
        update (id, data) {
            return obj.serviceCall({
                url: fetchUrl.default(`/global-config/authentication${getModuleUrl(id)}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data
            });
        }
    };
    return obj;
});
