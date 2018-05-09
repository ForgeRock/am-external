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
    "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardTasksView",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/global/RealmsService",
    "org/forgerock/openam/ui/admin/services/realm/DashboardService",
    "templates/admin/views/realms/dashboard/DashboardTemplate",
    "partials/util/_Status"
], ($, _, AbstractView, DashboardTasksView, Messages, RealmsService, DashboardService, DashboardTemplate,
    StatusPartial) => {
    return AbstractView.extend({
        template: DashboardTemplate,
        partials: {
            "util/_Status": StatusPartial
        },
        render (args, callback) {
            var self = this,
                realmPromise = RealmsService.realms.get(args[0]),
                tasksPromise = DashboardService.dashboard.commonTasks.all(args[0]);

            this.data.realmPath = args[0];

            $.when(realmPromise, tasksPromise).then((realmData, tasksData) => {
                self.data.realm = {
                    status: realmData.values.active ? $.t("common.form.active") : $.t("common.form.inactive"),
                    active: realmData.values.active,
                    aliases: realmData.values.aliases
                };

                self.parentRender(() => {
                    var dashboardTasks = new DashboardTasksView();
                    dashboardTasks.data.allTasks = tasksData[0].result;
                    dashboardTasks.data.taskGroup = { tasks: tasksData[0].result };
                    dashboardTasks.render(args, callback);
                }, callback);
            }, (errorRealm, errorTasks) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: errorRealm ? errorRealm : errorTasks
                });
            });
        }
    });
});
