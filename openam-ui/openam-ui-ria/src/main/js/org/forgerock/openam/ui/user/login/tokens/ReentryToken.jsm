/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * The Reentry Cookie (reentry) used by OpenAM Auth trees to to track an authentication session
 * @module org/forgerock/openam/ui/user/login/tokens/ReentryToken
 */

import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";

const cookieName = "reentry";

export function clear () {
    var date = new Date();
    date.setTime(date.getTime() + (-1 * 24 * 60 * 60 * 1000));
    return CookieHelper.setCookie(cookieName, "", date, "/", false);
}

export function get () {
    return CookieHelper.getCookie(cookieName);
}
