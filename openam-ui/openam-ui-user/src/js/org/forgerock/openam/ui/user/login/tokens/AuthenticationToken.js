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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { clear as clearToken, get as getToken } from "./ReentryToken";
/**
 * The Authentication Token (authId) used by OpenAM to track an authentication session, usually
 * this is an unauthenticated users progress through an authentication chain or tree.
 * @module org/forgerock/openam/ui/user/login/tokens/AuthenticationToken
 */

export function set (token) {
    try {
        sessionStorage.setItem(getToken(), token);
    } catch (e) {
        // Failed to retrieve item from session storage.
    }
}

export function get () {
    try {
        return sessionStorage.getItem(getToken());
    } catch (e) {
        // Failed to retrieve item from session storage.
    }
}

export function remove () {
    try {
        sessionStorage.removeItem(getToken());
    } catch (e) {
        // Failed to remove item from session storage.
    }
    clearToken();
}
