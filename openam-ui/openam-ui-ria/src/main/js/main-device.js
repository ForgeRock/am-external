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
 * Copyright 2015-2018 ForgeRock AS.
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
