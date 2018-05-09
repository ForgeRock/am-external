/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/ServersService"
], (TreeNavigation, createTreeNavigation, Router, ServersService) => {
    const navData = [{
        title: "console.common.navigation.general",
        icon: "fa-cog",
        route: "editServerGeneral"
    }, {
        title: "console.common.navigation.security",
        icon: "fa-lock",
        route: "editServerSecurity"
    }, {
        title: "console.common.navigation.session",
        icon: "fa-ticket",
        route: "editServerSession"
    }, {
        title: "console.common.navigation.sdk",
        icon: "fa-th",
        route: "editServerSdk"
    }, {
        title: "console.common.navigation.cts",
        icon: "fa-database",
        route: "editServerCts"
    }, {
        title: "console.common.navigation.uma",
        icon: "fa-check-circle-o",
        route: "editServerUma"
    }, {
        title: "console.common.navigation.advanced",
        icon: "fa-cogs",
        route: "editServerAdvanced"
    }, {
        title: "console.common.navigation.directoryConfiguration",
        icon: "fa-folder-open",
        route: "editServerDirectoryConfiguration"
    }];

    const EditServerTreeNavigationView = TreeNavigation.extend({
        render (args, callback) {
            const serverName = args[0];
            ServersService.servers.getUrl(serverName).always((url) => {
                this.data.treeNavigation = createTreeNavigation(navData, args);
                this.data.title = url || serverName;
                this.data.home = `#${Router.getLink(Router.configuration.routes.editServerGeneral, [serverName])}`;
                this.data.icon = "fa-server";
                TreeNavigation.prototype.render.call(this, args, callback);
            });
        }
    });

    return new EditServerTreeNavigationView();
});
