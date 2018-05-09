/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/dashboard/services/MyApplicationsService"
], ($, _, AbstractView, MyApplicationsService) => {
    var Applications = AbstractView.extend({
        template: "user/dashboard/MyApplicationsTemplate",
        noBaseTemplate: true,
        element: "#myApplicationsSection",
        render () {
            var self = this;

            MyApplicationsService.getMyApplications().then((apps) => {
                if (apps.length > 0) {
                    self.data.apps = apps;
                }
                self.parentRender();
            });
        }
    });

    return new Applications();
});
