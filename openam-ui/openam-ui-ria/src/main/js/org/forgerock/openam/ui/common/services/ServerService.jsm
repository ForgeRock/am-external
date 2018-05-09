/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
