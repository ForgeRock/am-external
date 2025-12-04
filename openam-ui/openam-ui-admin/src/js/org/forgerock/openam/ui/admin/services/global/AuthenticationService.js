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
 * @module org/forgerock/openam/ui/admin/services/global/AuthenticationService
 */
const AuthenticationService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

function getModuleUrl (id) {
    return id === "core" ? "" : `/modules/${id}`;
}

AuthenticationService.authentication = {
    getAll () {
        return AuthenticationService.serviceCall({
            url: fetchUrl("/global-config/authentication/modules?_action=getAllTypes", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((data) => _.sortBy(data.result, "name"));
    },
    schema () {
        const serviceCall = (action) => AuthenticationService.serviceCall({
            url: fetchUrl(`/global-config/authentication?_action=${action}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });

        return Promise.all([serviceCall("schema"), serviceCall("template")]).then((response) => ({
            schema: response[0],
            values: response[1]
        }));
    },
    get: (id) => {
        const moduleUrl = getModuleUrl(id);

        const getSchema = () => AuthenticationService.serviceCall({
            url: fetchUrl(`/global-config/authentication${moduleUrl}?_action=schema`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((response) => new JSONSchema(response));

        const getValues = () => AuthenticationService.serviceCall({
            url: fetchUrl(`/global-config/authentication${moduleUrl}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => new JSONValues(response));

        return Promise.all([getSchema(), getValues()]).then((response) => ({
            schema: response[0],
            values: response[1],
            name: response[1].raw._type.name
        }));
    },
    update (id, data) {
        return AuthenticationService.serviceCall({
            url: fetchUrl(`/global-config/authentication${getModuleUrl(id)}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data
        });
    }
};
export default AuthenticationService;
