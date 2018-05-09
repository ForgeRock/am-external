/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "templates/common/UnauthorizedTemplate",
    "templates/common/LoginBaseTemplate"
], function(AbstractView, Configuration, URIUtils, UnauthorizedTemplate, LoginBaseTemplate) {
    var UnauthorizedView = AbstractView.extend({
        template: UnauthorizedTemplate,
        baseTemplate: LoginBaseTemplate,
        events: {
            "click #goBack": function() {
                window.history.go(-1);
            },
            "click #logout": function() {
                Configuration.gotoFragment = "#" + URIUtils.getCurrentFragment();
            }
        }
    });

    return new UnauthorizedView();
});
