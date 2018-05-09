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
 * Copyright 2014-2018 ForgeRock AS.
 */

__webpack_public_path__ = window.pageData.baseUrl; // eslint-disable-line camelcase

import "babel-polyfill";
import "regenerator-runtime/runtime";

require([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/i18n/manager",
    "ThemeManager",
    "templates/user/AuthorizeTemplate",
    "templates/common/LoginBaseTemplate",
    "templates/common/FooterTemplate",
    "templates/common/LoginHeaderTemplate",
    "webpack/prependPublicPath"
], ($, _, Configuration, i18n, ThemeManager, AuthorizeTemplate, LoginBaseTemplate,
    FooterTemplate, LoginHeaderTemplate, prependPublicPath) => {
    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;

    var data = window.pageData || {},
        KEY_CODE_ENTER = 13,
        KEY_CODE_SPACE = 32,
        language = data.locale ? data.locale.split(" ")[0] : undefined;

    i18n.init({
        language,
        loadPath:  prependPublicPath.default("locales/__lng__/__ns__.json"),
        namespace: "authorize"
    }).then(() => {
        if (data.oauth2Data) {
            _.each(data.oauth2Data.displayScopes, (obj) => {
                if (_.isEmpty(obj.values)) {
                    delete obj.values;
                }
                return obj;
            });

            _.each(data.oauth2Data.displayClaims, (obj) => {
                if (_.isEmpty(obj.values)) {
                    delete obj.values;
                }
                return obj;
            });

            if (_.isEmpty(data.oauth2Data.displayScopes) && _.isEmpty(data.oauth2Data.displayClaims)) {
                data.noScopes = true;
            }
        } else {
            data.noScopes = true;
        }

        Configuration.globalData = { realm : data.realm };

        ThemeManager.getTheme().then((theme) => {
            data.theme = theme;

            $("#wrapper").html(LoginBaseTemplate(data));
            $("#footer").html(FooterTemplate(data));
            $("#loginBaseLogo").html(LoginHeaderTemplate(data));
            $("#content").html(AuthorizeTemplate(data)).find(".panel-heading").bind("click keyup", function (e) {
                // keyup is required so that the collapsed panel can be opened with the keyboard alone,
                // and without relying on a mouse click event.
                if (e.type === "keyup" && e.keyCode !== KEY_CODE_ENTER && e.keyCode !== KEY_CODE_SPACE) {
                    return;
                }
                $(this).toggleClass("expanded").next(".panel-collapse").slideToggle();
            });
        });
    });
});
