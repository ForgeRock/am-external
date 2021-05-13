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
 * Copyright 2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/amadmin/AmadminService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

import fetchUrl from "api/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

// TODO : Once OPENAM-14258 completed this needs to change to use the new rest call.
export function updatePassword ({ olduserpassword, userpassword }) {
    return obj.serviceCall({
        url: fetchUrl("/users/amadmin"),
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=1.2",
            olduserpassword
        },
        type: "PUT",
        data: JSON.stringify({ userpassword }),
        errorsHandlers: { "badRequest": { status: "400" } }
    });
}
