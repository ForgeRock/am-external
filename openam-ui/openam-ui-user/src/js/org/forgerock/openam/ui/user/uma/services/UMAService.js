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
 * Copyright 2015-2019 ForgeRock AS.
 */

import _ from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

const UMAService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

UMAService.getUmaConfig = function () {
    return UMAService.serviceCall({
        url: fetchUrl("/serverinfo/uma"),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((data) => {
        Configuration.globalData.auth.uma = Configuration.globalData.auth.uma || {};
        Configuration.globalData.auth.uma.enabled = data.enabled;
        Configuration.globalData.auth.uma.resharingMode = data.resharingMode;
        return data;
    });
};

UMAService.unshareAllResources = function () {
    const username = encodeURIComponent(Configuration.loggedUser.get("username"));
    return UMAService.serviceCall({
        url: fetchUrl(`/users/${username}/oauth2/resources/sets?_action=revokeAll`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    });
};

UMAService.approveRequest = function (id, permissions) {
    return UMAService.serviceCall({
        url: fetchUrl(`/users/${
            encodeURIComponent(Configuration.loggedUser.get("username"))
        }/uma/pendingrequests/${id}?_action=approve`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data: JSON.stringify({
            scopes: permissions
        })
    });
};

UMAService.denyRequest = function (id) {
    return UMAService.serviceCall({
        url: fetchUrl(`/users/${
            encodeURIComponent(Configuration.loggedUser.get("username"))
        }/uma/pendingrequests/${id}?_action=deny`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    });
};

// FIXME: Mutiple calls to #all end-point throughout this section. Optimize
UMAService.labels = {
    all () {
        return UMAService.serviceCall({
            url: fetchUrl(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/oauth2/resources/labels?_queryFilter=true`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    },
    create (name, type) {
        return UMAService.serviceCall({
            url: fetchUrl(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/oauth2/resources/labels?_action=create`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify({
                name,
                type
            })
        });
    },
    get (id) {
        return UMAService.serviceCall({
            url: fetchUrl(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/oauth2/resources/labels?_queryFilter=true`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((data) => {
            return _.find(data.result, { _id: id });
        });
    },
    remove (id) {
        return UMAService.serviceCall({
            url: fetchUrl(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/oauth2/resources/labels/${encodeURIComponent(id)}`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "DELETE"
        });
    }
};

export default UMAService;
