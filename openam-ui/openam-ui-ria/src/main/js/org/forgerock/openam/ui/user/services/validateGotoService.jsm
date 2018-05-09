/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/user/services/validateGotoService
  */
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json/users`);

const validateGotoService = (goto) => {
    return obj.serviceCall({
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
        data: JSON.stringify({ "goto": decodeURIComponent(goto) }),
        url: "?_action=validateGoto",
        errorsHandlers: { "Bad Request": { status: 400 }, "Unauthorized" : { status: 401 } }
    });
};

export default validateGotoService;
