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
    "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole",
    "templates/admin/views/realms/dashboard/DashboardTasksTemplate"
], ($, _, AbstractView, RedirectToLegacyConsole, DashboardTasksTemplate) => {
    var DashboardTasksView = AbstractView.extend({
        template: DashboardTasksTemplate,
        data: {},
        element: "[data-common-tasks-container]",
        events: {
            "click [data-panel-card] a" : "cardClick",
            "click [data-common-tasks]" : "commonTasksBtnClick"
        },
        render (args, callback) {
            this.realmPath = args[0];
            this.parentRender(callback);
        },

        cardClick (e) {
            var dataset = $(e.currentTarget).data();
            if (!dataset.taskLink || dataset.taskLink.indexOf("http") !== 0) {
                e.preventDefault();
                if (dataset.taskGroup) {
                    this.data.taskGroup = _.find(this.data.allTasks, { _id: dataset.taskGroup });
                    this.parentRender();
                } else {
                    RedirectToLegacyConsole.commonTasks(this.realmPath, dataset.taskLink);
                }
            }
        },

        commonTasksBtnClick (e) {
            e.preventDefault();
            this.data.taskGroup = {};
            this.data.taskGroup.tasks = this.data.allTasks;
            this.parentRender();
        }
    });

    return DashboardTasksView;
});
