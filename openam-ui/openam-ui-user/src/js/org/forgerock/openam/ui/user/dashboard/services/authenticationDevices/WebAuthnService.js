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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/WebAuthnService
  */
import { CRESTv2 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const resource = (username) => new CRESTv2(url(`/users/${encodeURIComponent(username)}/devices/2fa/webauthn`), {
    middleware: [middleware]
});

export const update = (username, uuid, body) => spinner(resource(username).update(uuid, body));
export const remove = (username, uuid) => spinner(resource(username).delete(uuid));
export const getAll = (username) => resource(username).queryFilter();
