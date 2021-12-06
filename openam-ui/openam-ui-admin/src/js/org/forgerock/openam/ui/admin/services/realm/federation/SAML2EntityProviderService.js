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
 * Copyright 2019-2021 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService
 */

import { CRESTv2 } from "@forgerock/crest-js";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Base64 from "org/forgerock/commons/ui/common/util/Base64";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";
import constructPaginationParams from "org/forgerock/openam/ui/admin/services/constructPaginationParams";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const path = "/realm-config/saml2";

const resource = (realm, location) => new CRESTv2(url(`${path}/${location}`, realm), {
    middleware: [middleware]
});

export const get = (realm, location, id) => resource(realm, location).get(Base64.encodeBase64Url(id));

export const update = (realm, location, id, body) =>
    spinner(resource(realm, location).update(Base64.encodeBase64Url(id), body));

export const remove = (realm, location, id) => resource(realm, location).delete(Base64.encodeBase64Url(id));

export const create = (realm, location, body) => {
    switch (location) {
        case "hosted":
            return spinner(resource(realm, location).create(body));
        case "remote":
            return spinner(resource(realm, location).action("importEntity", { body }));
    }
};

export const getSchema = (realm, location) => {
    return resource(realm, location).action("schema");
};

export const searchEntities = (realm, additionalParams = {}) => {
    const term = additionalParams.pagination.searchTerm;
    const pagination = constructPaginationParams(additionalParams.pagination);
    const queryFilter = term ? encodeURIComponent(`entityId co "${term}"`) : "true";
    return obj.serviceCall({
        url: fetchUrl(`${path}?_queryFilter=${queryFilter}${pagination}`, { realm })
    });
};
