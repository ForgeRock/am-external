/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (AbstractDelegate, Configuration, Constants, fetchUrl) => {
    var obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    obj.getApplications = function () {
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${
                encodeURIComponent(Configuration.loggedUser.get("username"))}/oauth2/applications?_queryFilter=true`),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.revokeApplication = function (id) {
        return obj.serviceCall({
            url: fetchUrl.default(
                `/users/${encodeURIComponent(Configuration.loggedUser.get("username"))}/oauth2/applications/${id}`),
            type: "DELETE",
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    return obj;
});
