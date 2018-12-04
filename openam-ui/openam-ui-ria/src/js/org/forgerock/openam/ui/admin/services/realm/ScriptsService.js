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
 * Copyright 2015-2018 ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

/**
 * @module org/forgerock/openam/ui/admin/services/realm/ScriptsService
 */

const ScriptsService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

function getLocalizedResponse (response) {
    Messages.addMessage({
        type: Messages.TYPE_DANGER,
        response
    });
}

ScriptsService.validateScript = function (data) {
    return ScriptsService.serviceCall({
        url: fetchUrl("/scripts/?_action=validate", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data: JSON.stringify(data),
        error: getLocalizedResponse
    });
};

export default ScriptsService;
