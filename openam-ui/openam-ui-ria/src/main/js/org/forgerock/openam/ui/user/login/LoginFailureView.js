/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/user/login/tokens/AuthenticationToken",
    "templates/common/LoginBaseTemplate"
], ($, AbstractView, URIUtils, AuthenticationToken, LoginBaseTemplate) => {
    const LoginFailureView = AbstractView.extend({
        template: "openam/ReturnToLoginTemplate",
        baseTemplate: LoginBaseTemplate,
        data: {},
        render () {
            AuthenticationToken.remove();
            const params = URIUtils.getCurrentFragmentQueryString();
            this.data.params = params ? `&${params}` : "";
            this.data.title = $.t("openam.authentication.unavailable");
            this.parentRender();
        }
    });

    return new LoginFailureView();
});
