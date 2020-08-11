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
 * Copyright 2015-2020 ForgeRock AS.
 */

import { t } from "i18next";

import { get as getRealm } from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import DashboardTasksView from "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardTasksView";
import DashboardTemplate from "templates/admin/views/realms/dashboard/DashboardTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import StatusPartial from "partials/util/_Status";

export default AbstractView.extend({
    template: DashboardTemplate,
    partials: {
        "util/_Status": StatusPartial
    },
    render (args, callback) {
        const realmPromise = getRealm(args[0]);

        this.data.realmPath = args[0];

        realmPromise.then((realmData) => {
            const tasksData = [
                {
                    route: "realmsAuthenticationTrees",
                    name: t("console.dashboard.tasks.authenticationTrees"),
                    icon: "fa-tree"
                },
                {
                    route: "realmsServices",
                    name: t("console.dashboard.tasks.services"),
                    icon: "fa-plug"
                },
                {
                    route: "realmsApplicationsOAuth2Clients",
                    name: t("console.dashboard.tasks.oauth2Clients"),
                    icon: "fa-list-alt"
                },
                {
                    route: "realmsApplicationsFederationEntityProviders",
                    name: t("console.dashboard.tasks.saml"),
                    icon: "fa-list-alt"
                },
                {
                    route: "realmsApplicationsAgentsIdentityGateway",
                    name: t("console.dashboard.tasks.identityGateway"),
                    icon: "fa-list-alt"
                },
                {
                    route: "realmsApplicationsAgentsJava",
                    name: t("console.dashboard.tasks.agentsJava"),
                    icon: "fa-list-alt"
                },
                {
                    route: "realmsApplicationsAgentsWeb",
                    name: t("console.dashboard.tasks.agentsWeb"),
                    icon: "fa-list-alt"
                },
                {
                    route: "realmsSts",
                    name: t("console.dashboard.tasks.sts"),
                    icon: "fa-credit-card"
                }
            ];
            this.data.realm = {
                status: realmData.values.active ? t("common.form.active") : t("common.form.inactive"),
                active: realmData.values.active,
                aliases: realmData.values.aliases
            };

            this.parentRender(() => {
                const dashboardTasks = new DashboardTasksView();
                dashboardTasks.data.tasks = tasksData;
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
