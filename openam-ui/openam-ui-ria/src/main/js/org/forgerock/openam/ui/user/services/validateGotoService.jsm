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
 * Copyright 2017 ForgeRock AS.
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
