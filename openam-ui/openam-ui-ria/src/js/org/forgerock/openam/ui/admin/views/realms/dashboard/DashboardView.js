/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2018 ForgeRock AS.
 */

import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import DashboardService from "org/forgerock/openam/ui/admin/services/realm/DashboardService";
import DashboardTasksView from "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardTasksView";
import DashboardTemplate from "templates/admin/views/realms/dashboard/DashboardTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import RealmsService from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import StatusPartial from "partials/util/_Status";

export default AbstractView.extend({
    template: DashboardTemplate,
    partials: {
        "util/_Status": StatusPartial
    },
    render (args, callback) {
        var self = this,
            realmPromise = RealmsService.realms.get(args[0]),
            tasksPromise = DashboardService.dashboard.commonTasks.all(args[0]);

        this.data.realmPath = args[0];

        Promise.all([realmPromise, tasksPromise]).then(([realmData, tasksData]) => {
            self.data.realm = {
                status: realmData.values.active ? $.t("common.form.active") : $.t("common.form.inactive"),
                active: realmData.values.active,
                aliases: realmData.values.aliases
            };

            self.parentRender(() => {
                var dashboardTasks = new DashboardTasksView();
                dashboardTasks.data.allTasks = tasksData.result;
                dashboardTasks.data.taskGroup = { tasks: tasksData.result };
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
