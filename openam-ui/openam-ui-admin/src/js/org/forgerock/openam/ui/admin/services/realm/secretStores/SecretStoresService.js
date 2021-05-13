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
 * @module org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService
 */

import { CRESTv2 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (realm, type) => {
    const basePath = "/realm-config/secrets/stores";
    const path = type ? `${basePath}/${type}` : basePath;
    return new CRESTv2(url(path, realm), { middleware: [middleware] });
};

export const create = (realm, type, body, id) => spinner(resource(realm, type).create(body, { id }));
export const get = (realm, type, id) => resource(realm, type).get(id);
export const getAllByType = (realm, type) => resource(realm, type).queryFilter();
export const getCreatableTypes = (realm) => resource(realm).action("getCreatableTypes");
export const getCreatableTypesByType = (realm, type) => resource(realm, type).action("getCreatableTypes");
export const getInitialState = (realm, type) => Promise.all([
    resource(realm, type).action("schema"),
    resource(realm, type).action("template")
]).then(([schema, values]) => ({ schema, values }));
export const getSchema = (realm, type) => resource(realm, type).action("schema");
export const getTemplate = (realm, type) => resource(realm, type).action("template");
export const remove = (realm, items) => spinner(
    Promise.all(items.map((item) => resource(realm, item._type._id).delete(item._id)))
);
export const update = (realm, type, id, body) => spinner(resource(realm, type).update(id, body));
