/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
