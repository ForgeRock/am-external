/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation",
    "org/forgerock/commons/ui/common/main/Router"
], ($, TreeNavigation, createTreeNavigation, Router) => {
    const navData = [{
        title: "console.common.navigation.general",
        icon: "fa-cog",
        route: "editServerDefaultsGeneral"
    }, {
        title: "console.common.navigation.security",
        icon: "fa-lock",
        route: "editServerDefaultsSecurity"
    }, {
        title: "console.common.navigation.session",
        icon: "fa-ticket",
        route: "editServerDefaultsSession"
    }, {
        title: "console.common.navigation.sdk",
        icon: "fa-th",
        route: "editServerDefaultsSdk"
    }, {
        title: "console.common.navigation.cts",
        icon: "fa-database",
        route: "editServerDefaultsCts"
    }, {
        title: "console.common.navigation.uma",
        icon: "fa-check-circle-o",
        route: "editServerDefaultsUma"
    }, {
        title: "console.common.navigation.advanced",
        icon: "fa-cogs",
        route: "editServerDefaultsAdvanced"
    }];

    const EditServerDefaultsTreeNavigationView = TreeNavigation.extend({
        render (args, callback) {
            this.data.treeNavigation = createTreeNavigation(navData);
            this.data.title = $.t("console.common.navigation.serverDefaults");
            this.data.home = `#${Router.getLink(Router.configuration.routes.editServerDefaultsGeneral, args)}`;
            TreeNavigation.prototype.render.call(this, args, callback);
        }
    });

    return new EditServerDefaultsTreeNavigationView();
});
