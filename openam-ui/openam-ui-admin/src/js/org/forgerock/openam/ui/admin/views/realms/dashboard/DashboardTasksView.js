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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import DashboardTasksTemplate from "templates/admin/views/realms/dashboard/DashboardTasksTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import { map } from "lodash";

const DashboardTasksView = AbstractView.extend({
    template: DashboardTasksTemplate,
    data: {},
    element: "[data-common-tasks-container]",
    events: {
        "click [data-panel-card] a" : "cardClick"
    },
    render (args, callback) {
        this.realmPath = args[0];
        this.parentRender(callback);
    },

    cardClick (e) {
        const dataset = $(e.currentTarget).data();
        e.preventDefault();
        Router.routeTo(Router.configuration.routes[dataset.taskRoute], {
            args: map([this.realmPath, location], encodeURIComponent),
            trigger: true
        });
    }
});

export default DashboardTasksView;
