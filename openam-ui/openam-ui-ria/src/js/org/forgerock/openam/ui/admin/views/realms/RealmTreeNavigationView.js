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
 * Copyright 2015-2019 ForgeRock AS.
 */

import $ from "jquery";

import createTreeNavigation from "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import humanizeRealmPath from "org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath";
import RealmBreadcrumb from "org/forgerock/openam/ui/admin/views/realms/RealmBreadcrumb";
import RealmsService from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import Router from "org/forgerock/commons/ui/common/main/Router";
import TreeNavigation from "org/forgerock/openam/ui/common/components/TreeNavigation";

const navData = [{
    title: "console.navigation.dashboard.title",
    icon: "fa-dashboard",
    route: "realmsDashboard"
}, {
    title: "console.navigation.applications.title",
    icon: "fa-list-alt",
    children: [{
        title: "console.navigation.applications-agents.title",
        icon: "fa-angle-right",
        children: [{
            title: "console.navigation.applications-agents-identityGateway.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsIdentityGateway"
        }, {
            title: "console.navigation.applications-agents-java.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsJava"
        }, {
            title: "console.navigation.applications-agents-remoteConsent.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsRemoteConsent"
        }, {
            title: "console.navigation.applications-agents-soapSts.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsSoapSTS"
        }, {
            title: "console.navigation.applications-agents-softwarePublisher.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsSoftwarePublisher"
        }, {
            title: "console.navigation.applications-agents-trustedJwtIssuer.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsOAuth2TrustedJwtIssuer"
        }, {
            title: "console.navigation.applications-agents-web.title",
            icon: "fa-angle-right",
            route: "realmsApplicationsAgentsWeb"
        }]
    }, {
        title: "console.navigation.applications-federation.title",
        icon: "fa-angle-right",
        route: "realmsApplicationsFederation"
    }, {
        title: "console.navigation.applications-oauth2.title",
        icon: "fa-angle-right",
        route: "realmsApplicationsOAuth2"
    }]
}, {
    title: "console.navigation.authentication.title",
    icon: "fa-user",
    children: [{
        title: "console.navigation.authentication-chains.title",
        icon: "fa-angle-right",
        route: "realmsAuthenticationChains"
    }, {
        title: "console.navigation.authentication-modules.title",
        icon: "fa-angle-right",
        route: "realmsAuthenticationModules"
    }, {
        title: "console.navigation.authentication-settings.title",
        icon: "fa-angle-right",
        route: "realmsAuthenticationSettings"
    }, {
        title: "console.navigation.authentication-trees.title",
        icon: "fa-angle-right",
        route: "realmsAuthenticationTrees"
    }, {
        title: "console.navigation.authentication-webhooks.title",
        icon: "fa-angle-right",
        route: "realmsAuthenticationWebhooks"
    }]
}, {
    title: "console.navigation.authorization.title",
    icon: "fa-key",
    children: [{
        title: "console.navigation.authorization-policySets.title",
        icon: "fa-angle-right",
        route: "realmsPolicySets"
    }, {
        title: "console.navigation.authorization-resourceTypes.title",
        icon: "fa-angle-right",
        route: "realmsResourceTypes"
    }]
}, {
    title: "console.navigation.identities.title",
    icon: "fa-address-card",
    route: "realmsIdentities"
}, {
    title: "console.navigation.identityStores.title",
    icon: "fa-database",
    route: "realmsDataStores"
}, {
    title: "console.navigation.scripts.title",
    icon: "fa-code",
    route: "realmsScripts"
}, {
    title: "console.navigation.secretStores.title",
    icon: "fa-eye-slash",
    route: "realmsSecretStores"
}, {
    title: "console.navigation.services.title",
    icon: "fa-plug",
    route: "realmsServices"
}, {
    title: "console.navigation.sessions.title",
    icon: "fa-ticket",
    route: "realmsSessions"
}, {
    title: "console.navigation.sts.title",
    icon: "fa-credit-card",
    route: "realmsSts"
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
                RealmBreadcrumb.render(this.data, this.route.pattern);
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

export default new RealmTreeNavigationView();
