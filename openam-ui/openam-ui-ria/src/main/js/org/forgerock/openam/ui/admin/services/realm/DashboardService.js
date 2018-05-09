/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/DashboardService
 */
define([
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (AbstractDelegate, Constants, fetchUrl) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    obj.dashboard = {
        commonTasks: {
            all: (realm) => obj.serviceCall({
                url: fetchUrl.default("/realm-config/commontasks?_queryFilter=true", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            })
        }
    };

    return obj;
});
