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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import createTreeNavigation from "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServersService from "org/forgerock/openam/ui/admin/services/global/ServersService";
import TreeNavigation from "org/forgerock/openam/ui/common/components/TreeNavigation";

const navData = [{
    title: "console.navigation.general.title",
    icon: "fa-cog",
    route: "editServerGeneral"
}, {
    title: "console.navigation.security.title",
    icon: "fa-lock",
    route: "editServerSecurity"
}, {
    title: "console.navigation.session.title",
    icon: "fa-ticket",
    route: "editServerSession"
}, {
    title: "console.navigation.sdk.title",
    icon: "fa-th",
    route: "editServerSdk"
}, {
    title: "console.navigation.cts.title",
    icon: "fa-database",
    route: "editServerCts"
}, {
    title: "console.navigation.uma.title",
    icon: "fa-check-circle-o",
    route: "editServerUma"
}, {
    title: "console.navigation.advanced.title",
    icon: "fa-cogs",
    route: "editServerAdvanced"
}, {
    title: "console.navigation.directoryConfiguration.title",
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

export default new EditServerTreeNavigationView();
