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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import "whatwg-fetch";
import $ from "jquery";

import { getCurrentQueryParameters } from "org/forgerock/openam/ui/common/util/uri/query";
import { getTheme } from "org/forgerock/openam/ui/common/util/ThemeManager";
import { init as i18nInit } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import ServiceUnavailableTemplate from "themes/default/templates/common/error/503.html";

const params = getCurrentQueryParameters();

i18nInit().then(() => {
    const loadTemplates = (data) => {
        $("#content").html(ServiceUnavailableTemplate(data));
    };

    // required by ThemeManager
    Configuration.globalData = { realm : params.realm };
    getTheme().then((theme) => {
        const data = { theme };
        loadTemplates(data);
    }, loadTemplates);
    const lang = navigator.language || "en";
    $("html").attr("lang", lang);
});
