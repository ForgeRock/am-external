/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import getThemeConfiguration from "./getThemeConfiguration";
import prependPublicPath from "webpack/prependPublicPath";
import Router from "Router";
import store from "store/index";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const defaultThemeName = "default";
const applyThemeToPage = function (path, icon, stylesheets) {
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

    _.each(stylesheets, (stylesheet) => {
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
 *
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
    const matcherMappings = _.pick(mapping, _.keys(matchers));

    return _.every(matcherMappings, (mappings, matcher) => {
        const value = matchers[matcher];
        return _.some(mappings, (mapping) => {
            if (_.isRegExp(mapping)) {
                return mapping.test(value);
            } else {
                return mapping === value;
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
    const themeConfiguration = await getThemeConfiguration();
    if (!_.isArray(themeConfiguration.mappings)) {
        return defaultThemeName;
    }
    const matchedThemeMapping = _.find(themeConfiguration.mappings,
        _.partial(isMatchingThemeMapping, realm, authenticationChain));
    if (matchedThemeMapping) {
        return matchedThemeMapping.theme;
    }
    return defaultThemeName;
};

const extendTheme = function (theme, parentTheme) {
    return _.merge({}, parentTheme, theme, (objectValue, sourceValue) => {
        // We don't want to merge arrays. If a theme has specified an array, it should be used verbatim.
        if (_.isArray(sourceValue)) {
            return sourceValue;
        }
        return undefined;
    });
};

const validateConfig = function (configuration) {
    if (!_.isObject(configuration)) {
        throw "Theme configuration must return an object";
    }

    if (!_.isObject(configuration.themes)) {
        throw "Theme configuration must specify a themes object";
    }

    if (!_.isObject(configuration.themes[defaultThemeName])) {
        throw "Theme configuration must specify a default theme";
    }
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

/**
 * Determine the theme from the current realm and setup the theme on the page. This will
 * clear out any previous theme.
 * @returns {Promise} a promise that is resolved when the theme has been applied.
 */
export async function getTheme () {
    const themeConfiguration = await getThemeConfiguration();
    validateConfig(themeConfiguration);

    const realm = store.getState().remote.info.realm || Configuration.globalData.realm;
    const themeName = await findMatchingTheme(realm, getAuthenticationChainName());
    const isAdminTheme = _.get(Router, "currentRoute.navGroup") === "admin";
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
    const stylesheets = isAdminTheme ? Constants.DEFAULT_STYLESHEETS : theme.stylesheets;

    applyThemeToPage(theme.path, theme.icon, stylesheets);
    Configuration.globalData.theme = theme;
    Configuration.globalData.themeName = themeName;
    Configuration.globalData.isAdminTheme = isAdminTheme;
    EventManager.sendEvent(Constants.EVENT_THEME_CHANGED);
    return theme;
}
