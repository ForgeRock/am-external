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
 * Copyright 2015-2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/services/ServerService
 */
import _ from "lodash";
import { addRealm } from "store/modules/remote/info";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "./fetchUrl";
import store from "store/index";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

function getRealmUrlParameter () {
    return URIUtils.parseQueryString(URIUtils.getCurrentQueryString()).realm;
}

function getUrl (path) {
    const realmParameter = getRealmUrlParameter();
    return fetchUrl(path, { realm: realmParameter ? realmParameter : false });
}

export function getVersion () {
    return obj.serviceCall({
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        url: getUrl("/serverinfo/version")
    }).then(({ fullVersion }) => fullVersion);
}

export function getConfiguration (callParams) {
    return obj.serviceCall(_.extend({
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.1" },
        url: getUrl("/serverinfo/*")
    }, callParams)).then((response) => {
        store.dispatch(addRealm(response.realm));

        return response;
    }, (reason) => {
        const realmUrlParameter = getRealmUrlParameter();
        if (realmUrlParameter) {
            store.dispatch(addRealm(realmUrlParameter));
        }

        return reason;
    });
}
