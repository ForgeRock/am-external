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
 * Copyright 2018 ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

/**
 * @module org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/WebAuthnService
 */

const delegate = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const getPath = () => {
    return `/users/${Configuration.loggedUser.get("username")}/devices/2fa/webauthn`;
};

/**
 * getAll will either return an array of devices, or it will return a 403 if the user has not signed in securely
 * and does not have access to this endpoint.
 * Because we are using $.Deferred in place of Promise, the expected 403 causes the Promise.all function to fail early
 * and run it's finally block before all the Deferreds have completed. Adding the `async` here ensures the returned item
 * is a real Promise, which the promise.all.finally can handle properly.
 * @returns {Promise} promise that will contain the response
 */
export async function getAll () {
    return delegate.serviceCall({
        url: fetchUrl(`${getPath()}?_queryFilter=true`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        errorsHandlers: { "forbidden": { status: 403 } }
    });
}

export function update (uuid, data) {
    return delegate.serviceCall({
        url: fetchUrl(`${getPath()}/${uuid}`),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=1.0",
            "If-Match": "*"
        },
        data: JSON.stringify(data)
    });
}

export function remove (uuid) {
    return delegate.serviceCall({
        url: fetchUrl(`${getPath()}/${uuid}`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        suppressEvents: true,
        method: "DELETE"
    });
}
