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
 * Copyright 2017 ForgeRock AS.
 */

require.config({
    map: {
        "*": {
            "ThemeManager": "org/forgerock/openam/ui/common/util/ThemeManager",
            "Router": "org/forgerock/openam/ui/common/SingleRouteRouter",
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore": "lodash"
        }
    },
    paths: {
        "handlebars": "libs/handlebars-4.0.5",
        "i18next": "libs/i18next-1.7.3-min",
        "jquery": "libs/jquery-2.1.1-min",
        "lodash": "libs/lodash-3.10.1-min",
        "redux": "libs/redux-3.5.2-min",
        "redux-actions": "libs/redux-actions-2.0.1-min",
        "text": "libs/text-2.0.15"
    },
    shim: {
        "handlebars": {
            exports: "handlebars"
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18n"
        },
        "lodash": {
            exports: "_"
        }
    }
});

require([
    "jquery",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "text!templates/common/error/503.html",
    "text!templates/common/LoginHeaderTemplate.html",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/openam/ui/common/util/uri/query",
    "ThemeManager"
], ($, HandleBars, Configuration, Constants, ServiceUnavailableTemplate, LoginHeaderTemplate,
    i18nManager, query, ThemeManager) => {
    const params = query.getCurrentQueryParameters();

    i18nManager.init({
        paramLang: { locale : params.locale },
        defaultLang: Constants.DEFAULT_LANGUAGE
    });

    const loadTemplates = (data) => {
        $("#loginBaseLogo").html(HandleBars.compile(LoginHeaderTemplate)(data));
        $("#content").html(HandleBars.compile(ServiceUnavailableTemplate)(data));
    };

    // required by ThemeManager
    Configuration.globalData = { realm : params.realm };
    ThemeManager.getTheme().then((theme) => {
        const data = { theme };
        loadTemplates(data);
    }, loadTemplates);
});
