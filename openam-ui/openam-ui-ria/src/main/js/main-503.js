/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import "babel-polyfill";
import "regenerator-runtime/runtime";

require([
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "templates/common/error/503.html",
    "templates/common/LoginHeaderTemplate.html",
    "org/forgerock/commons/ui/common/main/i18n/manager",
    "org/forgerock/openam/ui/common/util/uri/query",
    "ThemeManager"
], ($, Configuration, ServiceUnavailableTemplate, LoginHeaderTemplate, i18n, query, ThemeManager) => {
    const params = query.getCurrentQueryParameters();

    i18n.init().then(() => {
        const loadTemplates = (data) => {
            $("#loginBaseLogo").html(LoginHeaderTemplate(data));
            $("#content").html(ServiceUnavailableTemplate(data));
        };

        // required by ThemeManager
        Configuration.globalData = { realm : params.realm };
        ThemeManager.getTheme().then((theme) => {
            const data = { theme };
            loadTemplates(data);
        }, loadTemplates);
    });
});
