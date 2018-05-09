/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "templates/common/404",
    "templates/common/LoginBaseTemplate"
], (AbstractView, FourZeroFour, LoginBaseTemplate) => {
    const NotFoundView = AbstractView.extend({
        template: FourZeroFour,
        baseTemplate: LoginBaseTemplate
    });

    return new NotFoundView();
});
