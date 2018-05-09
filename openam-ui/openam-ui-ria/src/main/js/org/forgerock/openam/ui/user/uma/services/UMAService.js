/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (_, AbstractDelegate, Configuration, Constants, fetchUrl) => {
    var obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    obj.getUmaConfig = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/serverinfo/uma"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((data) => {
            Configuration.globalData.auth.uma = Configuration.globalData.auth.uma || {};
            Configuration.globalData.auth.uma.enabled = data.enabled;
            Configuration.globalData.auth.uma.resharingMode = data.resharingMode;
            return data;
        });
    };

    obj.unshareAllResources = function () {
        const username = encodeURIComponent(Configuration.loggedUser.get("username"));
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${username}/oauth2/resources/sets?_action=revokeAll`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    };

    obj.approveRequest = function (id, permissions) {
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/uma/pendingrequests/${id}?_action=approve`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify({
                scopes: permissions
            })
        });
    };

    obj.denyRequest = function (id) {
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))
            }/uma/pendingrequests/${id}?_action=deny`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    };

    // FIXME: Mutiple calls to #all end-point throughout this section. Optimize
    obj.labels = {
        all () {
            return obj.serviceCall({
                url: fetchUrl.default(`/users/${
                    encodeURIComponent(Configuration.loggedUser.get("username"))
                }/oauth2/resources/labels?_queryFilter=true`),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },
        create (name, type) {
            return obj.serviceCall({
                url: fetchUrl.default(`/users/${
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
            return obj.serviceCall({
                url: fetchUrl.default(`/users/${
                    encodeURIComponent(Configuration.loggedUser.get("username"))
                }/oauth2/resources/labels?_queryFilter=true`),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then((data) => {
                return _.find(data.result, { _id: id });
            });
        },
        remove (id) {
            return obj.serviceCall({
                url: fetchUrl.default(`/users/${
                    encodeURIComponent(Configuration.loggedUser.get("username"))
                }/oauth2/resources/labels/${encodeURIComponent(id)}`),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE"
            });
        }
    };

    return obj;
});
