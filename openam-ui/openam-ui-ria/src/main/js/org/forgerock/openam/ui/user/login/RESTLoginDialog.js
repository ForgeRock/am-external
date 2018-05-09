/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginView",
    "templates/common/DefaultBaseTemplate"
], (Backbone, AbstractView, Configuration, RESTLoginView, DefaultBaseTemplate) => {
    const LoginDialog = AbstractView.extend({
        template: DefaultBaseTemplate,
        data : {},
        actions: [],
        render () {
            Configuration.backgroundLogin = true;
            RESTLoginView.render();
        }
    });
    return new LoginDialog();
});
