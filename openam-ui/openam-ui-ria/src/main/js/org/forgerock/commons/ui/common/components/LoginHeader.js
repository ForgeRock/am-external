/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "templates/common/LoginHeaderTemplate"
], function(AbstractView, LoginHeaderTemplate) {
    var LoginHeader = AbstractView.extend({
        element: "#loginBaseLogo",
        template: LoginHeaderTemplate,
        noBaseTemplate: true
    });

    return new LoginHeader();
});
