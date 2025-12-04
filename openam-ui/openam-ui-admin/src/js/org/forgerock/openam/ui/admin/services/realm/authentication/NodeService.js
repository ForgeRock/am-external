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
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/NodeService
 */
import { CRESTv2_1 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (realm, type = "") =>
    new CRESTv2_1(url(`/realm-config/authentication/authenticationtrees/nodes/${type}`, realm), {
        middleware: [middleware]
    });

export const createOrUpdate = (realm, body, type, id) => spinner(resource(realm, type).update(id, body));
export const get = (realm, type, id) => resource(realm, type).get(id);
export const getAllTypes = (realm) => resource(realm).action("getAllTypes");
export const getSchema = (realm, type) => resource(realm, type).action("schema");
export const getTemplate = (realm, type) => resource(realm, type).action("template");
export const listOutcomes = (realm, body, type) => resource(realm, type).action("listOutcomes", { body });
export const remove = (realm, type, id) => resource(realm, type).delete(id);
