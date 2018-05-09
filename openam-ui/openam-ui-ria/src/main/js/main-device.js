/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

__webpack_public_path__ = window.pageData.baseUrl; // eslint-disable-line camelcase

import "babel-polyfill";
import "regenerator-runtime/runtime";

require([
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "templates/user/DeviceTemplate",
    "templates/user/DeviceDoneTemplate",
    "templates/common/LoginBaseTemplate",
    "templates/common/FooterTemplate",
    "templates/common/LoginHeaderTemplate",
    "org/forgerock/commons/ui/common/main/i18n/manager",
    "ThemeManager",
    "webpack/prependPublicPath"
], ($, Configuration, DeviceTemplate, DeviceDoneTemplate, LoginBaseTemplate, FooterTemplate,
    LoginHeaderTemplate, i18n, ThemeManager, prependPublicPath) => {
    const data = window.pageData;
    const template = data.done ? DeviceDoneTemplate : DeviceTemplate;
    const language = data.locale ? data.locale.split(" ")[0] : undefined;

    i18n.init({
        language,
        loadPath:  prependPublicPath.default("locales/__lng__/__ns__.json"),
        namespace: "device"
    }).then(() => {
        Configuration.globalData = { realm : data.realm };

        ThemeManager.getTheme().then((theme) => {
            data.theme = theme;
            $("#wrapper").html(LoginBaseTemplate(data));
            $("#footer").html(FooterTemplate(data));
            $("#loginBaseLogo").html(LoginHeaderTemplate(data));
            $("#content").html(template(data));
        });
    });
});
