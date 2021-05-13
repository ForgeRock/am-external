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
 * The Reentry Cookie (reentry) used by OpenAM Auth trees to to track an authentication session
 * @module org/forgerock/openam/ui/user/login/tokens/ReentryToken
 */

import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";

const cookieName = "reentry";

export function clear () {
    const date = new Date();
    date.setTime(date.getTime() + (-1 * 24 * 60 * 60 * 1000));
    return CookieHelper.setCookie(cookieName, "", date, "/", false);
}

export function get () {
    return CookieHelper.getCookie(cookieName);
}
