/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "templates/common/error/403",
    "templates/common/LoginBaseTemplate"
], (AbstractView, Configuration, ForbiddenErrorTemplate, LoginBaseTemplate) => {
    var ForbiddenView = AbstractView.extend({
        template: ForbiddenErrorTemplate,
        data: {},
        render () {
            if (!Configuration.loggedUser) {
                this.baseTemplate = LoginBaseTemplate;
            }
            this.parentRender();
        }
    });

    return new ForbiddenView();
});
