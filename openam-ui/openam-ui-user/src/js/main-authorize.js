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
 * Copyright 2014-2020 ForgeRock AS.
 */

import "./webpack/setWebpackPublicPath";
import "whatwg-fetch";

import { each, isEmpty } from "lodash";
import $ from "jquery";

import { getTheme } from "org/forgerock/openam/ui/common/util/ThemeManager";
import { init } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import loadTemplate from "org/forgerock/openam/ui/common/util/theme/loadTemplate";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";

const data = window.pageData || {};
const KEY_CODE_ENTER = 13;
const KEY_CODE_SPACE = 32;
const language = data.locale ? data.locale.split(" ")[0] : undefined;

const loadThemedTemplates = async (templates, theme) => {
    return await Promise.all(templates.map((template) => loadTemplate(template, theme)));
};

init({ language, namespace: "authorize" }).then(() => {
    if (data.oauth2Data) {
        each(data.oauth2Data.displayScopes, (obj) => {
            if (isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        each(data.oauth2Data.displayClaims, (obj) => {
            if (isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        if (isEmpty(data.oauth2Data.displayScopes) && isEmpty(data.oauth2Data.displayClaims)) {
            data.noScopes = true;
        }
    } else {
        data.noScopes = true;
    }

    Configuration.globalData = { realm : data.realm || "/" };

    const render = async () => {
        const theme = await getTheme();
        data.theme = theme;

        const templates = ["common/LoginBaseTemplate", "common/FooterTemplate",
            "common/LoginHeaderTemplate", "user/AuthorizeTemplate"];
        const [LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate,
            AuthorizeTemplate] = await loadThemedTemplates(templates, theme.path);
        UIUtils.initHelpers();
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
    };
    render();
});
