/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { clear as clearToken, get as getToken } from "./ReentryToken";
/**
 * The Authentication Token (authId) used by OpenAM to track an authentication session, usually
 * this is an unauthenticated users progress through an authentication chain or tree.
 * @module org/forgerock/openam/ui/user/login/tokens/AuthenticationToken
 */

export function set (token) {
    sessionStorage.setItem(getToken(), token);
}

export function get () {
    return sessionStorage.getItem(getToken());
}

export function remove () {
    sessionStorage.removeItem(getToken());
    clearToken();
}
