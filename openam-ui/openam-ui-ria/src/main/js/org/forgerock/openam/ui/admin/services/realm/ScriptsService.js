/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
* @module org/forgerock/openam/ui/admin/services/realm/ScriptsService
*/
define([
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (Messages, AbstractDelegate, Constants, fetchUrl) => {
    var obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    function getLocalizedResponse (response) {
        Messages.addMessage({
            type: Messages.TYPE_DANGER,
            response
        });
    }

    obj.validateScript = function (data) {
        return obj.serviceCall({
            url: fetchUrl.default("/scripts/?_action=validate", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(data),
            error: getLocalizedResponse
        });
    };

    return obj;
});
