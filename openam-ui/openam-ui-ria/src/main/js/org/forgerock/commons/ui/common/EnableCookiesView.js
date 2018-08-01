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
 * Copyright 2011-2018 ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "templates/common/EnableCookiesTemplate",
    "templates/common/LoginBaseTemplate"
], (AbstractView, cookieHelper, EnableCookiesTemplate, LoginBaseTemplate) => {
    const EnableCookiesView = AbstractView.extend({
        template: EnableCookiesTemplate,
        baseTemplate: LoginBaseTemplate,
        render () {
            if (cookieHelper.cookiesEnabled()) {
                location.href = "#login/";
            } else {
                this.parentRender();
            }
        }
    });

    return new EnableCookiesView();
});