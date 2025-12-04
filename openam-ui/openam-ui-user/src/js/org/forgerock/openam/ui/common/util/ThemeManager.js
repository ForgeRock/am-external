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
 * Copyright 2011-2025 Ping Identity Corporation.
 */

import { each, every, find, get, has, isArray, isRegExp, keys, mergeWith, partial, pick, some } from "lodash";
import $ from "jquery";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import getThemeConfiguration from "./getThemeConfiguration";
import prependPublicPath from "@/webpack/prependPublicPath";
import Router from "org/forgerock/commons/ui/common/main/Router";
import store from "store";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const defaultThemeName = "default";
const applyThemeToPage = function (path, icon, stylesheets) {
    // If path is present, ensure path is correctly scoped within themes
    path = path ? `themes/${path}` : "";

    // We might be switching themes (due to a realm change) and so we need to clean up the previous theme.
    $("link").remove();

    $("<link/>", {
        rel: "icon",
        type: "image/x-icon",
        href: prependPublicPath(`${path}${icon}`)
    }).appendTo("head");

    $("<link/>", {
        rel: "shortcut icon",
        type: "image/x-icon",
        href: prependPublicPath(`${path}${icon}`)
    }).appendTo("head");

    each(stylesheets, (stylesheet) => {
        $("<link/>", {
            rel: "stylesheet",
            type: "text/css",
            href: stylesheet
        }).appendTo("head");
    });
};

/**
 * Determine if a mapping specification matches the current environment. Mappings are of the form:
 * { theme: "theme-name", realms: ["/a", "/b"], authenticationChains: ["test", "cats"] }.
 * @param {string} realm The full realm path to match the themes against.
 * @param {string} authenticationChain The name of the authentication chain to match themes against.
 * @param {object} mapping the mapping specification provided by the theme configuration.
 * @returns {boolean} true if mapping matches the current environment.
 */
const isMatchingThemeMapping = function (realm, authenticationChain, mapping) {
    const matchers = {
        realms: realm,
        authenticationChains: authenticationChain
    };
    const matcherMappings = pick(mapping, keys(matchers));

    return every(matcherMappings, (mappings, matcher) => {
        const value = matchers[matcher];
        return some(mappings, (mapping) => {
            if (isRegExp(mapping)) {
                return new RegExp(mapping, "i").test(value);
            } else {
                return mapping.toUpperCase() === value.toUpperCase();
            }
        });
    });
};

/**
 * Find the appropriate theme for the current environment by using the theme configuration mappings.
 * <p>
 * If a theme is found that matches the current environment then its name will be
 * returned, otherwise the default theme name will be returned.
 * @param {string} realm The full realm path to match the themes against.
 * @param {string} authenticationChain The name of the authentication chain to match themes against.
 * @returns {string} theme The selected theme configuration name.
 */
const findMatchingTheme = async function (realm, authenticationChain) {
    const themeConfiguration = (await getThemeConfiguration()).default;
    if (!isArray(themeConfiguration.mappings)) {
        return defaultThemeName;
    }
    const matchedThemeMapping = find(themeConfiguration.mappings,
        partial(isMatchingThemeMapping, realm, authenticationChain));
    if (matchedThemeMapping) {
        return matchedThemeMapping.theme;
    }
    return defaultThemeName;
};

const extendTheme = function (theme, parentTheme) {
    return mergeWith({}, parentTheme, theme, (objectValue, sourceValue) => {
        // We don't want to merge arrays. If a theme has specified an array, it should be used verbatim.
        if (isArray(sourceValue)) {
            return sourceValue;
        }
        return undefined;
    });
};

// TODO: This code should be shared with the RESTLoginView and friends.
const getAuthenticationChainName = function () {
    const urlParams = URIUtils.parseQueryString(URIUtils.getCurrentCompositeQueryString());

    if (urlParams.service) {
        return urlParams.service;
    }
    if (urlParams.authIndexType && urlParams.authIndexType === "service") {
        return urlParams.authIndexValue || "";
    }
    return "";
};

const applyBaseUrl = (path) => {
    return path.startsWith(window.pageData.baseUrl) ? path : window.pageData.baseUrl + path;
};

const applyBaseUrlToTheme = (theme) => {
    theme.settings.loginLogo.src = applyBaseUrl(theme.settings.loginLogo.src);
    theme.settings.logo.src = applyBaseUrl(theme.settings.logo.src);

    return theme;
};

/**
 * Determine the theme from the current realm and setup the theme on the page. This will
 * clear out any previous theme.
 * @returns {Promise} a promise that is resolved when the theme has been applied.
 */
export async function getTheme () {
    const themeConfiguration = (await getThemeConfiguration()).default;

    if (__DEV__) {
        const validate = await import("./theme/validate");
        validate.default(themeConfiguration.themes);
    }

    const realm = store.getState().remote.info.realm || Configuration.globalData.realm;
    const themeName = await findMatchingTheme(realm, getAuthenticationChainName());
    const isAdminTheme = get(Router, "currentRoute.navGroup") === "admin";
    const hasThemeNameChanged = themeName !== Configuration.globalData.themeName;
    const hasAdminThemeFlagChanged = isAdminTheme !== Configuration.globalData.isAdminTheme;
    const hasThemeChanged = hasThemeNameChanged || hasAdminThemeFlagChanged;
    let theme;

    if (!hasThemeChanged) {
        return Configuration.globalData.theme;
    }

    const defaultTheme = themeConfiguration.themes[defaultThemeName];

    theme = themeConfiguration.themes[themeName];
    theme = extendTheme(theme, defaultTheme);

    // We don't apply themes to the admin interface because it would take significant effort to make the UI
    // themeable.
    let stylesheets = isAdminTheme ? Constants.DEFAULT_STYLESHEETS : theme.stylesheets;

    /**
     * When the XUI is booted outside of the XUI directory (e.g. the OAuth 2 Consent page "/oauth2/authorize"),
     * paths to assets must be fully qualified (e.g. "https://www.../images/logo.png" instead of "images/logo.png")
     * to successfully address those assets. When assets are loaded via an import statement in ThemeConfiguration,
     * this is done automatically, however, when plain strings are provided, we must apply this manually.
     */
    const hasBaseUrl = has(window, "pageData.baseUrl");
    if (hasBaseUrl) {
        theme = applyBaseUrlToTheme(theme);
        stylesheets = stylesheets.map(applyBaseUrl);
    }

    applyThemeToPage(theme.path, theme.icon, stylesheets);
    /* eslint-disable require-atomic-updates */
    Configuration.globalData.theme = theme;
    Configuration.globalData.themeName = themeName;
    Configuration.globalData.isAdminTheme = isAdminTheme;
    /* eslint-enable require-atomic-updates */
    EventManager.sendEvent(Constants.EVENT_THEME_CHANGED);
    return theme;
}
