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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/TreeService
 */
import { CRESTv2_1 } from "@forgerock/crest-js";
import { mapValues, omitBy, startsWith } from "lodash";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (realm) => new CRESTv2_1(url("/realm-config/authentication/authenticationtrees/trees", realm), {
    middleware: [middleware]
});

const removeNodeReadOnlyProperties = (body) => ({
    ...body,
    nodes: mapValues(body.nodes, (node) => omitBy(node, (prop, key) => startsWith(key, "_")))
});

export const create = (realm, body, id) => spinner(resource(realm).create(removeNodeReadOnlyProperties(body), { id }));
export const get = (realm, id) => spinner(resource(realm).get(id, { queryString: { forUI: true } }));
export const getAll = (realm) => resource(realm).action("getIds");
export const getInitialState = (realm) => Promise.all([
    resource(realm).action("schema"),
    resource(realm).action("template")
]).then(([schema, template]) => ({ schema, template }));
export const remove = (realm, ids) => Promise.all(ids.map((id) => resource(realm).delete(id)));
export const update = (realm, body, id) => spinner(resource(realm).update(id, removeNodeReadOnlyProperties(body)));
