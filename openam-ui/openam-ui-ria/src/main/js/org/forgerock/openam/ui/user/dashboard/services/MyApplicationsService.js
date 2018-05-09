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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (_, AbstractDelegate, Constants, fetchUrl) => {
    var obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    function sortApps (apps) {
        return _.map(_.sortBy(_.keys(apps), (key) => { return key; }), (key) => {
            var app = {
                id: key
            };
            _.each(apps[key], (v, k) => { app[k] = v[0]; });
            return app;
        });
    }

    obj.getMyApplications = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/dashboard/assigned"),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(sortApps);
    };

    obj.getAvailableApplications = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/dashboard/available"),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(sortApps);
    };

    return obj;
});
