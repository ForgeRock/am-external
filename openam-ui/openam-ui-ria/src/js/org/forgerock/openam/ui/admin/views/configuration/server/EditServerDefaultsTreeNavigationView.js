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
 * Copyright 2016-2018 ForgeRock AS.
 */

import $ from "jquery";

import TreeNavigation from "org/forgerock/openam/ui/common/components/TreeNavigation";
import createTreeNavigation from "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation";
import Router from "org/forgerock/commons/ui/common/main/Router";

const navData = [{
    title: "console.navigation.general.title",
    icon: "fa-cog",
    route: "editServerDefaultsGeneral"
}, {
    title: "console.navigation.security.title",
    icon: "fa-lock",
    route: "editServerDefaultsSecurity"
}, {
    title: "console.navigation.session.title",
    icon: "fa-ticket",
    route: "editServerDefaultsSession"
}, {
    title: "console.navigation.sdk.title",
    icon: "fa-th",
    route: "editServerDefaultsSdk"
}, {
    title: "console.navigation.cts.title",
    icon: "fa-database",
    route: "editServerDefaultsCts"
}, {
    title: "console.navigation.uma.title",
    icon: "fa-check-circle-o",
    route: "editServerDefaultsUma"
}, {
    title: "console.navigation.advanced.title",
    icon: "fa-cogs",
    route: "editServerDefaultsAdvanced"
}];

const EditServerDefaultsTreeNavigationView = TreeNavigation.extend({
    render (args, callback) {
        this.data.treeNavigation = createTreeNavigation(navData);
        this.data.title = $.t("config.AppConfiguration.Navigation.links.configure.server-defaults");
        this.data.home = `#${Router.getLink(Router.configuration.routes.editServerDefaultsGeneral, args)}`;
        TreeNavigation.prototype.render.call(this, args, callback);
    }
});

export default new EditServerDefaultsTreeNavigationView();
