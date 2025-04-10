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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService
 */

import { CRESTv2 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (realm) => new CRESTv2(url("/realm-config/federation/circlesoftrust", realm), {
    middleware: [middleware]
});

export const create = (realm, body, id) => spinner(resource(realm).create(body, { id }));
export const get = (realm, id) => spinner(resource(realm).get(id));
export const getAll = (realm) => resource(realm).queryFilter();
export const getSchema = (realm) => resource(realm).action("schema");
export const getTemplate = (realm) => resource(realm).action("template");
export const remove = (realm, ids) => Promise.all(ids.map((id) => resource(realm).delete(id)));
export const update = (realm, body, id) => spinner(resource(realm).update(id, body));
export const getInitialState = (realm) => Promise.all([
    resource(realm).action("schema"),
    resource(realm).action("template")
]).then(([schema, values]) => ({ schema, values }));
export class COTServiceError extends Error {
    constructor (message) {
        super(message);
        this.name = "COTServiceError";
    }
}
