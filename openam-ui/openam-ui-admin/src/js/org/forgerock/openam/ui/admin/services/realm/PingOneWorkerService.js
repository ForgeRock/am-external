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
 * Copyright 2026 Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/PingOneWorkerService
 */

import fetchUrl from "api/fetchUrl";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const service = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export const testConnection = (realm, workerId) => {
    const url = `/realm-config/services/pingOneWorkerService/workers/${encodeURIComponent(workerId)}` +
        "?_action=testConnection";

    return service.serviceCall({
        type: "POST",
        url: fetchUrl(url, { realm }),
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=1.0",
            "Content-Type": "application/json"
        },
        data: JSON.stringify({})
    });
};

export default {
    testConnection
};
