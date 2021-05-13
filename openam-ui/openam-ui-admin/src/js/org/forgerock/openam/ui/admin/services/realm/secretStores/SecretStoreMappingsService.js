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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoreMappingsService
 */

import { CRESTv2 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (realm, type, typeId) => new CRESTv2(
    url(`/realm-config/secrets/stores/${type}/${encodeURIComponent(typeId)}/mappings`, realm), {
        middleware: [middleware]
    }
);

export const create = (realm, type, typeId, body, id) => spinner(resource(realm, type, typeId).create(body, { id }));
export const getAllByType = (realm, type, typeId) => resource(realm, type, typeId).queryFilter();
export const getSchema = (realm, type, typeId) => resource(realm, type, typeId).action("schema");
export const getTemplate = (realm, type, typeId) => resource(realm, type, typeId).action("template");
export const remove = (realm, type, typeId, items) => spinner(
    Promise.all(items.map((item) => resource(realm, type, typeId).delete(item._id)))
);
export const update = (realm, type, typeId, body, id) => spinner(resource(realm, type, typeId).update(id, body));
