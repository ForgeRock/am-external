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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/views/realms/RealmBreadcrumb",
    "org/forgerock/openam/ui/admin/services/global/RealmsService",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation"
], ($, _, Constants, EventManager, Router, RealmBreadcrumb, RealmsService, TreeNavigation, humanizeRealmPath,
    createTreeNavigation) => {
    RealmBreadcrumb = RealmBreadcrumb.default;
    humanizeRealmPath = humanizeRealmPath.default;

    const navData = [{
        title: "console.common.navigation.dashboard",
        icon: "fa-dashboard",
        route: "realmsDashboard"
    }, {
        title: "console.common.navigation.applications",
        icon: "fa-list-alt",
        children: [{
            title: "console.common.navigation.federation",
            icon: "fa-angle-right",
            route: "realmsApplicationsFederation"
        }, {
            title: "console.common.navigation.oauth20",
            icon: "fa-angle-right",
            route: "realmsApplicationsOAuth2"
        }, {
            title: "console.common.navigation.agents",
            icon: "fa-angle-right",
            children: [{
                title: "console.common.navigation.agents-web",
                icon: "fa-angle-right",
                route: "realmsApplicationsAgentsWeb"
            }, {
                title: "console.common.navigation.agents-java",
                icon: "fa-angle-right",
                route: "realmsApplicationsAgentsJava"
            },
            {
                title: "console.common.navigation.agents-soap-sts",
                icon: "fa-angle-right",
                route: "realmsApplicationsAgentsSoapSTS"
            }, {
                title: "console.common.navigation.agents-remote-consent",
                icon: "fa-angle-right",
                route: "realmsApplicationsAgentsRemoteConsent"
            }, {
                title: "console.common.navigation.agents-software-publisher",
                icon: "fa-angle-right",
                route: "realmsApplicationsAgentsSoftwarePublisher"
            }]
        }]
    }, {
        title: "console.common.navigation.authentication",
        icon: "fa-user",
        children: [{
            title: "console.common.navigation.settings",
            icon: "fa-angle-right",
            route: "realmsAuthenticationSettings"
        }, {
            title: "console.common.navigation.chains",
            icon: "fa-angle-right",
            route: "realmsAuthenticationChains"
        }, {
            title: "console.common.navigation.modules",
            icon: "fa-angle-right",
            route: "realmsAuthenticationModules"
        }, {
            title: "console.common.navigation.trees",
            icon: "fa-angle-right",
            route: "realmsAuthenticationTrees"
        }, {
            title: "console.common.navigation.webhooks",
            icon: "fa-angle-right",
            route: "realmsAuthenticationWebhooks"
        }]
    }, {
        title: "console.common.navigation.services",
        icon: "fa-plug",
        route: "realmsServices"
    }, {
        title: "console.common.navigation.sessions",
        icon: "fa-ticket",
        route: "realmsSessions"
    }, {
        title: "console.common.navigation.datastores",
        icon: "fa-database",
        route: "realmsDataStores"
    }, {
        title: "console.common.navigation.authorization",
        icon: "fa-key",
        children: [{
            title: "console.common.navigation.policySets",
            icon: "fa-angle-right",
            route: "realmsPolicySets"
        }, {
            title: "console.common.navigation.resourceTypes",
            icon: "fa-angle-right",
            route: "realmsResourceTypes"
        }]
    }, {
        title: "console.common.navigation.identities",
        icon: "fa-address-card",
        route: "realmsIdentities"
    }, {
        title: "console.common.navigation.sts",
        icon: "fa-credit-card",
        route: "realmsSts"
    }, {
        title: "console.common.navigation.scripts",
        icon: "fa-code",
        route: "realmsScripts"
    }];

    const RealmTreeNavigationView = TreeNavigation.extend({
        sendEvent (e) {
            e.preventDefault();
            EventManager.sendEvent($(e.currentTarget).data().event, this.data.realmPath);
        },

        realmExists (path) {
            return RealmsService.realms.get(path);
        },
        render (args) {
            this.events = {
                ...this.events,
                "click a[data-event]": "sendEvent"
            };
            this.data.realmPath = args[0];
            this.data.realmName = humanizeRealmPath(this.data.realmPath);
            this.data.treeNavigation = createTreeNavigation(navData, [encodeURIComponent(this.data.realmPath)]);
            this.data.title = this.data.realmName;
            this.data.home = `#${Router.getLink(
                Router.configuration.routes.realmDefault, [encodeURIComponent(this.data.realmPath)])}`;
            this.data.icon = "fa-cloud";
            this.realmExists(this.data.realmPath).then(() => {
                TreeNavigation.prototype.render.call(this, args, () => {
                    RealmBreadcrumb.render(this.data.title, this.route.pattern);
                });
            }, (xhr) => {
                /**
                 * If a non-existant realm was specified, return to realms list
                 */
                if (xhr.status === 404) {
                    Router.routeTo(Router.configuration.routes.realms, {
                        args: [],
                        trigger: true
                    });
                }
            });
        }
    });

    return new RealmTreeNavigationView();
});
