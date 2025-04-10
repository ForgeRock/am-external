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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import "./webpack/setWebpackPublicPath";
import "whatwg-fetch";

import $ from "jquery";

import { getTheme } from "org/forgerock/openam/ui/common/util/ThemeManager";
import { init } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import loadTemplate from "org/forgerock/openam/ui/common/util/theme/loadTemplate";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";

const data = window.pageData;
const language = data.locale ? data.locale.split(" ")[0] : undefined;

const loadThemedTemplates = async (templates, theme) => {
    return await Promise.all(templates.map((template) => loadTemplate(template, theme)));
};

init({ language, namespace: "uma" }).then(() => {
    Configuration.globalData = { realm : data.realm };

    const render = async () => {
        const theme = await getTheme();
        data.theme = theme;
        const templates = ["common/LoginBaseTemplate", "common/FooterTemplate",
            "common/LoginHeaderTemplate", "user/PctConsentTemplate"];
        const [LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate,
            PctConsentTemplate] = await loadThemedTemplates(templates, theme.path);
        UIUtils.initHelpers();
        $("html").attr("lang", language);
        $("#wrapper").html(LoginBaseTemplate(data));
        $("#footer").html(FooterTemplate(data));
        $("#loginBaseLogo").html(LoginHeaderTemplate(data));
        $("#content").html(PctConsentTemplate(data));
    };
    render();
});
