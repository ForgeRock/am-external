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
 * Copyright 2018 ForgeRock AS.
 */

import { omit } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";

/**
 * @module org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresCoreService
 */
const SecretStoresCoreService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function get () {
    const getSchema = () => SecretStoresCoreService.serviceCall({
        url: fetchUrl("/global-config/secrets/GlobalSecrets?_action=schema", { realm: false }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(response));

    const getValues = () => SecretStoresCoreService.serviceCall({
        url: fetchUrl("/global-config/secrets/GlobalSecrets", { realm: false }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" }
    }).then((response) => new JSONValues(response));

    return Promise.all([getSchema(), getValues()]).then(([schema, values]) => ({
        schema,
        values,
        name: values.raw._type.name
    }));
}

export function update (data) {
    return SecretStoresCoreService.serviceCall({
        url: fetchUrl("/global-config/secrets/GlobalSecrets", { realm: false }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "PUT",
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omit(JSON.parse(data), "_rev"))
    });
}
