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
 * Copyright 2015-2021 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/ScriptsService
 */

import { CRESTv2 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";
import fetchUrl from "api/fetchUrl";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export const validate = (body) => {
    const resource = new CRESTv2(url("/scripts"), {
        middleware: [middleware]
    });
    return spinner(resource.action("validate", { body }));
};

export const getAll = (realm) => {
    const resource = (realm) => new CRESTv2(url("/scripts", realm), {
        middleware: [middleware]
    });
    return resource(realm).queryFilter();
};

export const getAllByScriptType = (realm, scriptType) => {
    return obj.serviceCall({
        url: fetchUrl(`/scripts?_queryFilter=context+eq+%22${encodeURIComponent(scriptType)}%22`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};
