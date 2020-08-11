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
 * Copyright 2011-2019 ForgeRock AS.
 */

import { each, isArray } from "lodash";

/**
 * @exports org/forgerock/commons/ui/common/util/CookieHelper
 */
const CookieHelper = {};

/**
 * Creates a cookie with given parameters.
 * @param {string} name - cookie name.
 * @param {string} [value] - cookie value.
 * @param {Date} [expirationDate] - cookie expiration date.
 * @param {string} [path] - cookie path.
 * @param {string|Array<string>} [domain] - cookie domain(s).
 * @param {boolean} [secure] - is cookie secure.
 * @returns {string} created cookie.
 */
CookieHelper.createCookie = function (name, value, expirationDate, path, domain, secure) {
    const expirationDatePart = expirationDate ? `;expires=${expirationDate.toGMTString()}` : "";
    const nameValuePart = `${name}=${value}`;
    const pathPart = path ? `;path=${path}` : "";
    const domainPart = domain ? `;domain=${domain}` : "";
    const securePart = secure ? ";secure" : "";

    return nameValuePart + expirationDatePart + pathPart + domainPart + securePart;
};

/**
 * Sets a cookie with given parameters in the browser.
 * @param {string} name - cookie name.
 * @param {string} [value] - cookie value.
 * @param {Date} [expirationDate] - cookie expiration date.
 * @param {string} [path] - cookie path.
 * @param {string|Array<string>} [domains] - cookie domain(s). Use empty array for creating host-only cookies.
 * @param {boolean} [secure] - is cookie secure.
 */
CookieHelper.setCookie = function (name, value, expirationDate, path, domains, secure) {
    if (!isArray(domains)) {
        domains = [domains];
    }

    if (domains.length === 0) {
        document.cookie = CookieHelper.createCookie(name, value, expirationDate, path, undefined, secure);
    } else {
        each(domains, (domain) => {
            document.cookie = CookieHelper.createCookie(name, value, expirationDate, path, domain, secure);
        });
    }
};

/**
 * Returns cookie with a given name.
 * @param {string} name - cookie name.
 * @returns {string} cookie or undefined if cookie was not found
 */
CookieHelper.getCookie = function (name) {
    let i; let x; let y; const cookies = document.cookie.split(";");
    for (i = 0; i < cookies.length; i++) {
        x = cookies[i].substr(0, cookies[i].indexOf("="));
        y = cookies[i].substr(cookies[i].indexOf("=") + 1);
        x = x.replace(/^\s+|\s+$/g, "");
        if (x === name) {
            return unescape(y);
        }
    }
};

/**
 * Deletes cookie with given parameters.
 * @param {string} name - cookie name.
 * @param {string} [path] - cookie path.
 * @param {string|Array<string>} [domains] - cookie domain(s).
 */
CookieHelper.deleteCookie = function (name, path, domains) {
    const date = new Date();
    date.setTime(date.getTime() + (-1 * 24 * 60 * 60 * 1000));
    CookieHelper.setCookie(name, "", date, path, domains);
};

/**
 * Checks if cookies are enabled.
 * @returns {boolean} whether cookies enabled or not.
 */
CookieHelper.cookiesEnabled = function () {
    this.setCookie("cookieTest", "test");
    if (!this.getCookie("cookieTest")) {
        return false;
    }
    this.deleteCookie("cookieTest");
    return true;
};

export default CookieHelper;
